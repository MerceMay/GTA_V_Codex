<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  breaks: true
})
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const chatContainer = ref(null)
let abortController = null

onMounted(() => {
  messages.value.push({
    role: 'ai',
    content: "Agent Mode activated. I can perform tasks and answer complex queries using tools. How can I assist?",
    renderedContent: md.render("Agent Mode activated. I can perform tasks and answer complex queries using tools. How can I assist?")
  })
})

onUnmounted(() => {
  closeConnection()
})

const closeConnection = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return

  const userMsg = inputMessage.value.trim()
  messages.value.push({
    role: 'user',
    content: userMsg,
    renderedContent: md.render(userMsg)
  })
  
  inputMessage.value = ''
  isLoading.value = true
  scrollToBottom()

  const aiMsgIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    renderedContent: ''
  })

  closeConnection()

  // Agent Endpoint: /api/ai/chat/agent?message=...
  const url = `/api/ai/chat/agent?message=${encodeURIComponent(userMsg)}`
  
  abortController = new AbortController()

  try {
    const response = await fetch(url, {
      signal: abortController.signal,
      headers: {
        'Accept': 'text/event-stream'
      }
    })

    if (!response.ok) {
       throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk
      
      const events = buffer.split('\n\n')
      buffer = events.pop()

      for (const event of events) {
         if (!event.trim()) continue
         
         const lines = event.split('\n')
         let eventData = ''
         let isFirstDataLine = true

         for (const line of lines) {
             const dataPrefix = 'data:'
             if (line.startsWith(dataPrefix)) {
                 let content = line.substring(dataPrefix.length)
                 // Unescape literal \n sequences (often sent by backend serializers) to real newlines
                 content = content.replace(/\\n/g, '\n')
                 
                 if (!isFirstDataLine) {
                     eventData += '\n'
                 }
                 eventData += content
                 isFirstDataLine = false
             }
         }
         
         if (!isFirstDataLine) {
             messages.value[aiMsgIndex].content += eventData
             messages.value[aiMsgIndex].renderedContent = md.render(messages.value[aiMsgIndex].content)
             scrollToBottom()
         }
      }
    }
  } catch (err) {
    if (err.name === 'AbortError') {
      console.log('Stream aborted')
    } else {
      console.error('Stream error:', err)
      messages.value[aiMsgIndex].content += '\n[Error: Connection failed]'
      messages.value[aiMsgIndex].renderedContent = md.render(messages.value[aiMsgIndex].content)
    }
  } finally {
    closeConnection()
    isLoading.value = false
  }
}
</script>

<template>
  <div class="chat-container agent-theme">
    <div class="chat-header">
      <h2>Agent Workspace</h2>
    </div>
    
    <div class="messages" ref="chatContainer">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        class="message-wrapper"
        :class="msg.role"
      >
        <div class="avatar">
          {{ msg.role === 'user' ? '👤' : '🕵️' }}
        </div>
        <div class="message-content markdown-body" v-html="msg.renderedContent"></div>
      </div>
      <div v-if="isLoading && !abortController" class="loading">Agent is thinking...</div>
    </div>

    <div class="input-area">
      <input 
        v-model="inputMessage" 
        @keyup.enter="sendMessage"
        type="text" 
        placeholder="Task for Agent..."
        :disabled="isLoading"
      />
      <button @click="sendMessage" :disabled="isLoading || !inputMessage.trim()">
        Execute
      </button>
    </div>
  </div>
</template>

<style scoped>
/* Reuse basic chat styles but maybe tweak colors for 'Agent' feel */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  background: white;
  box-shadow: 0 0 20px rgba(0,0,0,0.05);
}

.agent-theme .chat-header {
  background: #333;
  color: white;
}

.chat-header {
  padding: 1rem;
  border-bottom: 1px solid #eee;
  text-align: center;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message-wrapper {
  display: flex;
  gap: 10px;
  max-width: 80%;
}

.message-wrapper.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-wrapper.ai {
  align-self: flex-start;
}

.avatar {
  width: 35px;
  height: 35px;
  background: #eee;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.message-content {
  padding: 0.8rem 1rem;
  border-radius: 12px;
  line-height: 1.5;
  font-size: 0.95rem;
}

.user .message-content {
  background-color: var(--user-msg-bg);
  border-top-right-radius: 2px;
}

.ai .message-content {
  background-color: #f0f0f0; /* Slightly different for agent */
  border-top-left-radius: 2px;
  border: 1px solid #ddd;
}

.input-area {
  padding: 1rem;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
}

input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 1rem;
}

input:focus {
  border-color: #333;
}

button {
  padding: 0 20px;
  background-color: #333;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  background-color: #555;
}
</style>
