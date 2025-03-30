package org.xhy.domain.plugins.config;

import java.util.Map;

import org.xhy.domain.plugins.constant.PluginType;
import org.xhy.infrastructure.exception.BusinessException;

/**
 * STDIO插件配置
 */
public class StdioPluginConfig extends PluginConfig {
    private String command;
    private String[] args;
    private Map<String, String> env;
    
    public StdioPluginConfig(String command, String[] args, Map<String, String> env) {
        this.command = command;
        this.args = args;
        this.env = env;
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public String[] getArgs() {
        return args;
    }
    
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    public Map<String, String> getEnv() {
        return env;
    }
    
    public void setEnv(Map<String, String> env) {
        this.env = env;
    }
    
    @Override
    public boolean validate() {
        if (!getConfigType().equals(PluginType.STDIO.getName())) {
            throw new BusinessException("配置类型不匹配, 预期类型: " + PluginType.STDIO.getName());
        }
        if(!(command != null && !command.trim().isEmpty())) {
            throw new BusinessException("STDIO插件配置无效");
        }
        return true;
    }
}
