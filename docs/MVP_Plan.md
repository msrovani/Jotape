# 🚀 Plano Faseado para MVP do Jotape

Este documento detalha o plano de desenvolvimento faseado para a Versão Mínima Viável (MVP) do Jotape, garantindo uma entrega incremental e focada.

## Fase 1: Chatbot de Texto Básico (Local)

**Objetivo:** Criar a interface de chat principal, permitindo ao usuário enviar mensagens de texto e ver respostas (simuladas), com o histórico salvo localmente no dispositivo. Sem necessidade de Backend IA ou Supabase nesta fase.

**Entregáveis (Android - Kotlin/Compose):**
*   **UI (`ConversationScreen`):**
    *   Campo de entrada de texto (`TextField`).
    *   Botão de envio (`Button`).
    *   Lista rolável para exibir a conversa (`LazyColumn` com `ChatItem` para diferenciar usuário/bot).
*   **ViewModel (`ConversationViewModel`):**
    *   Gerenciar o estado da UI (lista de mensagens, texto de entrada).
    *   Ao enviar: Adicionar mensagem do usuário à lista, simular uma resposta do bot (ex: "Recebi: [mensagem do usuário]") e adicionar à lista.
*   **Persistência Local (Room):**
    *   `InteractionDao` e `InteractionEntity`.
    *   `InteractionRepository` para salvar e carregar interações do banco de dados Room.
    *   Configuração do Hilt para injetar ViewModel, Repositório e Room DB.

**Resultado:** Um app funcional onde o usuário pode "conversar" com um bot simples, e o histórico da conversa persiste entre os usos do app (armazenado localmente).

## Fase 2: Integração com Supabase (Autenticação e Histórico Remoto)

**Objetivo:** Adicionar autenticação de usuários e mover o armazenamento do histórico de chat do local para o Supabase. Ainda sem interação real com IA.

**Entregáveis:**
*   **Supabase:**
    *   Configurar projeto, habilitar Auth (Email/Senha, Google).
    *   Criar tabelas `user_profiles` e `interactions` (com `user_id`, `text_input`, `text_response`, `timestamp`) e definir políticas RLS básicas (usuário só acessa seus dados).
*   **Android (Kotlin/Compose):**
    *   Implementar telas de Login/Cadastro (`LoginScreen`, `SignUpScreen`) e navegação.
    *   Implementar `AuthRepository` e `AuthViewModel` usando o Supabase Auth Kotlin Client.
    *   Modificar `InteractionRepository` para:
        *   Salvar interações na tabela `interactions` do Supabase (associadas ao `auth.uid()`).
        *   Carregar o histórico da tabela `interactions` do Supabase (com paginação simples).
    *   Atualizar `ConversationViewModel` para usar o repositório modificado e carregar histórico ao iniciar.

**Resultado:** Usuários podem criar contas e fazer login. O histórico de chat (ainda com respostas simuladas) é agora armazenado online no Supabase, específico para cada usuário.

**Observação (Atualizada):** A implementação da Fase 2 foi concluída e validada. Os erros de build anteriores foram resolvidos. O fluxo de autenticação (Email/Senha com confirmação, Google Sign-In, Logout) e o armazenamento/recuperação do histórico de interações no Supabase estão funcionais.

## Fase 3: Integração Inicial com Backend IA (Chat de Texto Real)

**Objetivo:** Conectar o app a um Backend de IA mínimo para obter respostas reais baseadas em LLM, substituindo as respostas simuladas. Foco apenas na interação textual.

**Entregáveis:**
*   **Backend IA (Python):**
    *   Criar um endpoint API mínimo (ex: `/generate-response` usando FastAPI/Flask).
    *   Receber texto do usuário e `user_id` (validando JWT do Supabase).
    *   Simplificação: Chamar diretamente uma API LLM (ex: Google Gemini) com o texto recebido.
    *   Salvar a interação (input do usuário, output da LLM) na tabela `interactions` do Supabase.
    *   Retornar a resposta da LLM.
*   **Android (Kotlin/Compose):**
    *   Configurar Retrofit para comunicar com a API do Backend IA.
    *   Modificar `InteractionRepository` para chamar o endpoint `/generate-response` (passando o JWT).
    *   Atualizar `ConversationViewModel` para exibir estado de carregamento enquanto espera a resposta da IA.

**Resultado:** O chatbot agora responde de forma inteligente usando um LLM real. As conversas são persistidas no Supabase. Este é um MVP funcional do core da experiência de chat.

## Fase 4: Adicionando Entrada/Saída de Voz Básica (STT/TTS)

**Objetivo:** Permitir que o usuário fale com o assistente e ouça a resposta, integrando os serviços básicos de STT e TTS do Backend IA.

**Entregáveis:**
*   **Backend IA (Python):**
    *   Endpoint `/stt`: Recebe áudio, transcreve usando Whisper/Vosk, retorna texto.
    *   Endpoint `/tts`: Recebe texto, sintetiza usando Coqui/Mozilla TTS, retorna áudio.
*   **Android (Kotlin/Compose):**
    *   Adicionar um botão de microfone (`VoiceButton`) à `ConversationScreen`.
    *   Implementar `AudioHandler`: Gravar áudio (`MediaRecorder`), enviar para `/stt`.
    *   Implementar `AudioPlayer`: Receber stream de áudio de `/tts`, reproduzir (`MediaPlayer`/`ExoPlayer`).
    *   Modificar `ConversationViewModel`: Orquestrar o fluxo de voz (clique -> gravar -> enviar STT -> receber texto -> enviar `/generate-response` -> receber resposta texto -> enviar TTS -> receber áudio -> reproduzir).
    *   Atualizar UI para indicar estados (gravando, processando, falando).

**Resultado:** O usuário pode interagir com o assistente por voz. A fala é transcrita, processada pela IA, e a resposta é falada de volta, além de exibida no chat.

## Próximos Passos (Pós-MVP)

Após completar estas 4 fases, teremos um MVP robusto. Os próximos passos seriam implementar as funcionalidades mais avançadas descritas na documentação principal:
*   RAG completo com busca vetorial.
*   Verificação de voz do dono.
*   Streaming de áudio/texto.
*   Controle ético, monetização, gamificação, feedback.
*   Testes abrangentes e otimizações. 