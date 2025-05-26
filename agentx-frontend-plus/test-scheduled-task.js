// 简单的定时任务API测试
const testScheduledTaskAPI = async () => {
  console.log("开始测试定时任务API...");
  
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
    const response = await fetch('http://localhost:8080/api/scheduled-task', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(testRequest)
    });
    
    const result = await response.json();
    console.log("API响应:", result);
    
    if (response.ok) {
      console.log("✅ 定时任务API测试成功");
    } else {
      console.log("❌ 定时任务API测试失败:", result.message);
    }
  } catch (error) {
    console.error("❌ API调用失败:", error.message);
  }
};

// 如果在浏览器环境中运行
if (typeof window !== 'undefined') {
  window.testScheduledTaskAPI = testScheduledTaskAPI;
  console.log("测试函数已添加到window对象，可以在控制台运行: testScheduledTaskAPI()");
}

// 如果在Node.js环境中运行
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { testScheduledTaskAPI };
} 