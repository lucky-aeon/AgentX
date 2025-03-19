<template>
  <div class="about">
    <h1>关于 AgentX</h1>
    <div class="content">
      <p>AgentX 是一个基于大语言模型的智能对话系统平台，支持多种LLM服务商，提供灵活易用的API接口。</p>
      
      <h2>主要功能</h2>
      <ul>
        <li>支持多种LLM服务商集成（SiliconFlow等）</li>
        <li>统一的对话API接口</li>
        <li>灵活的会话管理</li>
        <li>可扩展的插件系统</li>
      </ul>
      
      <h2>技术栈</h2>
      <div class="tech-stack">
        <div class="tech-group">
          <h3>前端</h3>
          <ul>
            <li>Vue 3.x</li>
            <li>TypeScript 4.x</li>
            <li>PrimeVue 3.x</li>
            <li>Pinia 2.x</li>
          </ul>
        </div>
        <div class="tech-group">
          <h3>后端</h3>
          <ul>
            <li>Java 1.8</li>
            <li>Spring Boot 2.7.x</li>
            <li>PostgreSQL 14.x</li>
            <li>MyBatis Plus 3.5.x</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.about {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
}

.content {
  background-color: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

h2 {
  margin-top: 20px;
  color: #3f51b5;
}

.tech-stack {
  display: flex;
  gap: 40px;
  margin-top: 20px;
}

.tech-group {
  flex: 1;
}

h3 {
  color: #673ab7;
}

ul {
  padding-left: 20px;
}

li {
  margin-bottom: 8px;
}
</style>
