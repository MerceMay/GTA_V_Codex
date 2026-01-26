# AI Agent: GTA V Wiki & Task Assistant

A sophisticated AI-powered system designed to provide expert knowledge on Grand Theft Auto V while doubling as a general-purpose ReAct (Reasoning and Acting) agent capable of performing real-world tasks.

## 🚀 Overview

This project implements a multi-modal AI assistant with two primary operating modes:
1.  **Chat Mode (RAG):** Utilizes Retrieval-Augmented Generation (RAG) to provide precise answers about GTA V missions, heists, characters, and collectibles using a curated local knowledge base.
2.  **Agent Mode (ReAct):** A highly capable autonomous agent that uses the ReAct paradigm to break down complex tasks, select appropriate tools, and execute them to reach a goal.

## 🏗️ Architecture

The system is built with a modern full-stack architecture:

-   **Backend:** Spring Boot 3.x using **Spring AI**.
    -   **AI Orchestration:** ReAct loop implementation with support for parallel tool calling.
    -   **RAG Engine:** Document processing, keyword enrichment, and vector storage integration (Milvus/PostgreSQL).
    -   **Streaming:** Server-Sent Events (SSE) for real-time response generation.
-   **Frontend:** Vue.js 3 with Vite.
    -   **Real-time UI:** Custom SSE stream parser to handle complex markdown and streaming updates.
    -   **Markdown Rendering:** Rich text support with syntax highlighting and LaTeX support.
-   **Infrastructure:** Docker Compose for easy deployment of vector databases (Milvus/PostgreSQL), Object Storage (MinIO), and other services.

## ✨ Key Features

### 🔍 Advanced RAG Capabilities
-   **Contextual Query Augmentation:** Rewrites user queries for better vector search results.
-   **Multi-Query Expansion:** Generates multiple search perspectives to ensure high recall.
-   **Keyword Enrichment:** Automatically tags documents with relevant keywords during ingestion.
-   **Custom Text Splitting:** Optimized chunking strategies for gaming guides and walkthroughs.

### 🛠️ Agentic Tools
The agent has access to a variety of tools:
-   **Web Search:** Search the internet for real-time information.
-   **Web Crawler:** Extract and read content from specific URLs.
-   **File Operations:** Create, read, and manage local files and workspace.
-   **Terminal Operation:** Execute safe shell commands.
-   **PDF Generation:** Create professional reports from gathered information.
-   **Resource Download:** Download external assets or files.

## 🛠️ Getting Started

### Prerequisites
-   Java 17 or higher
-   Node.js 18+ & npm
-   Docker & Docker Compose
-   OpenAI API Key (or other supported LLM provider)

### Setup Infrastructure
```bash
docker-compose up -d
```

### Backend Configuration
Update `src/main/resources/application.yml` with your API keys:
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

### Run the Application

**Backend:**
```bash
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd front-end
npm install
npm run dev
```

## 📖 Usage

-   **GTA V Knowledge:** Ask questions like "What is the best approach for the Big Score heist?" or "Where can I find all 50 letter scraps?".
-   **Task Execution:** In Agent Mode, try "Find 3 recent articles about AI news, summarize them, and save the result as a PDF."

## 📝 License
This project is for educational and research purposes. All GTA V related content belongs to Rockstar Games.
