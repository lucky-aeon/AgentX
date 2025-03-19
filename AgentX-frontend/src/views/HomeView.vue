<script setup>
import { ref, onMounted } from 'vue'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import { useChatStore } from '@/stores/chat'

// 获取聊天状态
const chatStore = useChatStore()
// 输入消息
const inputMessage = ref('')
// 消息容器ref
const messagesContainer = ref(null)

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim()) return
  
  const message = inputMessage.value
  inputMessage.value = ''
  
  await chatStore.sendMessage(message)
  // 滚动到底部
  scrollToBottom()
}

// 处理按键事件
const handleKeyDown = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  setTimeout(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }, 50)
}

// 监听消息列表变化，自动滚动到底部
onMounted(() => {
  scrollToBottom()
})
</script>

<template>
  <main class="chat-main">
    <div class="chat-container">
      <div class="chat-header">
        <h1>AgentX 智能助手</h1>
      </div>
      
      <div class="messages-container" ref="messagesContainer">
        <div v-if="chatStore.messages.length === 0" class="empty-chat">
          <p>开始与AI助手对话吧</p>
        </div>
        <div v-else class="messages">
          <div 
            v-for="(message, index) in chatStore.messages" 
            :key="index" 
            :class="['message', message.role]"
          >
            <div class="message-content">{{ message.content }}</div>
            <div class="message-meta">
              <span class="time">{{ message.time }}</span>
              <span v-if="message.provider" class="provider">{{ message.provider }}</span>
              <span v-if="message.model" class="model">{{ message.model }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="input-container">
        <InputText 
          v-model="inputMessage" 
          placeholder="输入消息..." 
          class="message-input"
          @keydown="handleKeyDown"
        />
        <Button 
          :disabled="chatStore.loading || !inputMessage.trim()" 
          icon="pi pi-send" 
          class="send-button p-button-rounded" 
          @click="sendMessage"
        />
      </div>
    </div>
    
    <div v-if="chatStore.loading" class="loading-overlay">
      <ProgressSpinner strokeWidth="4" />
    </div>
  </main>
</template>

<style scoped>
.chat-main {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  height: 100vh;
  background-color: var(--color-bg);
  position: relative;
  padding: 0;
  box-sizing: border-box;
  overflow: hidden;
}

.chat-container {
  width: 100%;
  max-width: 500px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: white;
  border-radius: 0;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05), 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  margin: 0 auto;
}

.chat-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  background-color: white;
}

.chat-header h1 {
  font-size: 1.5rem;
  color: var(--color-primary);
  margin: 0;
  text-align: center;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #f7f9fc;
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #888;
  font-size: 1.1rem;
  text-align: center;
  padding: 0 20px;
}

.messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  padding: 0;
}

.message {
  padding: 12px 16px;
  border-radius: 18px;
  max-width: 85%;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message.user {
  align-self: flex-end;
  background-color: var(--color-primary);
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant {
  align-self: flex-start;
  background-color: white;
  border-bottom-left-radius: 4px;
}

.message.system {
  align-self: center;
  background-color: #fff0e0;
  font-style: italic;
  max-width: 90%;
  text-align: center;
}

.message-meta {
  font-size: 0.75rem;
  margin-top: 6px;
  display: flex;
  gap: 8px;
}

.message.user .message-meta {
  color: rgba(255, 255, 255, 0.8);
}

.message.assistant .message-meta {
  color: #888;
}

.input-container {
  display: flex;
  gap: 10px;
  padding: 16px 16px 20px 16px;
  background-color: white;
  border-top: 1px solid var(--color-border);
  position: relative;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border-radius: 24px;
  border: 1px solid var(--color-border);
  font-size: 1rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.message-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.2);
}

.send-button {
  width: 48px;
  height: 48px;
  background-color: var(--color-primary) !important;
  color: white !important;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 5;
}

@media (max-width: 600px) {
  .chat-container {
    width: 100%;
    height: 100vh;
    border-radius: 0;
    margin: 0;
    max-width: none;
  }
  
  .chat-main {
    padding: 0;
  }
  
  .message {
    max-width: 90%;
  }
}
</style>
