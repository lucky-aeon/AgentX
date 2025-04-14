package service

import (
	"encoding/json"
	"fmt"
	"os"
	"sync"
	"time"

	"github.com/google/uuid"
	"github.com/labstack/echo/v4"
	"github.com/lucky-aeon/agentx/plugin-helper/config"
)

type ServiceManagerI interface {
	DeployServer(logger echo.Logger, name string, config config.MCPServerConfig) error
	StopServer(logger echo.Logger, name string)
	ListServerConfig(logger echo.Logger) map[string]config.MCPServerConfig
	GetMcpService(logger echo.Logger, name string) (ExportMcpService, error)
	GetMcpServices(logger echo.Logger) map[string]ExportMcpService
	CreateProxySession() *Session
	GetProxySession(id string) (*Session, bool)
	CloseProxySession(id string)
}

type PortManagerI interface {
	getNextAvailablePort() int
	releasePort(port int)
}

// ServiceManager 管理所有运行的服务
type ServiceManager struct {
	sync.RWMutex
	servers   map[string]*McpService
	usedPorts map[int]bool // 记录已使用的端口
	nextPort  int          // 下一个可用端口
	portMutex sync.Mutex   // 端口分配的互斥锁

	// all session-> mcp service
	sessions map[string]*McpService

	// proxy sessions
	proxySessionsMutex sync.RWMutex
	proxySessions     map[string]*Session

	cfg config.Config
}

func NewServiceManager(cfg config.Config) *ServiceManager {
	if cfg.SessionGCInterval == 0 {
		cfg.SessionGCInterval = 5 * time.Minute
	}
	mgr := &ServiceManager{
		cfg:           cfg,
		servers:       make(map[string]*McpService),
		usedPorts:     make(map[int]bool),
		nextPort:      10000,
		sessions:      make(map[string]*McpService),
		proxySessions: make(map[string]*Session),
	}
	go mgr.loopGC()
	return mgr
}

func (m *ServiceManager) DeployServer(logger echo.Logger, name string, config config.MCPServerConfig) error {
	m.Lock()
	defer m.Unlock()

	if mcpService, exists := m.servers[name]; exists {
		logger.Errorf("服务 %s 已存在", name)

		// 重启服务
		mcpService.Restart(logger)
		return nil
	}

	// 创建服务实例
	instance := NewMcpService(name, config, m, m.cfg)
	if err := instance.Start(logger); err != nil {
		logger.Errorf("Failed to start service %s: %v", name, err)
		return err
	}
	m.servers[name] = instance
	m.saveConfig()
	return nil
}

func (m *ServiceManager) ListServerConfig(logger echo.Logger) map[string]config.MCPServerConfig {
	m.RLock()
	defer m.RUnlock()
	config := make(map[string]config.MCPServerConfig)
	for name, instance := range m.servers {
		config[name] = instance.Config
	}
	return config
}

func (m *ServiceManager) GetMcpService(logger echo.Logger, name string) (ExportMcpService, error) {
	instance, err := m.getMcpService(name)
	if err != nil {
		logger.Errorf("获取服务 %s 失败: %v", name, err)
		return nil, err
	}
	return instance, nil
}

func (m *ServiceManager) getMcpService(name string) (*McpService, error) {
	m.RLock()
	defer m.RUnlock()
	if instance, exists := m.servers[name]; exists {
		return instance, nil
	}
	return nil, fmt.Errorf("服务 %s 不存在", name)
}

func (m *ServiceManager) StopServer(logger echo.Logger, name string) {
	mcp, err := m.getMcpService(name)
	if err != nil {
		logger.Errorf("获取服务 %s 失败: %v", name, err)
		return
	}
	mcp.Stop(logger)
}

func (m *ServiceManager) saveConfig() error {
	config := make(map[string]config.MCPServerConfig)
	for name, instance := range m.servers {
		config[name] = instance.Config
	}

	data, err := json.MarshalIndent(config, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(m.cfg.GetMcpConfigPath(), data, 0644)
}

// getNextAvailablePort 获取下一个可用端口
func (m *ServiceManager) getNextAvailablePort() int {
	m.portMutex.Lock()
	defer m.portMutex.Unlock()
	for m.usedPorts[m.nextPort] {
		m.nextPort++
	}
	port := m.nextPort
	m.usedPorts[port] = true
	m.nextPort++
	return port
}

// releasePort 释放端口
func (m *ServiceManager) releasePort(port int) {
	m.portMutex.Lock()
	delete(m.usedPorts, port)
	m.portMutex.Unlock()
}

func (m *ServiceManager) GetMcpServices(logger echo.Logger) map[string]ExportMcpService {
	m.RLock()
	defer m.RUnlock()
	exportServices := make(map[string]ExportMcpService)
	for name, instance := range m.servers {
		exportServices[name] = instance
	}
	return exportServices
}

// CreateProxySession 创建一个新的代理会话
func (m *ServiceManager) CreateProxySession() *Session {
	m.proxySessionsMutex.Lock()
	defer m.proxySessionsMutex.Unlock()

	session := NewSession(uuid.New().String())
	m.proxySessions[session.Id] = session

	// 订阅所有MCP服务的SSE事件
	m.RLock()
	for name, instance := range m.servers {
		session.SubscribeSSE(name, instance.GetSSEUrl())
	}
	m.RUnlock()

	return session
}

// CloseProxySession 关闭代理会话
func (m *ServiceManager) CloseProxySession(id string) {
	m.proxySessionsMutex.Lock()
	defer m.proxySessionsMutex.Unlock()

	if session, exists := m.proxySessions[id]; exists {
		session.Close()
		delete(m.proxySessions, id)
	}
}

// GetProxySession 获取代理会话
func (m *ServiceManager) GetProxySession(id string) (*Session, bool) {
	m.proxySessionsMutex.RLock()
	defer m.proxySessionsMutex.RUnlock()

	session, exists := m.proxySessions[id]
	return session, exists
}

// GC长时间未使用的Session
func (m *ServiceManager) loopGC() {
	tick := time.NewTicker(m.cfg.SessionGCInterval)
	for {
		select {
		case _, ok := <-tick.C:
			if !ok {
				return
			}
			m.RLock()
			// GC MCP services sessions
			for _, instance := range m.servers {
				instance.GC()
			}
			
			// GC proxy sessions
			m.proxySessionsMutex.Lock()
			for id, session := range m.proxySessions {
				if time.Since(session.LastReceiveTime) > 5*m.cfg.SessionGCInterval {
					delete(m.proxySessions, id)
				}
			}
			m.proxySessionsMutex.Unlock()
			m.RUnlock()
		}
	}
}
