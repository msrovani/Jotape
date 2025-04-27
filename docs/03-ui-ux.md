# 📱 UI/UX com Jetpack Compose

A interface do usuário será construída inteiramente com Jetpack Compose, seguindo os princípios do Material Design 3 e focando em uma experiência de voz fluida, **coordenada com o Backend de IA**.

## 4.1 Componentes Principais (Composables)

*   **`LoginScreen` & `SignUpScreen`:**
    *   Telas padrão para entrada de credenciais (email/senha), botões de ação (Login, Cadastro, Login com Google) e navegação entre elas.
    *   Utilizam componentes básicos do Material 3 (`OutlinedTextField`, `Button`, `TextButton`, `CircularProgressIndicator`).
    *   Exibem mensagens de erro e feedback de carregamento controlados pelo ViewModel.

*   **`VoiceButton`:**
    *   Botão principal para iniciar a interação por voz.
    *   Deve ter animações claras para indicar os estados: inativo, **gravando áudio local**, **enviando para STT**, **aguardando resposta IA**, **reproduzindo áudio TTS**.
    *   Exemplo de Estados Visuais:
        *   Inativo: Ícone de microfone estático.
        *   Gravando: Animação de ondas sonoras ou pulsação.
        *   Enviando/Aguardando: Animação de spinner ou similar.
        *   Reproduzindo TTS: Ícone de alto-falante ou similar.

*   **`TranscriptView`:**
    *   Exibe o histórico da conversa (entradas do usuário **(transcritas pelo backend)** e respostas do assistente **(geradas pelo backend)**).
    *   Deve ser rolável (`LazyColumn`).
    *   Implementar paginação: Carregar mais interações do histórico (via ViewModel -> Repositório -> Supabase/Room) quando o usuário rolar até o topo.
    *   Distinguir visualmente as falas do usuário e do assistente.

*   **`StatusIndicator`:**
    *   Uma área (talvez na parte superior ou inferior) que fornece feedback textual ou iconográfico sobre o estado atual: **"Gravando..."**, **"Processando sua fala..."**, **"Pensando..."**, **"Falando..."**, **"Erro de conexão com IA"**, etc.
    *   Controlado pelo estado do `ConversationViewModel`, que reflete a comunicação com o **Backend de IA**.

## 4.2 Ações do Usuário

1.  **`onClick` no `VoiceButton`:**
    *   Dispara um evento para o `ConversationViewModel`.
    *   ViewModel inicia a **captura de áudio local**.
    *   Após captura (ou durante, se streaming), ViewModel **envia o áudio para o endpoint STT do Backend IA**.
    *   Atualiza o estado da UI para 'gravando', 'enviando', 'processando fala', etc., conforme o fluxo avança.

2.  **Swipe/Scroll no `TranscriptView`:**
    *   Detectar scroll até o topo.
    *   Disparar evento para o ViewModel carregar a próxima página de histórico.
    *   ViewModel chama o Caso de Uso/Repositório para buscar mais dados (Supabase ou Room).

## 4.3 Modo Android Auto

**Nota Importante:** O desenvolvimento e a integração com Android Auto estão **fora do escopo inicial** deste projeto. As informações abaixo são mantidas como referência futura caso essa funcionalidade seja priorizada posteriormente.

Desenvolver para Android Auto requer considerações especiais de segurança e usabilidade.

*   **Biblioteca:** Usar a [Android for Cars App Library](https://developer.android.com/training/cars/navigation) que permite criar apps de navegação, estacionamento e carregamento. Para um assistente como o Jotape, a integração pode ser mais limitada ou exigir uma abordagem de *Assistente de Condução* se permitido pelas políticas.
*   **UI Segura:**
    *   Layouts devem ser extremamente simples, com fontes grandes e áreas de toque generosas.
    *   Limitar a quantidade de informação na tela.
    *   Seguir as diretrizes de design do Android Auto rigorosamente. [Android Auto Design Guidelines](https://developers.google.com/cars/design/android-auto)
*   **Prioridade de Voz:**
    *   Interação primária deve ser por voz.
    *   Feedback deve ser predominantemente auditivo (TTS claro e conciso).
    *   Minimizar a necessidade de interação tátil.
*   **Restrições:** O sistema operacional do Android Auto impõe restrições sobre o que pode ser exibido e quando, para minimizar a distração do motorista.

**Nota:** A viabilidade e o escopo exato da integração com Android Auto precisam ser validados contra as políticas atuais da plataforma Google. Uma alternativa pode ser um modo de condução otimizado dentro do próprio app mobile, sem integração direta com a tela do carro. 