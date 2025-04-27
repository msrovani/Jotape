# 📊 Feedback, Métricas & Gamificação

Esta seção descreve como o feedback do usuário é coletado e como métricas e elementos de gamificação são implementados usando Supabase.

## 5.1 Fluxo de Feedback

O feedback explícito do usuário sobre as respostas do assistente é crucial para o aprendizado e melhoria contínua.

*   **Coleta (App Android):**
    *   Incluir botões de feedback simples (e.g., 👍 e 👎) associados a cada resposta do assistente na `TranscriptView`.
    *   *Opcional:* Detecção de palavras-chave de cortesia/frustração para feedback implícito.
*   **Armazenamento (Supabase):**
    *   Tabela `feedback` no Supabase DB (`id`, `interaction_id`, `user_id`, `rating`, `timestamp`, `comment`).
    *   RLS: Usuários podem inserir seu próprio feedback.
*   **Ação (App Android):**
    *   `OnFeedbackSubmitted()`: Envia dados para a tabela `feedback` via Repositório -> Supabase Client (`insert`).
*   **Utilização (Backend de IA):**
    *   Os dados coletados na tabela `feedback` serão utilizados periodicamente pelo **Backend de IA** como parte do **loop de aprendizado contínuo**, agregados em formato JSONL para **fine-tuning (e.g., LoRA/QLoRA)** dos modelos LLM, conforme descrito em `Prompt-AI.txt`.

## 5.2 Métricas em Tempo Real (Dashboard)

Visualizar métricas de uso e feedback pode ajudar a entender o comportamento do usuário e a qualidade do assistente.

*   **Supabase Realtime:**
    *   O App (ou uma aplicação web de dashboard separada) pode se inscrever em mudanças nas tabelas `interactions` ou `feedback` usando as **Supabase Realtime Subscriptions**.
    *   [Supabase Realtime Client (Kotlin)](https://github.com/supabase-community/supabase-kt/blob/master/Realtime/src/commonMain/kotlin/io/github/jan/supabase/realtime/Realtime.kt)
*   **Ações (App/Dashboard):**
    *   `RealtimeListener()`: Um listener no ViewModel/Componente recebe eventos de novas inserções/atualizações (e.g., nova interação, novo feedback).
    *   Atualiza a UI do dashboard (e.g., contadores, gráficos) em tempo real.
*   **Considerações:**
    *   Gerenciar o ciclo de vida da subscrição Realtime para conectar/desconectar quando a tela/componente está visível/invisível para economizar recursos.
    *   Para métricas agregadas complexas, pode ser mais eficiente usar Funções de Banco de Dados (RPC) chamadas periodicamente em vez de ouvir cada evento individual.

## 5.3 Gamificação

Elementos de gamificação podem aumentar o engajamento do usuário.

*   **Badges/Conquistas (Supabase):**
    *   Criar tabelas `badges` (definições: `id`, `name`, `description`, `icon_url`) e `user_badges` (associação: `id`, `user_id`, `badge_id`, `achieved_at`).
    *   RLS: Usuários podem ler seus próprios `user_badges`. Definições de `badges` são públicas.
*   **Triggers de Banco de Dados (Supabase - SQL):**
    *   Criar triggers no PostgreSQL que rodam após `INSERT` na tabela `interactions` ou outras tabelas relevantes.
    *   Exemplo: Um trigger na tabela `interactions` pode verificar se o usuário atingiu 10 interações e, se sim, inserir uma linha na `user_badges` para o badge "Conversador Iniciante".
    *   `CREATE TRIGGER check_interaction_milestone AFTER INSERT ON interactions FOR EACH ROW EXECUTE FUNCTION grant_beginner_badge();`
*   **Notificações (Supabase Realtime / Push):**
    *   O App pode usar **Supabase Realtime** para ouvir inserções na tabela `user_badges`.
    *   Quando um novo badge é concedido (detectado pelo listener Realtime), o App exibe uma notificação local (Toast, Snackbar ou notificação do sistema) parabenizando o usuário.
    *   Alternativamente, um trigger/função no Supabase poderia enviar uma notificação Push (requer configuração adicional de serviços de push como FCM).
*   **Eventos Rastreáveis:**
    *   `RoutineCreated`: Quando o usuário define uma nova rotina (se aplicável).
    *   `FavorRequested`: Contagem de interações que usam palavras de cortesia.
    *   Estes eventos podem ser colunas na tabela `interactions` ou tabelas separadas, alimentando triggers para badges. 