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
                    *   Usa `SupabaseClient.functions` para invocar a Edge Function `extract-chat-and-points` ao enviar uma nova mensagem de usuário. Esta função orquestra a chamada ao Gemini e salva a interação no banco de dados Supabase.
                    *   Usa `SupabaseClient.realtime` (`postgresChangeFlow`) para *receber* atualizações (novas interações) do banco de dados Supabase em tempo real.
                    *   Usa `SupabaseClient.postgrest` para buscar o histórico inicial e para limpar o histórico.
                    *   **Utiliza `WorkManager` (`SyncInteractionWorker`)** como um mecanismo de *fallback* para sincronizar interações com o Supabase caso a escrita inicial (potencialmente feita pela Edge Function ou diretamente pelo app) falhe ou para sincronizações periódicas. (Nota: A estratégia exata de escrita ainda está sendo refinada, mas o Worker está presente para robustez).
            *   **Fontes de Dados:**
                *   **Supabase (`SupabaseClient`, `Postgrest`, `Auth`, `Realtime`, `Functions`):** Backend para autenticação, armazenamento persistente das interações, lógica serverless e atualizações em tempo real.
                *   **Supabase Edge Function (`extract-chat-and-points`):** Lógica server-side (Deno/TypeScript) que recebe a mensagem do usuário e histórico, chama a API do Gemini, e **potencialmente** salva as interações (usuário e assistente) no banco de dados Supabase. Retorna a resposta do Gemini para o app.
                *   **Google AI API (Gemini):** Chamado **pela Edge Function** para gerar as respostas do assistente.
            *   **API Services (`api` - Retrofit):**
                *   **Removido/Não utilizado atualmente:** A comunicação com a Edge Function é feita diretamente via `SupabaseClient.functions`.
            *   **DI (`di` - Hilt):
                *   `SupabaseModule`: Fornece o `SupabaseClient` e seus componentes (`Auth`, `Postgrest`, `Realtime`, `Functions`).
                *   **Removido:** `NetworkModule` (Retrofit/OkHttp não são mais usados para a Edge Function).
                *   `RepositoryModule`: Faz o bind das interfaces de repositório (`AuthRepository`, `InteractionRepository`) às suas implementações (`...Impl`).
                *   `WorkerModule` (ou similar): Configura o `WorkManager` e fornece a `HiltWorkerFactory`.
            *   **Tecnologias:** Kotlin (Coroutines, Flow), Supabase Kotlin Client (Auth, Postgrest, Realtime, Functions), Kotlinx Serialization, Hilt, **WorkManager**.

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
*   **Google AI Generative SDK:** Biblioteca cliente para interagir com a API Gemini (usada *pela Edge Function*).
*   **Kotlinx Serialization:** Para serialização/desserialização JSON entre App e Edge Function.
*   **Supabase Kotlin Client:** Biblioteca completa para interagir com Auth, Postgrest, Realtime e Functions.
*   **WorkManager:** Para execução de tarefas em background (sincronização de dados).
*   **Kotlinx DateTime & Serialization:** Para manipulação de data/hora e JSON no Android. 