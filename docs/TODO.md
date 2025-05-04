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

*   [x] **(A-C) Build/Dependências (Android):**
    *   [x] Verificar/atualizar dependências Gradle (Supabase Clients, Hilt, Room, WorkManager, Google AI SDK, Serialization, DateTime, etc.).
    *   [x] Configurar plugin `kotlinx.serialization` (se usado).
    *   [x] Corrigir problemas de build relacionados a dependências (`kotlin-stdlib`, `material-icons-extended`) e `BuildConfig`.
*   [ ] **(B-C) Build/Dependências (Backend IA):** (Se houver Backend dedicado)
*   [x] **(S-C) Infraestrutura Supabase:**
    *   [x] Validar config Supabase Client no Android (`di/SupabaseModule.kt`).
    *   [x] Configurar projeto Supabase (Auth Providers Email/Google).
    *   [x] Definir Schemas (`users`, `interactions`) e RLS via SQL Editor/Migrations.
*   [ ] **(B-C) Infraestrutura Backend IA:** (Se houver Backend dedicado)
*   [ ] **(I) Definição de API Backend IA:** (Se houver Backend dedicado)
*   [x] **(A-C) Configuração Android:**
    *   [x] Validar config Room (`di/DatabaseModule.kt`).
    *   [x] Implementar `Provides`/@Binds para DAOs e Repositórios Hilt.
    *   [x] Definir `DomainResult` wrapper no Android.
    *   [x] Configurar `GeminiModule` para ler chave API do `BuildConfig`.
    *   [x] Configurar `WorkManager` para usar Hilt (`JotapeApplication` simplificada).
    *   [ ] **(Novo)** Implementar `PromptManager` para centralizar prompts.
*   [ ] **(A-R) Refatoração Android:**
    *   [ ] Mover lógica de IA do `InteractionRepositoryImpl` para UseCases dedicados.
    *   [ ] Mover prompts do `PromptManager` para `strings.xml` para i18n.
    *   [ ] **(Novo)** Investigar e corrigir erros persistentes de Linter no IDE.
    *   [ ] **(Novo)** Investigar e corrigir fluxo de Google Sign-In.
    *   [ ] **(Novo)** Refatorar `InteractionRepositoryImpl` para usar UseCases para chamadas Gemini/DB (desacoplamento).
*   [ ] **(A-R)** Reconciliar/Refatorar interface `VoiceRepository` para refletir chamadas ao Backend IA/Edge Function.
*   [x] **(A-R)** Organizar pacotes conforme Clean Architecture (movido DatabaseModule).

---

## 🔐 (S/A) Autenticação e Perfis (`auth`, `user_profiles`) - Fase 2

*   [x] **(S)** Configurar projeto Supabase Cloud (Auth Providers Email/Google).
*   [ ] **(S)** Definir schema `user_profiles` (se necessário), RLS.
*   [x] **(A)** Implementar `AuthRepositoryImpl` (Android): usa `Supabase Client Auth`.
*   [x] **(A)** Implementar `AuthViewModel` (Android).
*   [x] **(A)** Implementar UI `LoginScreen` / `SignUpScreen`.
*   [x] **(A)** Configurar Navegação.
*   [x] **(A)** Implementar função de Logout.
*   [x] **(C)** Configurar `local.properties` com credenciais.
*   [x] **(S/C)** Configurar URI de redirecionamento Google.
*   [ ] **(A)** Implementar recuperação de senha.
*   [ ] **(A)** Refinar tratamento de erros Auth.
*   [ ] **(A)** Implementar gerenciamento de perfil do usuário (se `user_profiles` for usado).
*   [ ] **(A) (Pendente)** Investigar e corrigir fluxo de Google Sign-In.

*   **Status:** Autenticação Email/Senha c/ Confirmação FUNCIONAL. Logout FUNCIONAL. Google Sign-In PENDENTE DE CORREÇÃO. Tarefas pendentes: recuperação de senha, perfil, tratamento de erro refinado.

---

## 💬 (S/A) Conversa, Interações e Sincronização (`interactions`)

*   [x] **(S)** Definir schema `interactions` (com `user_id`). Habilitar RLS.
*   [ ] **(A)** ~~Implementar `InteractionDao` (Room).~~ (Removido, persistência primária agora é Supabase).
*   [x] **(A)** Implementar `InteractionRepositoryImpl` (Android):
    *   [x] Usa `SupabaseClient.functions` para chamar Edge Function (`extract-chat-and-points`).
    *   [x] Usa `SupabaseClient.realtime` para receber atualizações do DB.
    *   [x] Usa `SupabaseClient.postgrest` para buscar histórico inicial e limpar.
    *   [x] Usa `WorkManager` (`SyncInteractionWorker`) para sincronização em background como fallback.
*   [x] **(A)** Implementar `SyncInteractionWorker` (HiltWorker).
*   [x] **(A)** Adaptar `ConversationViewModel` (Android):
    *   [x] Chamar `InteractionRepository` (`sendMessage`, `getAllInteractions`).
    *   [x] Expor estado de UI (mensagens, loading, erro) via `StateFlow`.
    *   [x] Implementar `clearChatHistory()`.
*   [x] **(A)** Implementar `ConversationScreen` (Compose) para exibir mensagens, input e loading.

*   **Status:** Chat funcional com interação via Edge Function (`extract-chat-and-points`) e Gemini. Atualizações da UI via Supabase Realtime. Sincronização robusta em background via WorkManager. Persistência primária no Supabase. Próximos passos: implementar `PromptManager`, refatorar repositório com UseCases, otimizar UI/UX.

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