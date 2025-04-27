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
            *   `AuthRepositoryImpl`: Implementação usando `supabase-kt-gotrue`.
            *   `InteractionRepositoryImpl`: Implementação usando `supabase-kt-postgrest`.
            *   **Supabase:** Para autenticação, banco de dados (metadados, perfis, histórico), storage (arquivos brutos).
            *   **Backend de IA:** Para todas as operações de IA (STT, TTS, RAG, LLM).
            *   **Cache Local (Room):** Para dados offline e buffer.
            *   Tecnologias: Kotlin (Coroutines, Flow), Supabase Kotlin Client, Retrofit/OkHttp (para Backend IA), Room.

*   **Backend de IA (Serviço Dedicado):**
    *   **Propósito:** Orquestra e executa todo o pipeline de processamento de IA descrito no `Prompt-AI.txt`. Responsável por STT, diarização/verificação, geração de embeddings, RAG, chamadas LLM, controle ético e TTS.
    *   **Tecnologia (Potencial):** Python, **Whisper.cpp/Vosk** (ASR), **pyannote.audio** (Diarização), **SentenceTransformers** (Embeddings), **Haystack/LangChain** (RAG), **LLMs** (via API ou hospedados), **Coqui/Mozilla TTS**, **pgvector** (via Supabase), **FAISS/Milvus** (alternativa para vetores).
    *   **API:** Expõe uma API (e.g., REST, gRPC) para o App Android consumir os serviços de IA.

*   **Supabase (BaaS):**
    *   **Propósito:** Fornece serviços de backend essenciais: autenticação, banco de dados relacional (PostgreSQL), armazenamento de objetos e funções serverless leves.
    *   **Componentes Usados:** Auth (JWT), PostgreSQL DB (com **RLS** e extensão **pgvector** para busca vetorial usada pelo Backend de IA), Storage, Edge Functions (TypeScript/Deno).
    *   **Edge Functions:** Usadas para lógica serverless *leve* que interage diretamente com Supabase (e.g., gatilhos de DB, validações simples, endpoints seguros para operações diretas no Supabase) ou como *gateways* seguros para invocar o Backend de IA. **Não devem conter lógica de IA pesada.**

## 2.2 Linguagens e Ferramentas Principais

*   **Kotlin:** Linguagem primária para o App Android. Uso extensivo de Coroutines e Flow.
*   **Jetpack Compose:** Toolkit de UI declarativo para Android.
*   **Hilt:** Injeção de Dependência no Android.
*   **Python (Potencial):** Linguagem primária para o Backend de IA dedicado.
*   **Supabase:** Backend as a Service (BaaS) central.
    *   Auth, PostgreSQL (com **pgvector**), RLS, Realtime Subscriptions, Storage, Edge Functions (TypeScript/Deno).
*   **Room:** Persistência local (cache offline) no Android.
*   **Retrofit & OkHttp:** Cliente HTTP no Android para comunicação com o Backend de IA e Edge Functions.
*   **~~Android SpeechRecognizer:~~** **Substituído** pela API de STT do Backend de IA para a funcionalidade principal.
*   **~~Android TextToSpeech:~~** **Substituído** pela API de TTS do Backend de IA para a voz principal do assistente. Pode ser usado para feedback secundário/local se necessário.
*   **Tecnologias de IA (Backend):** Whisper/Vosk, pyannote, SentenceTransformers, Haystack/LangChain, Coqui/Mozilla TTS, etc. (conforme `Prompt-AI.txt`).
*   **Kotlinx DateTime & Serialization:** Para manipulação de data/hora e JSON no Android. 