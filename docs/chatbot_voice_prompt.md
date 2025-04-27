```markdown
Você é um engenheiro de sistemas responsável por projetar e implementar um **chatbot de voz** com **aprendizado contínuo**, **memória contextual** e **feedback implícito e explícito** baseado em **avaliação contextual** (sem palavras-chave predefinidas). Utilize apenas bibliotecas e ferramentas **open source**. Documente cada componente técnico, fluxo de dados e requisitos de hardware de ponta a ponta.

### 1. Captura de Áudio (Dispositivo → Cliente)
- **Input**: áudio capturado via microfone do dispositivo Android.
- **Pré-processamento**:
  - Voice Activity Detection com **webrtcvad**.
  - Redução de ruído (e.g., **rnnoise**).
  - Chunking de 10 s com 50 % overlap.

### 2. Transcrição (Cliente → ASR)
- **Modelos ASR**:
  - **Default Offline:** `ggerganov/whisper.cpp` (ggml, MIT, CPU, multilingue).
  - **Alta Acurácia:** `openai/whisper-large-v2` (PyTorch/TF/JAX, 99 idiomas, requer GPU ou quantização para dispositivo móvel).
  - **Inglês Leve:** `facebook/wav2vec2-base-960h` (fine-tuned Librispeech 960 h, rápido em CPU/GPU).
- **Output**: JSON `{text, start_time, end_time, confidence}`.

### 3. Diarização e Tagging (ASR → Servidor)
- **Diarização:** `collinbarnwell/pyannote-speaker-diarization-3.1` (pure PyTorch, mono 16 kHz).
- **Metadados:** anexar `{user_id, speaker_id, channel, environment, timestamp}`.

### 4. Persistência de Dados (Servidor → DB)
- **Banco:** **Supabase** (Postgres + `pgvector`) com **RLS** e **JWT Auth**.
- **Tabelas:** `users`, `sessions`, `messages(id, session_id, speaker_id, text, timestamp, embedding, audio_ref)`.
- **Storage:** Supabase Storage para arquivos de áudio.

### 5. Embeddings e Indexação Vetorial
- **Texto:** `sentence-transformers/all-MiniLM-L6-v2` (384-dim) como padrão.
- **Paráfrases:** `sentence-transformers/paraphrase-MiniLM-L6-v2` para enriquecer similaridade semântica.
- **Multimodal (opcional):** **AudioCLIP** para vetores de áudio+texto.
- **Indexação:** **FAISS** local ou **Milvus/Weaviate** em cluster.

### 6. Pipelines de RAG e Geração de Resposta
- **Framework:** **Haystack** ou **LangChain**.
- **Integração:** uso de `HuggingFaceHub` ou `huggingface_hub` SDK para instanciar modelos do Hub.
- **Fluxo:**  
  1. `emb = embed(last_user_turn)`  
  2. `ctx = retrieve(emb, top_k)`  
  3. `prompt = build_prompt(ctx, session_history, entitlements)`  
  4. `response_raw = LLM.generate(prompt, stream=True)`

### 7. Síntese de Voz (Servidor → Cliente)
- **TTS Primário:** **Coqui TTS** ou **Mozilla TTS** com SSML para entonação.
- **TTS Regionais:** `firstpixel/F5-TTS-pt-br` (Português BR), `facebook/mms-tts-por` (MMS multilíngue).
- **Fallback Offline:** manter Coqui/Mozilla local em caso de falha.
- **Streaming:** WebSocket ou HTTP/2.

### 8. Feedback e Aprendizado Contínuo
- **Feedback Implícito:** latência, abortos, repetições.
- **Feedback Explícito:** classificação contextual via zero-shot e embeddings para `gratitude`, `praise`, `agreement`, `disagreement`, `clarification`, `question`, `apology`, `greeting`, `farewell`, `confusion`, `humor`, `emotion`, `suggestion`.
- **Loop de Treino:** agregar logs e feedback em JSONL → fine-tuning incremental via **LoRA/QLoRA**.

### 9. MemoryManager e Categorização de Memórias
- **Objetivo:** Gerir memórias de curto e longo prazo, categorizadas semânticamente.
- **Taxonomia:**  
  - **Trabalho:** últimas 5–10 turns ativos.  
  - **Curto Prazo:** detalhes do tópico corrente, resumidos periodicamente.  
  - **Longo Prazo:** fatos estáveis (preferências, perfil, projetos).
- **Categorização:** zero-shot (BART-MNLI), embeddings+clustering, dialogue-act models.
- **APIs:** `addMemory()`, `getMemories()`, `updateMemory()`, `deleteMemory()`.
- **Infraestrutura:** Key-Value Store + Índice Vetorial.
- **Privacidade:** Painel de memórias e chats temporários sem persistência.

### 10. Monitoramento e Segurança
- **Criptografia:** TLS 1.3 fim-a-fim.
- **Autenticação:** JWT via Supabase Auth.
- **Métricas:** Prometheus + Grafana para todas as camadas.

### 11. Requisitos de Hardware Mínimos
#### 11.1 App Android (minSDK 30)
- **SO:** Android 11 (API 30+) ou superior.
- **CPU:** ARMv8-A (NEON), ideal Snapdragon 7xx/8xx.
- **RAM:** ≥ 4 GB.
- **Armazenamento:** ≥ 64 GB UFS 2.1/3.1.
- **ML Acceleration:** Vulkan/NNAPI, opcional NPU/DSP para latência <1 s em Whisper TFLite.

#### 11.2 Servidor Backend
- **Persistência POC:** 4 vCPU, 16 GB RAM, 100 GB NVMe SSD.
- **Produção Escalada:** 8 vCPU, 32 GB RAM, 250 GB NVMe SSD, réplicas de leitura.
- **Inferência:** GPU NVIDIA T4 (16 GB GDDR6) em servidor Intel Xeon Gold 6154 (18 cores, 3 GHz), 32 GB RAM, 500 GB NVMe SSD, rede 1 Gbps.
```