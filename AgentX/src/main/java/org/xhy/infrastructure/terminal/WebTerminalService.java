package org.xhy.infrastructure.terminal;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.docker.DockerService;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/** Web终端服务 */
@Service
public class WebTerminalService {

    private static final Logger logger = LoggerFactory.getLogger(WebTerminalService.class);
    
    private final DockerService dockerService;
    private final ContainerDomainService containerDomainService;
    private final ConcurrentHashMap<String, TerminalSession> activeSessions = new ConcurrentHashMap<>();

    public WebTerminalService(DockerService dockerService, ContainerDomainService containerDomainService) {
        this.dockerService = dockerService;
        this.containerDomainService = containerDomainService;
    }

    /** 创建终端会话
     * 
     * @param sessionId 会话ID
     * @param containerId 数据库容器ID
     * @param webSocketSession WebSocket会话
     * @return 是否创建成功 */
    public boolean createTerminalSession(String sessionId, String containerId, WebSocketSession webSocketSession) {
        try {
            // 根据数据库容器ID查找容器实体
            ContainerEntity containerEntity = containerDomainService.getContainerById(containerId);
            if (containerEntity == null) {
                sendMessage(webSocketSession, "错误: 容器不存在\r\n");
                return false;
            }

            // 获取实际的Docker容器ID
            String dockerContainerId = containerEntity.getDockerContainerId();
            if (dockerContainerId == null || dockerContainerId.trim().isEmpty()) {
                sendMessage(webSocketSession, "错误: Docker容器ID为空\r\n");
                return false;
            }

            // 检查并启动容器（如果需要）
            String containerStatus = dockerService.getContainerStatus(dockerContainerId);
            logger.info("容器当前状态: {} -> {}", dockerContainerId, containerStatus);
            
            if (!"running".equalsIgnoreCase(containerStatus)) {
                sendMessage(webSocketSession, "容器未运行，正在启动容器...\r\n");
                try {
                    dockerService.startContainer(dockerContainerId);
                    sendMessage(webSocketSession, "容器启动成功\r\n");
                    // 等待容器完全启动
                    Thread.sleep(2000);
                } catch (Exception e) {
                    logger.error("启动容器失败: {}", dockerContainerId, e);
                    sendMessage(webSocketSession, "错误: 启动容器失败 - " + e.getMessage() + "\r\n");
                    return false;
                }
            }
            
            // 再次检查容器是否可以执行命令
            if (!dockerService.canExecuteCommands(dockerContainerId)) {
                sendMessage(webSocketSession, "错误: 容器启动后仍无法连接\r\n");
                return false;
            }

            // 创建终端会话（使用Docker容器ID）
            TerminalSession terminalSession = new TerminalSession(sessionId, dockerContainerId, webSocketSession);
            terminalSession.start();
            
            activeSessions.put(sessionId, terminalSession);
            logger.info("创建终端会话成功: {} -> {} (Docker: {})", sessionId, containerId, dockerContainerId);
            
            return true;
        } catch (Exception e) {
            logger.error("创建终端会话失败: {} -> {}", sessionId, containerId, e);
            sendMessage(webSocketSession, "错误: " + e.getMessage() + "\r\n");
            return false;
        }
    }

    /** 发送命令到终端
     * 
     * @param sessionId 会话ID
     * @param command 命令 */
    public void sendCommand(String sessionId, String command) {
        TerminalSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.sendCommand(command);
        }
    }

    /** 关闭终端会话
     * 
     * @param sessionId 会话ID */
    public void closeTerminalSession(String sessionId) {
        TerminalSession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.close();
            logger.info("关闭终端会话: {}", sessionId);
        }
    }

    /** 发送消息到WebSocket */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            logger.error("发送WebSocket消息失败", e);
        }
    }

    /** 测试shell是否在容器中可用 */
    private boolean testShellAvailable(DockerClient dockerClient, String containerId, String shell) {
        try {
            logger.debug("测试shell: {} 通过创建exec会话", shell);
            // 直接尝试创建exec会话来测试shell是否存在
            ExecCreateCmdResponse execCreateCmd = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd(shell, "-c", "echo 'shell_test_ok'")
                    .exec();

            String execId = execCreateCmd.getId();
            
            // 启动并等待执行完成
            StringBuilder output = new StringBuilder();
            dockerClient.execStartCmd(execId)
                    .withDetach(false)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            if (frame.getPayload() != null) {
                                output.append(new String(frame.getPayload()));
                            }
                        }
                    })
                    .awaitCompletion(3, TimeUnit.SECONDS);

            // 检查执行结果
            var inspectResponse = dockerClient.inspectExecCmd(execId).exec();
            Integer exitCode = inspectResponse.getExitCode();
            
            boolean success = exitCode != null && exitCode == 0 && output.toString().contains("shell_test_ok");
            logger.debug("Shell {} 测试结果: exitCode={}, output={}, success={}", shell, exitCode, output.toString().trim(), success);
            return success;
            
        } catch (Exception e) {
            logger.debug("测试Shell {} 失败: {}", shell, e.getMessage());
            return false;
        }
    }

    /** 终端会话类 */
    private class TerminalSession {
        private final String sessionId;
        private final String containerId;
        private final WebSocketSession webSocketSession;
        private String execId;
        private PipedOutputStream inputStream;
        private boolean active = false;

        public TerminalSession(String sessionId, String containerId, WebSocketSession webSocketSession) {
            this.sessionId = sessionId;
            this.containerId = containerId;
            this.webSocketSession = webSocketSession;
        }

        public void start() throws Exception {
            logger.info("开始创建终端会话: {} -> {}", sessionId, containerId);
            
            // 创建Docker exec会话
            DockerClient dockerClient = dockerService.getDockerClient();
            
            try {
                // 测试可用的shell (优先使用sh，因为轻量级容器通常只有sh)
                String[] shells = {"/bin/sh", "/bin/bash"};
                String selectedShell = null;
                
                for (String shell : shells) {
                    logger.debug("测试shell是否可用: {}", shell);
                    if (testShellAvailable(dockerClient, containerId, shell)) {
                        selectedShell = shell;
                        logger.info("选择shell: {}", shell);
                        break;
                    } else {
                        logger.warn("Shell {} 不可用", shell);
                    }
                }
                
                if (selectedShell == null) {
                    throw new Exception("容器中没有可用的shell (/bin/bash 或 /bin/sh)");
                }
                
                // 创建真正的exec会话
                logger.debug("创建exec会话使用shell: {}", selectedShell);
                ExecCreateCmdResponse execCreateCmd = dockerClient.execCreateCmd(containerId)
                        .withAttachStdout(true)
                        .withAttachStderr(true)
                        .withAttachStdin(true)
                        .withTty(true)
                        .withWorkingDir("/")  // 设置工作目录为根目录，避免特定应用目录的干扰
                        .withCmd(selectedShell)
                        .exec();
                
                execId = execCreateCmd.getId();
                logger.info("成功创建exec会话，execId: {}", execId);
                
                // 创建输入管道
                PipedInputStream pipedInputStream = new PipedInputStream();
                inputStream = new PipedOutputStream(pipedInputStream);
                
                // 启动exec会话
                logger.debug("启动exec会话: {}", execId);
                dockerClient.execStartCmd(execId)
                        .withDetach(false)
                        .withTty(true)
                        .withStdIn(pipedInputStream)
                        .exec(new ResultCallback.Adapter<Frame>() {
                            private long lastOutputTime = 0;
                            private String lastOutput = "";
                            private int consecutivePrompts = 0;
                            private boolean suppressOutput = false;
                            
                            @Override
                            public void onNext(Frame frame) {
                                if (frame != null && frame.getPayload() != null) {
                                    String output = new String(frame.getPayload());
                                    long currentTime = System.currentTimeMillis();
                                    
                                    // 简化检测逻辑：只检测空的shell提示符行
                                    String trimmedOutput = output.trim();
                                    boolean isEmptyPrompt = trimmedOutput.matches("^[/#$]\\s*$") || 
                                                          trimmedOutput.matches("^\\s*[/#$]\\s*$");
                                    
                                    // 如果是连续的空提示符，且时间间隔很短
                                    if (isEmptyPrompt && (currentTime - lastOutputTime) < 100) {
                                        consecutivePrompts++;
                                        
                                        if (consecutivePrompts > 3) {
                                            if (!suppressOutput) {
                                                logger.warn("检测到连续空提示符输出，启用抑制模式");
                                                suppressOutput = true;
                                            }
                                            return; // 跳过连续的空提示符
                                        }
                                    } else {
                                        // 重置计数器和抑制状态
                                        if (consecutivePrompts > 0 || suppressOutput) {
                                            consecutivePrompts = 0;
                                            if (suppressOutput) {
                                                logger.info("检测到有效输出，退出抑制模式");
                                                suppressOutput = false;
                                            }
                                        }
                                    }
                                    
                                    lastOutput = output;
                                    lastOutputTime = currentTime;
                                    
                                    // 只在调试模式下记录输出内容
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("终端输出 (连续提示符:{}): {}", consecutivePrompts, 
                                                   output.replace("\n", "\\n").replace("\r", "\\r"));
                                    }
                                    
                                    sendMessage(webSocketSession, output);
                                }
                            }
                            
                            @Override
                            public void onError(Throwable throwable) {
                                logger.error("终端会话错误: {}", sessionId, throwable);
                                sendMessage(webSocketSession, "\r\n[终端连接出错: " + throwable.getMessage() + "]\r\n");
                                closeTerminalSession(sessionId);
                            }
                            
                            @Override
                            public void onComplete() {
                                logger.info("终端会话结束: {}", sessionId);
                                closeTerminalSession(sessionId);
                            }
                        });
                
                active = true;
                
                // 发送初始提示
                sendMessage(webSocketSession, "\033[2J\033[H"); // 清屏并移动光标到左上角
                sendMessage(webSocketSession, "Welcome to AgentX Container Terminal\r\n");
                sendMessage(webSocketSession, "Container: " + containerId + "\r\n");
                sendMessage(webSocketSession, "Shell: " + selectedShell + "\r\n");
                sendMessage(webSocketSession, "Working Directory: /\r\n");
                sendMessage(webSocketSession, "Type 'exit' to close the terminal.\r\n\r\n");
                
                logger.info("终端会话启动成功: {}", sessionId);
                
            } catch (Exception e) {
                logger.error("创建终端会话失败: {}", sessionId, e);
                throw new Exception("无法创建终端会话: " + e.getMessage(), e);
            }
        }

        public void sendCommand(String command) {
            if (active && inputStream != null) {
                try {
                    logger.debug("发送命令到容器: {}", command);
                    
                    // 直接发送命令，不添加额外的换行符（xterm.js会处理回车）
                    inputStream.write(command.getBytes());
                    inputStream.flush();
                } catch (IOException e) {
                    logger.error("发送命令失败: {}", sessionId, e);
                }
            }
        }

        public void close() {
            active = false;
            
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                logger.error("关闭输入流失败: {}", sessionId, e);
            }
            
            if (execId != null) {
                try {
                    // 尝试优雅地终止exec会话
                    DockerClient dockerClient = dockerService.getDockerClient();
                    dockerClient.execStartCmd(execId).exec(new ResultCallback.Adapter<Frame>()).close();
                } catch (Exception e) {
                    logger.warn("终止exec会话失败: {}", sessionId, e);
                }
            }
        }
    }
}