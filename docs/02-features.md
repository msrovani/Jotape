# 🛠 Funcionalidades Detalhadas (com Foco em Supabase e Backend IA)

Esta seção descreve o fluxo de dados e as interações entre o app Android, o Backend de IA e o Supabase para as funcionalidades principais.

## 3.0 Autenticação (Login, Cadastro, Logout)

O fluxo de autenticação permite que os usuários acessem o aplicativo de forma segura.

1.  **Interface (App Android):**
    *   `LoginScreen`: Permite ao usuário entrar com email/senha ou usando provedores OAuth (Google Sign-In).
    *   `SignUpScreen`: Permite ao usuário criar uma nova conta com email/senha.
    *   Feedback de erro/sucesso é exibido nessas telas.
    *   Navegação entre Login/Cadastro e para a tela principal (`ConversationScreen`) após sucesso.

2.  **Gerenciamento de Estado (App Android - Camada de Apresentação):
    *   `AuthViewModel` (Idealmente): Orquestra o fluxo de autenticação, mantém o estado da UI (isLoading, errorMessage, isUserLoggedIn, isSignUpSuccessful) e chama o repositório.
    *   *Nota:* Atualmente, essa lógica está temporariamente no `ConversationViewModel`, mas deve ser refatorada para um `AuthViewModel` dedicado para seguir a Clean Architecture.

3.  **Lógica de Dados (App Android - Camada de Dados):
    *   `AuthRepository`: Abstrai a fonte de dados de autenticação. Implementa funções como `signInWithEmail`, `signUpWithEmail`, `signInWithGoogle`, `signOut`, `getCurrentUserSession`.

4.  **Interação com Backend (App Android - Camada de Dados):
    *   `Supabase Kotlin Client`:
        *   `Auth`: Usado diretamente pelo `AuthRepository` para chamadas de `signInWith(Email)`, `signUpWith(Email)`, `signOut()`, `sessionStatus` (para observar mudanças).
        *   `ComposeAuth`: Usado pelo `AuthViewModel`/`AuthRepository` (ou UI via `rememberSignInLauncher`) para simplificar o fluxo de `signInWith(Google)`.

5.  **Processo de Cadastro (Supabase):
    *   Ao chamar `signUpWithEmail`, o Supabase cria o registro do usuário.
    *   **Confirmação de Email:** Por padrão (e recomendado), o Supabase envia um email de confirmação. O usuário só consegue fazer login *após* clicar no link de confirmação. A UI deve informar o usuário sobre isso após o cadastro.

6.  **Segurança (Supabase):
    *   **JWT:** Após login bem-sucedido, o Supabase Client armazena um JSON Web Token (JWT) que é enviado em todas as requisições subsequentes.
    *   **RLS:** As Row-Level Security policies nas tabelas do Supabase DB (`interactions`, `user_profiles`, etc.) usam `auth.uid()` (extraído do JWT) para garantir que os usuários só acessem seus próprios dados (ver `05-security-privacy.md`).

## 3.1 Reconhecimento de Voz do Dono (Verificação)

O objetivo é verificar se a voz do usuário corresponde a um modelo previamente treinado e armazenado de forma segura, **utilizando o Backend de IA para o processamento pesado**.

1.  **`RecordVoiceSample()` (App Android):**
    *   Grava uma amostra de áudio usando `MediaRecorder` ou similar.
    *   *Segurança:* Criptografar localmente antes do upload.
    *   Faz upload do arquivo de áudio para o **Supabase Storage** (bucket seguro `private/voice_samples/{user_id}/`).

2.  **`TriggerVoiceProcessing()` (App Android):**
    *   Chama uma **Supabase Edge Function** (e.g., `trigger-voice-processing`) via Retrofit/OkHttp, atuando como um *gateway seguro*.
    *   Passa a referência (path) do arquivo de áudio no Storage e a intenção (`train` ou `verify`).

3.  **Edge Function `trigger-voice-processing` (Supabase - TypeScript/Deno):**
    *   **Autenticação:** Valida o JWT do usuário para obter o `user_id` seguro.
    *   **Invocação do Backend IA:** Chama um endpoint específico no **Backend de IA** (e.g., `/process-voice`), passando o `user_id`, `audioStoragePath` e `intent`.
    *   **Relay da Resposta:** Aguarda a resposta do Backend IA e a retorna para o App Android.

4.  **Processamento no Backend de IA (Python):**
    *   **Recebe Requisição:** Obtém `user_id`, `audioStoragePath`, `intent` da Edge Function.
    *   **Busca Áudio:** Usa credenciais apropriadas (ou a identidade passada pela Edge Function) para baixar o áudio do **Supabase Storage**.
    *   **Busca Modelo Existente (se `verify` ou retreinando):** Consulta a tabela `voice_models` no **Supabase DB** (via API ou conexão direta) para obter o `model_storage_path` do modelo atual do usuário. Baixa o arquivo do modelo do **Supabase Storage**.
    *   **Processamento de Voz:** Usa bibliotecas Python (e.g., para extração de MFCCs, treinamento de GMM, ou outro método de Speaker Verification) para:
        *   **Treinar:** Gerar um novo modelo a partir da amostra de áudio. Salva o arquivo do modelo no **Supabase Storage** (e.g., `private/voice_models/{user_id}/model_vX.bin`). Atualiza/Insere metadados (incluindo o novo `model_storage_path`) na tabela `voice_models` no **Supabase DB**.
        *   **Verificar:** Comparar os MFCCs da amostra atual com o modelo existente carregado.
    *   **Limpeza:** Pode deletar o arquivo de *áudio bruto* do Storage após o processamento.
    *   **Retorna Resultado:** Envia JSON com o resultado (`success`, `message`, `isVerified` ou metadados do modelo treinado) de volta para a Edge Function.

5.  **`HandleVerificationResult()` (App Android):**
    *   Recebe a resposta retransmitida pela Edge Function.
    *   Atualiza o estado da UI (e.g., mostra mensagem de sucesso/falha).
    *   Usa **TextToSpeech (TTS) nativo do Android** para feedback audível *secundário* (pois a voz principal virá do Backend IA).

## 3.2 Reconhecimento de Fala (Speech-to-Text - STT)

Converte a fala do usuário em texto usando o **Backend de IA**.

1.  **`StartListening()` (App Android):**
    *   Utiliza `MediaRecorder` ou APIs de áudio de baixo nível para capturar áudio.
    *   *Opcional:* Implementar detecção de atividade de voz (VAD) local (e.g., `webrtcvad`) e chunking (enviar pedaços de áudio) para streaming e menor latência, como sugerido em `Prompt-AI.txt`.
    *   Gerencia permissões de áudio e atualiza a UI (`isListening`).

2.  **`SendAudioToBackend()` (App Android):**
    *   Envia os chunks de áudio (ou o arquivo completo) para um endpoint específico da **API do Backend de IA** (e.g., `/stt` via WebSocket para streaming ou POST para arquivos completos).
    *   Inclui metadados necessários (formato do áudio, idioma, `user_id`).

3.  **Processamento STT no Backend de IA (Python):**
    *   Recebe o áudio do App Android.
    *   Utiliza um modelo ASR (e.g., **Whisper.cpp**, **Vosk**) para transcrever o áudio em texto.
    *   Pode realizar pós-processamento (pontuação, formatação).
    *   Retorna a transcrição (JSON com `{text, timestamps, confidence}`, como em `Prompt-AI.txt`) para o App Android.

4.  **`HandleTranscriptionResult()` (App Android):**
    *   Recebe o texto transcrito do Backend IA.
    *   Atualiza a UI para exibir o texto reconhecido.
    *   Prepara para enviar o texto para o processamento de IA (próxima seção).

## 3.3 Processamento de IA e Geração de Resposta (RAG + LLM)

Envia o texto do usuário para o **Backend de IA** para obter uma resposta contextualizada e inteligente.

1.  **`SendTextToBackend()` (App Android):**
    *   Envia o texto transcrito (obtido do STT na etapa anterior) para um endpoint da **API do Backend de IA** (e.g., `/generate-response`).
    *   Inclui `user_id` e, opcionalmente, histórico recente da conversa (para contexto) ou outros metadados relevantes.

2.  **Pipeline RAG e LLM no Backend de IA (Python):**
    *   **Recebe Requisição:** Obtém texto do usuário, `user_id`, histórico.
    *   **1. Embedding da Entrada:** Gera embedding do texto do usuário (e.g., usando **SentenceTransformers**).
    *   **2. Busca Vetorial (Memória):** Consulta o índice vetorial (e.g., usando **`pgvector`** no **Supabase DB**) para encontrar interações passadas semanticamente similares (`candidates`) do `user_id`.
    *   **3. Construção do Prompt:** Cria um prompt otimizado para o LLM, incluindo:
        *   Instruções de sistema (persona, tarefa).
        *   Histórico da conversa atual.
        *   Informações recuperadas da busca vetorial (`candidates`).
        *   Potencialmente, informações do perfil do usuário ou `entitlements` (tier Free/Premium).
    *   **4. Chamada ao LLM:** Envia o prompt para o modelo de linguagem configurado (e.g., API do Google Gemini, OpenAI, etc.). Pode usar streaming se suportado.
    *   **5. Controle Ético:** Valida a resposta bruta do LLM usando um modelo de moderação ou regras internas (`EthicalControlService`) para filtrar conteúdo inadequado.
    *   **6. Personalização:** Ajusta a resposta final (estilo, formatação) com base nas configurações/persona do usuário.
    *   **Retorna Resposta:** Envia a resposta textual final para o App Android.

3.  **`HandleBackendResponse()` (App Android):**
    *   Recebe a resposta textual do Backend IA.
    *   Exibe a resposta na `TranscriptView`.
    *   Inicia a síntese de voz (próxima seção).

## 3.4 Síntese de Voz (Text-to-Speech - TTS)

Converte a resposta textual do assistente em áudio usando o **Backend de IA**.

1.  **`RequestTTSFromBackend()` (App Android):**
    *   Envia o texto da resposta final (recebido na etapa anterior) para um endpoint da **API do Backend de IA** (e.g., `/tts`).
    *   Inclui `user_id` e preferências de voz (se aplicável, baseado em `entitlements` ou IAPs).

2.  **Geração de TTS no Backend de IA (Python):**
    *   Recebe o texto e as preferências de voz.
    *   Consulta o `EntitlementManager` para determinar a voz/modelo TTS apropriado (e.g., voz padrão para Free, vozes premium/customizadas para Premium/IAP).
    *   Usa um mecanismo TTS (e.g., **Coqui TTS**, **Mozilla TTS**) para converter o texto em áudio.
    *   Aplica formatação SSML, se necessário, para melhor prosódia.
    *   Envia o áudio gerado (e.g., stream WAV/OGG) de volta para o App Android (via WebSocket ou HTTP streaming).

3.  **`PlaySynthesizedAudio()` (App Android):**
    *   Recebe o stream de áudio do Backend IA.
    *   Utiliza `MediaPlayer` ou `ExoPlayer` para reproduzir o áudio recebido.
    *   Atualiza a UI para indicar que o assistente está "falando".

---
*Nota: As seções sobre UI (03) e Feedback/Métricas (04) podem precisar de pequenos ajustes para refletir que STT/TTS principais vêm do backend, mas a estrutura geral permanece válida.*