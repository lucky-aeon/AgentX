package config

import (
	"path/filepath"
	"time"
)

const COMMAND_SUPERGATEWA = "/usr/local/bin/supergateway"

type Config struct {
	ConfigDirPath       string        // 配置文件路径
	SessionGCInterval   time.Duration // Session GC间隔
	McpServiceMgrConfig McpServiceMgrConfig
}

type McpServiceMgrConfig struct {
	McpServiceRetryCount int // 服务重试次数，服务挂掉后会重试
}

func (c *McpServiceMgrConfig) GetMcpServiceRetryCount() int {
	if c.McpServiceRetryCount == 0 {
		return 1
	}
	return c.McpServiceRetryCount
}

// MCP Config path
const MCP_CONFIG_PATH = "mcp_servers.json"

func (c *Config) GetMcpConfigPath() string {
	return filepath.Join(c.ConfigDirPath, MCP_CONFIG_PATH)
}

// MCPServerConfig 定义单个MCP服务器的配置
type MCPServerConfig struct {
	URL     string            `json:"url,omitempty"`
	Command string            `json:"command,omitempty"`
	Args    []string          `json:"args,omitempty"`
	Env     map[string]string `json:"env,omitempty"`
}
