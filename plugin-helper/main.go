package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"

	"github.com/lucky-aeon/agentx/plugin-helper/config"
	"github.com/lucky-aeon/agentx/plugin-helper/service"
	"github.com/lucky-aeon/agentx/plugin-helper/xlog"
)

// DeployRequest 部署请求结构
type DeployRequest struct {
	MCPServers map[string]config.MCPServerConfig `json:"mcpServers"`
}

// ServerManager 管理所有运行的服务
type ServerManager struct {
	sync.RWMutex
	mcpServiceMgr service.ServiceManagerI
	cfg           config.Config
}

var manager *ServerManager

// initServerManager 初始化服务管理器
func initServerManager(cfg config.Config) *ServerManager {
	return &ServerManager{
		mcpServiceMgr: service.NewServiceManager(cfg),
		cfg:           cfg,
	}
}

// loadConfig 加载已保存的配置
func (m *ServerManager) loadConfig() error {
	data, err := os.ReadFile(m.cfg.GetMcpConfigPath())
	if os.IsNotExist(err) {
		return nil
	}
	if err != nil {
		return err
	}

	var config map[string]config.MCPServerConfig
	if err := json.Unmarshal(data, &config); err != nil {
		return err
	}

	for name, srv := range config {
		fmt.Printf("Loading server %s: %v\n", name, srv)
		if err := m.DeployServer(echo.New().Logger, name, srv); err != nil {
			fmt.Printf("Error deploying server %s: %v\n", name, err)
		}
	}
	fmt.Printf("Loaded %d servers\n", len(config))
	return nil
}

// DeployServer 部署单个服务
func (m *ServerManager) DeployServer(logger echo.Logger, name string, config config.MCPServerConfig) error {
	m.Lock()
	defer m.Unlock()

	if config.Command == "" && config.URL == "" {
		return fmt.Errorf("服务配置必须包含 URL 或 Command")
	}

	if config.Command != "" && config.URL != "" {
		return fmt.Errorf("服务配置不能同时包含 URL 和 Command")
	}

	return m.mcpServiceMgr.DeployServer(logger, name, config)
}

// handleDeploy 处理部署请求
func (m *ServerManager) handleDeploy(c echo.Context) error {
	c.Logger().Infof("Deploy request: %v", c.Request().Body)
	var req DeployRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, map[string]string{"error": err.Error()})
	}
	c.Logger().Infof("Deploy request: %v", req)

	for name, config := range req.MCPServers {
		c.Logger().Infof("Deploying %s: %v", name, config)
		if err := m.DeployServer(c.Logger(), name, config); err != nil {
			return c.JSON(http.StatusInternalServerError, map[string]string{
				"error": fmt.Sprintf("Failed to deploy %s: %v", name, err),
			})
		}
	}

	c.Logger().Infof("Deployed all servers")

	return c.JSON(http.StatusOK, map[string]string{"status": "success"})
}

// proxyHandler 返回代理处理函数
func proxyHandler() echo.HandlerFunc {
	return func(c echo.Context) error {
		path := c.Request().URL.Path

		// 从路径中提取服务名和路由
		parts := strings.Split(strings.TrimPrefix(path, "/"), "/")
		if len(parts) < 2 {
			return c.String(http.StatusNotFound, "Invalid path")
		}

		serviceName := parts[0]
		lastRoute := parts[len(parts)-1] // 获取最后一个路由部分
		remainingPath := "/" + strings.Join(parts[1:], "/")

		// 获取服务配置
		manager.RLock()
		instance, err := manager.mcpServiceMgr.GetMcpService(c.Logger(), serviceName)
		manager.RUnlock()

		if err != nil {
			return c.String(http.StatusNotFound, "Service not found")
		}

		// 获取原始请求的查询参数
		originalQuery := c.Request().URL.RawQuery

		// 根据最后一个路由进行不同处理
		var baseURL string
		switch lastRoute {
		case "sse":
			// 对于SSE，使用完整的SSE URL
			baseURL = instance.GetSSEUrl()
		case "message":
			// 对于message，使用完整的Message URL
			baseURL = instance.GetMessageUrl()
			c.Logger().Infof("Message URL: %s", baseURL)
		default:
			// 对于其他路由，使用基础URL加上完整路径
			if url := instance.GetUrl(); url != "" {
				// 移除URL末尾的斜杠，避免双斜杠
				baseURL = strings.TrimRight(url, "/")
				if remainingPath != "/" {
					baseURL += remainingPath
				}
			} else {
				return c.String(http.StatusNotFound, "Service not available")
			}
		}

		// 构建目标URL，保留原始查询参数
		targetURL := baseURL
		if originalQuery != "" {
			// 检查baseURL是否已经包含查询参数
			if strings.Contains(baseURL, "?") {
				targetURL = baseURL + "&" + originalQuery
			} else {
				targetURL = baseURL + "?" + originalQuery
			}
		}

		c.Logger().Infof("Proxy request: %s, target URL: %s, lastRoute: %s, query: %s",
			c.Request().URL, targetURL, lastRoute, originalQuery)

		// 创建新的请求
		req, err := http.NewRequest(c.Request().Method, targetURL, c.Request().Body)
		if err != nil {
			return err
		}

		// 复制原始请求的 header
		for k, v := range c.Request().Header {
			req.Header[k] = v
		}

		// 发送请求
		client := &http.Client{
			Transport: &http.Transport{
				ForceAttemptHTTP2: false,
			},
		}
		resp, err := client.Do(req)
		if err != nil {
			return err
		}
		defer resp.Body.Close()

		// 复制响应 header
		for k, v := range resp.Header {
			c.Response().Header()[k] = v
		}

		// 对于 SSE 请求的特殊处理
		if isSSE(resp.Header) {
			c.Response().Header().Set("Content-Type", "text/event-stream")
			c.Response().Header().Set("Cache-Control", "no-cache")
			c.Response().Header().Set("Connection", "keep-alive")
			c.Response().WriteHeader(resp.StatusCode)
			c.Response().Flush()

			reader := bufio.NewReader(resp.Body)
			var currentEvent string

			for {
				line, err := reader.ReadString('\n')
				if err != nil {
					if err == io.EOF {
						break
					}
					return err
				}

				line = strings.TrimSpace(line)
				if line == "" {
					continue
				}

				// 处理事件行
				if strings.HasPrefix(line, "event: ") {
					currentEvent = strings.TrimPrefix(line, "event: ")
					fmt.Fprintf(c.Response(), "event: %s\n", currentEvent)
				} else if strings.HasPrefix(line, "data: ") {
					data := strings.TrimPrefix(line, "data: ")

					// 如果是endpoint事件，添加服务名前缀
					if currentEvent == "endpoint" && strings.HasPrefix(data, "/message") {
						data = fmt.Sprintf("/%s%s", serviceName, data)
					}

					fmt.Fprintf(c.Response(), "data: %s\n\n", data)
				} else {
					fmt.Fprintf(c.Response(), "%s\n", line)
				}
				c.Response().Flush()
			}
			return nil
		}

		// 非 SSE 请求的普通处理
		c.Response().WriteHeader(resp.StatusCode)
		_, err = io.Copy(c.Response().Writer, resp.Body)
		return err
	}
}

// isSSE 检查是否是 SSE 请求
func isSSE(header http.Header) bool {
	return strings.Contains(header.Get("Content-Type"), "text/event-stream")
}

func main() {
	cfgDir := "/etc/proxy"
	if _, err := os.Stat(cfgDir); os.IsNotExist(err) {
		cfgDir = "."
	}
	cfg := config.Config{
		SessionGCInterval: 5 * time.Minute,
		ConfigDirPath:     cfgDir,
	}

	// 初始化服务管理器
	manager = initServerManager(cfg)
	if err := manager.loadConfig(); err != nil {
		panic(fmt.Errorf("failed to load config: %w", err))
	}

	// 创建proxy log
	proxyLogFile, err := xlog.CreateLogFile(cfg.ConfigDirPath, "plugin-proxy.log")
	if err != nil {
		panic(fmt.Errorf("failed to create proxy log file: %w", err))
	}

	// 创建 Echo 实例
	e := echo.New()
	e.Logger.SetLevel(1)
	e.Logger.SetOutput(io.MultiWriter(proxyLogFile, os.Stdout))

	// 添加中间件
	e.Use(middleware.Logger())
	e.Use(middleware.Recover())

	// 注册路由
	e.POST("/deploy", manager.handleDeploy)
	e.Any("/*", proxyHandler())

	e.GET("/sse", manager.handleGlobalSSE)
	e.POST("/message", manager.handleGlobalMessage)

	// 启动服务器
	e.Logger.Fatal(e.Start(":8080"))
}

// 全局SSE，这里返回所有MCP服务的SSE事件
func (m *ServerManager) handleGlobalSSE(c echo.Context) error {
	c.Logger().Infof("Global SSE request: %v", c.Request().Body)
	c.Response().Header().Set("Content-Type", "text/event-stream")
	c.Response().Header().Set("Cache-Control", "no-cache")
	c.Response().Header().Set("Connection", "keep-alive")

	// create proxy session
	session := m.mcpServiceMgr.CreateProxySession()
	defer m.mcpServiceMgr.CloseProxySession(session.Id)

	// 返回endpoint事件
	c.Response().WriteHeader(http.StatusOK)
	fmt.Fprintf(c.Response(), "event: endpoint\ndata: /message?sessionId=%s\n\n", session.Id)
	c.Response().Flush()

	// 转发所有SSE事件
	for {
		select {
		case <-c.Request().Context().Done():
			return nil
		case event := <-session.GetEventChan():
			if _, err := fmt.Fprintf(c.Response(), "%s", event); err != nil {
				return err
			}
			c.Response().Flush()
		}
	}
}

// 全局MESSAGE，这里将POST请求转发到所有MCP服务
func (m *ServerManager) handleGlobalMessage(c echo.Context) error {
	c.Logger().Infof("Global message: %v", c.Request().Body)
	sessionId := c.QueryParam("sessionId")
	if sessionId == "" {
		return c.String(http.StatusBadRequest, "missing sessionId")
	}

	// 获取session
	session, exists := m.mcpServiceMgr.GetProxySession(sessionId)
	if !exists {
		return c.String(http.StatusNotFound, "session not found")
	}

	// 读取请求体
	body, err := io.ReadAll(c.Request().Body)
	if err != nil {
		return err
	}

	// 转发消息
	c.Logger().Infof("Global message from session %s: %s", session.Id, string(body))
	mcpServices := m.mcpServiceMgr.GetMcpServices(c.Logger())
	for name, instance := range mcpServices {
		c.Logger().Infof("Forwarding message to %s", name)
		// 记录发送的消息
		session.AddMessage(name, string(body), "send")
		if err := instance.SendMessage(string(body)); err != nil {
			c.Logger().Errorf("Failed to forward message to %s: %v", name, err)
		}
	}

	return nil
}
