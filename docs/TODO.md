# ✅ Roadmap / Lista de Tarefas (TODO) - Arquitetura Revisada

Este documento detalha as tarefas pendentes para a implementação completa das funcionalidades planejadas, **considerando a arquitetura com App Android, Backend IA e Supabase**.

**Legenda:**
*   `[ ]`: Tarefa pendente.
*   `[x]`: Tarefa concluída.
*   `(A)`: Tarefa do App Android (Kotlin/Compose).
*   `(B)`: Tarefa do Backend de IA (Python).
*   `(S)`: Tarefa do Supabase (Configuração, DB, Storage, Edge Functions).
*   `(I)`: Tarefa de Integração (API entre componentes).
*   `(T)`: Tarefa relacionada a Testes.
*   `(C)`: Tarefa relacionada à Configuração Geral/Infraestrutura.
*   `(R)`: Tarefa de Refatoração.

---

## ⚙️ (C/S/B/I) Configuração, Base e Infraestrutura

*   [ ] **(A-C) Build/Dependências (Android):**
    *   [ ] Verificar/atualizar dependências Gradle (Supabase Clients, Retrofit, Hilt, Room, Serialization, DateTime, etc.).
    *   [ ] Configurar plugin `kotlinx.serialization`.
*   [ ] **(B-C) Build/Dependências (Backend IA):**
    *   [ ] Definir gerenciador de pacotes (pip/requirements.txt, poetry, etc.).
    *   [ ] Adicionar dependências Python (framework web como FastAPI/Flask, libs Supabase, libs de IA: transformers, whisper, sentence-transformers, TTS, etc.).
*   [ ] **(S-C) Infraestrutura Supabase:**
    *   [x] Validar config Supabase Client no Android (`di/SupabaseModule.kt` - **mover credenciais para local seguro**).
    *   [ ] Configurar projeto Supabase (Auth Providers, DB, Storage Buckets).
    *   [ ] Habilitar extensão `pgvector` no Supabase DB.
    *   [ ] Definir e aplicar Schemas/RLS/Políticas de Storage via Migrations (Supabase CLI).
*   [ ] **(B-C) Infraestrutura Backend IA:**
    *   [ ] Escolher e configurar plataforma de hospedagem (Cloud Run, Kubernetes, etc.).
    *   [ ] Configurar CI/CD para Backend IA (ver `06-devops-qa.md`).
*   [ ] **(I) Definição de API Backend IA:**
    *   [ ] Definir contratos (OpenAPI/Swagger?) para os endpoints: `/stt`, `/generate-response`, `/tts`, `/process-voice` (verificação), etc. Especificar formatos de requisição/resposta, autenticação (JWT vindo do App?).
*   [ ] **(A-C) Configuração Android:**
    *   [x] Validar config Retrofit (`di/NetworkModule.kt` - **mover URL base do Backend IA para local seguro**).
    *   [ ] Validar config Room (`di/DatabaseModule.kt`).
    *   [ ] Implementar `Provides` para DAOs e Repositórios Hilt.
    *   [ ] Definir `Result` wrapper no Android.
*   [ ] **(A-R) Refatoração Android:**
    *   [ ] Mover lógica Auth do `ConversationViewModel` para `AuthViewModel`.
    *   [x] Mover DTOs/Mappers para arquivos dedicados.
    *   [ ] **(A-R)** Reconciliar/Refatorar interface `VoiceRepository` para refletir chamadas ao Backend IA/Edge Function.
    *   [x] **(A-R)** Organizar pacotes conforme Clean Architecture (movido DatabaseModule).

---

## 🔐 (S/A) Autenticação e Perfis (`auth`, `user_profiles`) - Fase 2

*   [x] **(S)** Configurar projeto Supabase Cloud (Auth Providers Email/Google, Tabelas, RLS via SQL Editor).
*   [x] **(S)** Definir schema `user_profiles`, RLS. (Feito no script SQL)
*   [x] **(A)** Implementar `AuthRepositoryImpl` (Android): usa `Supabase Client Auth` (inclui tratamento básico de erro de login).
*   [x] **(A)** Implementar `AuthViewModel` (Android): Gerencia `AuthUiState` (inclui estado `isSignUpSuccess`).
*   [x] **(A)** Implementar UI `LoginScreen` / `SignUpScreen` (Android Compose) (SignUp com diálogo de confirmação).
*   [x] **(A)** Configurar Navegação (`MainActivity`, `Routes.kt`) para fluxo básico Auth -> Chat e Logout -> Auth.
*   [x] **(A)** Implementar função de Logout no `AuthViewModel` e botão na `ConversationScreen`.
*   [x] **(C)** Configurar `local.properties` com credenciais Supabase/Google.
*   [x] **(C)** Corrigir problemas de build relacionados a dependências (`kotlin-stdlib`, `material-icons-extended`) e `BuildConfig`.
*   [x] **(S/C)** Configurar URI de redirecionamento correto para Google Sign-In no Google Cloud Console.

*   **Status:** Fluxo básico de Autenticação (Cadastro com confirmação, Login Email/Senha, Login Google, Logout) implementado e funcional. Próximos passos: refinar tratamento de erros, implementar recuperação de senha, gerenciar perfil do usuário.

---

## 🗣️ (S/B/A/I) Conversa, Interações e Memória (`interactions`, RAG)

*   [x] **(S)** Definir schema `interactions` (com `user_id`). Habilitar RLS. (Feito no script SQL)
*   [ ] **(S)** Criar índice vetorial (e.g., IVFFlat, HNSW) na coluna `embedding`. (Adiado)
*   [ ] **(B)** Implementar Pipeline RAG (Backend IA - Python). (Adiado - Fase 3+)
*   [x] **(A)** Implementar `InteractionRepositoryImpl` (Android):
    *   [x] Usar Supabase Postgrest para `getAllInteractions`, `addInteraction`, `clearHistory`.
    *   [ ] Remover dependência/uso do Room (ou manter apenas para cache). (Pendente)
*   [x] **(A)** Adaptar `ConversationViewModel` (Android):
    *   [x] Chamar `InteractionRepository` (agora suspend).
    *   [ ] Implementar paginação (se necessário). (Adiado)
    *   [x] Expor estado de carregamento/resposta.
    *   [x] Implementar `clearChatHistory()` (inicialmente não funcional, agora corrigido).
*   [ ] **(A)** Implementar paginação na `ConversationScreen` (Android Compose). (Adiado)

*   **Status:** Interação básica com Supabase via Repositório/ViewModel implementada. Limpar histórico funcional. Paginação e RAG pendentes.

---

## 🎙️ (S/B/A/I) Reconhecimento de Voz do Dono (Verificação)

*   [ ] **(S)** Definir schema `voice_models` (incluir `user_id`, `model_storage_path`, `version`). Habilitar RLS.
*   [ ] **(S)** Criar bucket `voice_samples` (uploads do App) e `voice_models` (modelos processados pelo Backend). Definir políticas de segurança restritivas.
*   [ ] **(S)** Implementar Edge Function `trigger-voice-processing` (TypeScript/Deno - Gateway Seguro):
    *   [ ] Validar JWT, obter `user_id`.
    *   [ ] Chamar endpoint `/process-voice` do Backend IA, passando `user_id`, `audioStoragePath`, `intent`.
    *   [ ] Retornar resposta do Backend IA.
*   [ ] **(B)** Implementar Endpoint `/process-voice` (Backend IA - Python):
    *   [ ] Receber dados da Edge Function.
    *   [ ] Baixar áudio de `voice_samples` (Supabase Storage).
    *   [ ] Se `verify` ou retreino, buscar/baixar modelo de `voice_models` (Supabase DB/Storage).
    *   [ ] **[CORE]** Implementar extração de features (MFCC) e lógica de verificação/treinamento (GMM, etc.).
    *   [ ] Salvar/Atualizar modelo em `voice_models` (Supabase Storage/DB).
    *   [ ] Limpar áudio bruto.
    *   [ ] Retornar resultado (`success`, `isVerified` ou metadata).
*   [ ] **(A)** Implementar `VoiceRepositoryImpl` (Android):
    *   [ ] `recordAndUploadSample()`: Grava áudio, faz upload para `voice_samples` (Supabase Storage).
    *   [ ] `triggerProcessing(audioPath, intent)`: Chama a Edge Function `trigger-voice-processing` via Retrofit.
    *   [ ] Tratar `Result`.
*   [ ] **(A)** Criar `VoiceSetupViewModel` (Android): Gerenciar estados (recording, uploading, processing, success, error).
*   [ ] **(A)** Criar UI (Android Compose) para fluxo de configuração/verificação.
*   [ ] **(A)** Usar TTS Nativo Android para feedback *secundário*.

---

## 🎤 (B/A/I) STT / TTS

*   [ ] **(B)** Implementar Endpoint `/stt` (Backend IA - Python):
    *   [ ] Receber áudio (arquivo POST ou stream WebSocket).
    *   [ ] Usar ASR (Whisper.cpp, Vosk).
    *   [ ] Retornar JSON `{text, timestamps, confidence}`.
*   [ ] **(B)** Implementar Endpoint `/tts` (Backend IA - Python):
    *   [ ] Receber texto, `user_id`, preferências de voz.
    *   [ ] Consultar `entitlements` (Supabase DB) para selecionar voz.
    *   [ ] Usar TTS (Coqui, Mozilla).
    *   [ ] Retornar stream de áudio (WAV/OGG).
*   [ ] **(A)** Implementar `AudioHandler` (Android):
    *   [ ] Capturar áudio (`MediaRecorder`).
    *   [ ] (Opcional) VAD local, chunking.
    *   [ ] Enviar áudio para endpoint `/stt` do Backend IA (via Retrofit/WebSocket).
    *   [ ] Receber transcrição.
*   [ ] **(A)** Implementar `AudioPlayer` (Android):
    *   [ ] Receber stream de áudio do endpoint `/tts` do Backend IA.
    *   [ ] Reproduzir usando `MediaPlayer`/`ExoPlayer`.
*   [ ] **(A)** Adaptar `ConversationViewModel` (Android):
    *   Coordenar `AudioHandler` e `