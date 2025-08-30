// 简单的定时任务API测试
const testScheduledTaskAPI = async () => {
 
  
  // 测试数据
  const testRequest = {
    agentId: "test-agent-id",
    sessionId: "test-session-id", 
    content: "测试定时任务内容",
    repeatType: "DAILY",
    repeatConfig: {
      executeDateTime: new Date().toISOString(),
      executeTime: "09:00"
    }
  };
  
  try {
    // 测试创建定时任务
    const response = await fetch('http://localhost:8080/api/scheduled-tasks', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(testRequest)
    });
    
    const result = await response.json();
 
    
    if (response.ok) {
 
    } else {
 
    }
  } catch (error) {
 
  }
};

// 如果在浏览器环境中运行
if (typeof window !== 'undefined') {
  window.testScheduledTaskAPI = testScheduledTaskAPI;
 
}

// 如果在Node.js环境中运行
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { testScheduledTaskAPI };
} 