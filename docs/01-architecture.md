# 🧱 Arquitetura e Stack Tecnológico

Adotamos a **Clean Architecture** para separar as preocupações e promover a testabilidade e manutenibilidade, combinada com um **Backend de IA dedicado** para processamento de linguagem e voz.

## 2.1 Componentes Principais

*   **App Android Nativo (Kotlin/Compose):**
    *   Interface do usuário, interação direta com o usuário, captura de áudio e gerenciamento de estado local. Segue a Clean Architecture internamente.
    *   **Camadas:**
        *   **Apresentação (`presentation`):** UI (Compose), ViewModels (MVVM), gerenciamento de estado da UI (Flow). Tecnologias: Jetpack Compose, ViewModel, Coroutines/Flow, Hilt.
            *   **Componentes de UI (Exemplos):**
                *   `auth/LoginScreen.kt`: Tela para entrada de email/senha.
                *   `auth/SignUpScreen.kt`: Tela para cadastro com email/senha e confirmação.
                *   `conversation/ConversationScreen.kt`: Tela principal exibindo a lista de mensagens e a barra de entrada.
            *   **ViewModels (Exemplos):**
                *   `auth/AuthViewModel.kt`: Gerencia o estado da UI para login/cadastro (campos, loading, erros, status de sucesso, estado de login) e interage com `AuthRepository`.
                *   `conversation/ConversationViewModel.kt`: Gerencia o estado da UI da conversa (lista de interações, texto de entrada, loading, erros) e interage com `InteractionRepository`.
        *   **Domínio (`domain`):** Lógica de negócios pura do *lado do cliente*, casos de uso da UI, entidades de negócio do cliente, interfaces de repositório *do cliente*. Tecnologia: Kotlin puro.
            *   `AuthRepository`: Interface para operações de autenticação.
            *   `InteractionRepository`: Interface para operações com as interações da conversa.
            *   `DomainResult`: Wrapper genérico para resultados de operações.
            *   `Interaction`, `User` (etc.): Modelos de domínio.
        *   **Dados (`data`):** Implementa interfaces de repositório *do cliente*. Lida com a obtenção/armazenamento de dados de **múltiplas fontes**:
            *   **Repositórios:**
                *   `AuthRepositoryImpl`: Implementação usando `supabase-kt-gotrue`.
                *   `InteractionRepositoryImpl`: Implementação que:
                    *   Usa `JotapeApiService` (Retrofit) para chamar a Edge Function `process-user-command` para enviar mensagens e receber respostas.
                    *   Usa `SupabaseClient.realtime` (`postgresChangeFlow`) para obter e observar o histórico de interações em tempo real.
                    *   Usa `SupabaseClient.postgrest` para limpar o histórico.
                    *   **Não usa mais Room ou WorkManager.**
            *   **Fontes de Dados:**
                *   **Supabase (`SupabaseClient`, `Postgrest`, `Auth`, `Realtime`):** Backend para autenticação, armazenamento persistente das interações, e atualizações em tempo real.
                *   **Supabase Edge Function (`process-user-command`):** Lógica server-side (Deno/TypeScript) que recebe o prompt, salva no DB, busca histórico, chama Gemini, salva a resposta no DB e retorna ao cliente.
                *   **Google AI API (Gemini):** Chamado **pela Edge Function** para gerar as respostas do assistente.
            *   **API Services (`api` - Retrofit):**
                *   `JotapeApiService`: Define o endpoint para a Edge Function `process-user-command`.
            *   **DI (`di` - Hilt):
                *   `SupabaseModule`: Fornece o `SupabaseClient` e seus componentes (`Auth`, `Postgrest`, `Realtime`).
                *   `NetworkModule`: Fornece `OkHttpClient`, `Retrofit` e `JotapeApiService`. Configura `AuthInterceptor` para adicionar headers Supabase.
                *   `RepositoryModule`: Faz o bind das interfaces de repositório (`AuthRepository`, `InteractionRepository`) às suas implementações (`...Impl`).
                *   `PromptManager`: **Mantido** (pode ser usado para prompts do sistema enviados à Edge Function ou para lógica futura no cliente).
            *   **Tecnologias:** Kotlin (Coroutines, Flow), Supabase Kotlin Client (Auth, Postgrest, Realtime), Retrofit, OkHttp, Kotlinx Serialization, Hilt.

*   **Backend de IA (Serviço Dedicado):**
    *   **Propósito:** Orquestra e executa todo o pipeline de processamento de IA descrito no `Prompt-AI.txt`. Responsável por STT, diarização/verificação, geração de embeddings, RAG, chamadas LLM, controle ético e TTS.
    *   **Tecnologia (Potencial):** Python, **Whisper.cpp/Vosk** (ASR), **pyannote.audio** (Diarização), **SentenceTransformers** (Embeddings), **Haystack/LangChain** (RAG), **LLMs** (via API ou hospedados), **Coqui/Mozilla TTS**, **pgvector** (via Supabase), **FAISS/Milvus** (alternativa para vetores).
    *   **API:** Expõe uma API (e.g., REST, gRPC) para o App Android consumir os serviços de IA.

*   **Supabase (BaaS):**
    *   **Propósito:** Fornece serviços de backend essenciais: autenticação (JWT), banco de dados relacional (PostgreSQL para histórico), **Edge Functions (lógica serverless)**, **Realtime (WebSockets para atualizações)**.
    *   **Componentes Usados:** Auth, PostgreSQL DB, Edge Functions, Realtime.

*   **Google AI Platform (Gemini):**
    *   **Propósito:** Fornece o modelo de linguagem grande (LLM) para gerar as respostas do assistente.
    *   **Componentes Usados:** API Gemini (atualmente configurada para `gemini-2.0-flash`).

## 2.2 Linguagens e Ferramentas Principais

*   **Kotlin:** Linguagem primária para o App Android. Uso extensivo de Coroutines e Flow.
*   **Jetpack Compose:** Toolkit de UI declarativo para Android.
*   **Hilt:** Injeção de Dependência no Android.
*   **Python (Potencial):** Linguagem primária para o Backend de IA dedicado.
*   **Supabase:** Backend as a Service (BaaS) para Auth e DB.
    *   Auth, PostgreSQL.
*   **Retrofit & OkHttp:** Cliente HTTP no Android para comunicação com a Edge Function.
*   **~~Android SpeechRecognizer:~~** **Substituído** pela API de STT do Backend de IA para a funcionalidade principal.
*   **~~Android TextToSpeech:~~** **Substituído** pela API de TTS do Backend de IA para a voz principal do assistente. Pode ser usado para feedback secundário/local se necessário.
*   **Tecnologias de IA (Backend):** Whisper/Vosk, pyannote, SentenceTransformers, Haystack/LangChain, Coqui/Mozilla TTS, etc. (conforme `Prompt-AI.txt`).
*   **Kotlinx DateTime & Serialization:** Para manipulação de data/hora e JSON no Android.
*   **Google AI Generative SDK:** Biblioteca cliente para interagir com a API Gemini.
*   **Kotlinx Serialization:** Para serialização/desserialização JSON entre App e Edge Function.
*   **Supabase Kotlin Client:** Biblioteca completa para interagir com Auth, Postgrest e Realtime.
*   **~~WorkManager:~~** **Removido.**
*   **Kotlinx DateTime & Serialization:** Para manipulação de data/hora e JSON no Android. 