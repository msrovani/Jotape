# 🔐 Privacidade, Segurança & Ética

A segurança e a privacidade dos dados do usuário são de suma importância, especialmente devido à natureza sensível das interações de voz e conversas.

## 6.1 Ações de Segurança

*   **Criptografia em Repouso (Local):**
    *   Dados armazenados localmente no cache do Room (e.g., `pending_interactions`, talvez partes do histórico cacheadas) devem ser criptografados.
    *   **Tecnologia:** Utilizar a biblioteca [Jetpack Security (Security Crypto)](https://developer.android.com/topic/security/data) para criptografar SharedPreferences e arquivos. Para Room, pode-se usar [SQLCipher for Android](https://github.com/sqlcipher/android-database-sqlcipher) integrado ao Room ou criptografar dados sensíveis *antes* de inseri-los.
    *   Gerenciamento de Chaves: Usar o [Android Keystore system](https://developer.android.com/training/articles/keystore) para armazenar as chaves de criptografia de forma segura.

*   **Criptografia em Trânsito:**
    *   Todas as comunicações entre o App Android e o Supabase (Client, Functions, Storage) ocorrem sobre **HTTPS**, garantindo a criptografia em trânsito por padrão.
    *   Verificar configurações de rede (e.g., via OkHttp/Retrofit) para garantir que apenas conexões seguras (TLS) sejam permitidas.

*   **Supabase Row-Level Security (RLS):**
    *   **Este é o pilar da segurança de dados no backend.**
    *   **Regra de Ouro:** Habilitar RLS (`ALTER TABLE ... ENABLE ROW LEVEL SECURITY;`) em **TODAS** as tabelas que contêm dados específicos do usuário (e.g., `interactions`, `voice_models`, `feedback`, `user_profiles`, `user_badges`).
    *   **Políticas Essenciais (Exemplos SQL - Tabela `interactions`):**
        ```sql
        -- Para a tabela 'interactions' (adaptar para outras tabelas)

        -- Permitir que usuários logados leiam SUAS PRÓPRIAS interações
        CREATE POLICY "Allow individual user select access"
        ON interactions
        FOR SELECT
        USING (auth.uid() = user_id);

        -- Permitir que usuários logados insiram interações PARA SI MESMOS
        CREATE POLICY "Allow individual user insert access"
        ON interactions
        FOR INSERT
        WITH CHECK (auth.uid() = user_id);

        -- Permitir que usuários logados atualizem SUAS PRÓPRIAS interações
        CREATE POLICY "Allow individual user update access"
        ON interactions
        FOR UPDATE
        USING (auth.uid() = user_id) -- Linhas que podem ser alvo
        WITH CHECK (auth.uid() = user_id); -- Validação dos novos dados

        -- Permitir que usuários logados deletem SUAS PRÓPRIAS interações
        CREATE POLICY "Allow individual user delete access"
        ON interactions
        FOR DELETE
        USING (auth.uid() = user_id);
        ```
    *   **Pontos Chave:**
        *   `auth.uid()`: Identifica o usuário autenticado via JWT.
        *   `user_id`: Coluna na tabela que armazena o UUID do dono da linha.
        *   `USING`: Filtra linhas visíveis/operáveis.
        *   `WITH CHECK`: Valida dados em `INSERT`/`UPDATE`.
        *   **Default DENY:** Se RLS habilitada, acesso é negado a menos que uma política permita.
    *   **Testes:** Testar RLS rigorosamente com diferentes usuários é crucial.
    *   **Aplicação a `voice_models`:** As mesmas políticas (`SELECT`, `INSERT`, `UPDATE`, `DELETE` baseadas em `auth.uid() = user_id`) devem ser aplicadas à tabela `voice_models` para proteger os metadados e os paths dos modelos.

*   **Supabase Storage Security:**
    *   Configurar **Políticas de Acesso** nos Buckets do Supabase Storage.
    *   Exemplo: Permitir que um usuário autenticado (`auth.uid()`) faça upload (`insert`) apenas para um caminho que inclua seu ID (`private/{user_id}/*`) e leia (`select`) apenas de seu próprio caminho.
    *   **Crucial para Modelos de Voz:** É fundamental aplicar políticas restritivas ao bucket que armazena os arquivos de modelo de voz (e.g., `private/voice_models/`) para garantir que apenas o usuário dono (e talvez as Edge Functions autorizadas) possam acessar esses arquivos, complementando a segurança da RLS na tabela de metadados.
    *   [Supabase Storage Access Control](https://supabase.com/docs/guides/storage/access-control)

*   **Validação de JWT:**
    *   Supabase gerencia a validação de JWT automaticamente para acessos ao DB e Storage.
    *   Nas **Edge Functions**, é necessário verificar manualmente o cabeçalho `Authorization` (Bearer token) usando as bibliotecas do Supabase para garantir que a requisição é autenticada e obter o `user_id`.

## 6.2 Moderação de Aprendizado e Conteúdo

É importante impedir que o assistente aprenda ou gere conteúdo impróprio/perigoso. A responsabilidade principal reside no **Backend de IA**.

*   **Filtro de Entrada/Saída (Backend de IA):**
    *   O **Backend de IA** (especificamente o `EthicalControlService` ou componente similar no pipeline RAG/LLM) deve incluir um `FilterPipeline`.
    *   **Na Entrada (antes da LLM):** Filtrar PII, linguagem ofensiva ou prompts maliciosos da entrada do usuário recebida do App Android.
    *   **Na Saída (após a LLM):** Filtrar respostas da LLM que contenham conteúdo impróprio, perigoso, ou que violem políticas de uso, **antes** de enviar a resposta para o App Android.
    *   Usar listas de palavras-chave, regex, **modelos de classificação dedicados** ou **chamadas a APIs de moderação** (e.g., Perspective API, moderação da OpenAI/Gemini).
    *   Este filtro é a implementação do **`EthicalControl`** mencionado em `Prompt-AI.txt` e base para o sistema de sanções em `17_Ethical_Control_Sanctions.md`.

*   **Validação Leve (Edge Functions/App Android - Opcional):**
    *   Podem existir filtros mais simples ou validações básicas no lado do cliente (App Android) ou nas Edge Functions (como gateways) para bloquear conteúdo obviamente problemático *antes* de chegar ao Backend de IA, como uma primeira linha de defesa, mas a lógica principal e mais robusta está no Backend de IA.

*   **Revisão Humana (Opcional):**
    *   Implementar uma `HumanReviewQueue`.
    *   Uma Edge Function ou trigger pode marcar certas interações (e.g., aquelas com baixo score de feedback, ou que ativaram filtros de moderação) para revisão.
    *   Essas interações seriam inseridas em uma tabela `review_queue` no Supabase.
    *   Uma interface de admin (separada) permitiria a revisores humanos analisar essas interações.
    *   RLS garantiria que apenas administradores/revisores autorizados possam acessar esta fila.

## 6.3 Considerações Éticas

*   **Transparência:** Ser claro com o usuário sobre como seus dados (voz, conversas) são usados, armazenados e processados.
*   **Consentimento:** Obter consentimento explícito para coleta de dados, especialmente voz.
*   **Controle do Usuário:** Permitir que o usuário visualize e delete seu histórico de interações e modelos de voz.
*   **Bias:** Estar ciente do potencial de bias nos modelos de IA (STT, LLM, modelo de voz) e monitorar/mitigar sempre que possível. 