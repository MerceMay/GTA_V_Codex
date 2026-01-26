<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({
  breaks: true
})
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const chatId = ref('')
const chatContainer = ref(null)
let abortController = null

onMounted(() => {
  // Generate a random chat ID
  chatId.value = 'chat-' + Math.random().toString(36).substr(2, 9)
  
  // Add initial welcome message
  messages.value.push({
    role: 'ai',
    content: "Hey there! I'm your GTA V expert. Ask me anything about missions, heists, or where to find those elusive spaceship parts.",
    renderedContent: md.render("Hey there! I'm your GTA V expert. Ask me anything about missions, heists, or where to find those elusive spaceship parts.")
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

  // Prepare AI message placeholder
  const aiMsgIndex = messages.value.length
  messages.value.push({
    role: 'ai',
    content: '',
    renderedContent: ''
  })

  // Close previous connection if any
  closeConnection()

  // Connect to SSE
  // URL: /api/ai/chat/sse?message=...&chatId=...
  const url = `/api/ai/chat/sse?message=${encodeURIComponent(userMsg)}&chatId=${chatId.value}`
  
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
      // The last element is potentially an incomplete event
      buffer = events.pop()

      for (const event of events) {
         if (!event.trim()) continue // Skip empty events
         
         const lines = event.split('\n')
         let eventData = ''
         let isFirstDataLine = true
         
         for (const line of lines) {
             const dataPrefix = 'data:'
             if (line.startsWith(dataPrefix)) {
                 // Extract content, preserving spaces (unlike EventSource which strips first space)
                 let content = line.substring(dataPrefix.length)
                 // Unescape literal \n sequences to real newlines
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
  <div class="chat-container">
    <div class="chat-header">
      <h2>Chat Room (ID: {{ chatId }})</h2>
    </div>
    
    <div class="messages" ref="chatContainer">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        class="message-wrapper"
        :class="msg.role"
      >
        <div class="avatar">
          {{ msg.role === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="message-content markdown-body" v-html="msg.renderedContent"></div>
      </div>
      <div v-if="isLoading && !abortController" class="loading">Connecting...</div>
    </div>

    <div class="input-area">
      <input 
        v-model="inputMessage" 
        @keyup.enter="sendMessage"
        type="text" 
        placeholder="Ask about GTA V..."
        :disabled="isLoading"
      />
      <button @click="sendMessage" :disabled="isLoading || !inputMessage.trim()">
        Send
      </button>
    </div>
  </div>
</template>

<style scoped>
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

.chat-header {
  padding: 1rem;
  border-bottom: 1px solid #eee;
  text-align: center;
  background: #f9f9f9;
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
  background-color: var(--chat-bg);
  border-top-left-radius: 2px;
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
  border-color: var(--gta-green);
}

button {
  padding: 0 20px;
  background-color: var(--gta-green);
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
  background-color: #247045;
}
</style>
