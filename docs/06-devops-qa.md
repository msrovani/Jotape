# 🚀 DevOps & QA

Práticas de DevOps e Garantia de Qualidade (QA) são essenciais para entregar um aplicativo **e um backend de IA** robustos e confiáveis de forma consistente.

## 7.1 CI/CD (Integração Contínua / Entrega Contínua)

*   **Plataforma:** GitHub Actions (ou similar).
*   **Workflows Separados (Recomendado):**
    *   **Workflow App Android (Kotlin/Compose):**
        1.  Trigger: Push/PR para branches do App.
        2.  Checkout, Setup JDK & Cache Gradle.
        3.  Lint (`ktlintCheck`).
        4.  Unit Tests (`testDebugUnitTest`).
        5.  Build App (`assembleDebug`/`Release`).
        6.  Instrumented Tests (Opcional/Noturno).
        7.  Deploy App (Firebase App Distribution, Google Play).
    *   **Workflow Backend IA (Python - Exemplo):**
        1.  Trigger: Push/PR para branches do Backend.
        2.  Checkout, Setup Python & Cache (pip/poetry).
        3.  Lint (e.g., `flake8`, `black`).
        4.  Unit/Integration Tests (e.g., `pytest` - mockando APIs externas/Supabase ou usando instâncias de teste).
        5.  Build (e.g., Docker image).
        6.  Deploy Backend (e.g., para serviço de container como Cloud Run, Kubernetes, etc.).
*   **Coordenação:** Garantir que as versões do App Android e do Backend de IA sejam compatíveis, potencialmente usando versionamento ou flags de feature.

## 7.2 Supabase Migrations & Versionamento

Alterações no schema do banco de dados e políticas RLS precisam ser versionadas e aplicadas de forma consistente entre ambientes (local, staging, produção).

*   **Ferramenta:** **Supabase CLI**.
*   **Fluxo de Trabalho:**
    1.  **Desenvolvimento Local:** Usar o Supabase Studio local (`supabase start`) ou um projeto de desenvolvimento dedicado no Supabase Cloud.
    2.  **Criar Migração:** Após fazer alterações no schema local (via UI do Studio ou SQL), criar um novo arquivo de migração: `supabase migration new <nome_descritivo_da_migracao>`.
    3.  **Editar Migração:** Escrever o SQL necessário no arquivo de migração gerado (dentro de `supabase/migrations`). Isso inclui `CREATE TABLE`, `ALTER TABLE`, `CREATE POLICY`, `ALTER POLICY`, etc.
    4.  **Aplicar Localmente (Testar):** `supabase db reset` (para limpar e reaplicar todas as migrações) ou aplicar a última migração.
    5.  **Commit:** Adicionar o arquivo de migração ao Git.
    6.  **Deploy no Staging/Produção:** Vincular a CLI ao projeto remoto (`supabase link --project-ref <id_do_projeto>`) e aplicar as migrações pendentes: `supabase db push`.
*   **Considerações:**
    *   **NÃO** usar a UI do Supabase Cloud para fazer alterações de schema diretamente em produção. Use migrações via CLI.
    *   Testar migrações complexas (especialmente aquelas que alteram dados) cuidadosamente em um ambiente de staging antes de aplicar em produção.
*   **Referências:** [Supabase CLI](https://supabase.com/docs/guides/cli), [Supabase Migrations](https://supabase.com/docs/guides/database/migrations)

## 7.3 Estratégia de Testes (QA)

Uma combinação de testes em diferentes níveis garante a qualidade do aplicativo **e do backend**.

*   **Testes Unitários:**
    *   **Android:** Camadas `domain`, `data` (Kotlin - JUnit, MockK/Mockito).
    *   **Backend IA:** Módulos individuais (Python - `pytest`, `unittest`). Testar lógica de pipeline, parsing, construção de prompt, etc., isoladamente.
*   **Testes de Integração:**
    *   **Android:** Camada `data` (Kotlin - Testes Instrumentados). Testar Repositório <-> Room, Repositório <-> Chamadas de Rede Mockadas (para Backend IA e Supabase).
    *   **Backend IA:** Testar interações entre componentes do backend (Python - `pytest`). Ex: STT -> RAG, RAG -> LLM, Interação com Supabase DB (`pgvector`), Interação com Supabase Storage.
*   **Testes de Contrato (API):**
    *   Verificar se a API exposta pelo **Backend de IA** e pelas **Edge Functions** adere ao contrato esperado pelo App Android (e vice-versa). Ferramentas como Pact podem ser úteis.
*   **Testes de UI (End-to-End - E2E) - Android:**
    *   **Onde:** Camada `presentation` (Compose Testing, Espresso).
    *   **Foco:** Simular interações do usuário, **envolvendo chamadas reais (ou mockadas no nível da rede) para o Backend de IA e Supabase**, verificando o fluxo completo.
    *   **Execução:** Lenta, requer dispositivo/emulador.
*   **Testes Manuais/Exploratórios:**
    *   Essenciais para avaliar a experiência completa (UI, Voz, Respostas IA), usabilidade e encontrar bugs não óbvios em ambos, App e Backend.
    *   Realizados em diferentes dispositivos e versões do Android.

## 7.4 Notas de Build e Teste no Emulador (Atualizado 2025-04-26)

Durante o desenvolvimento inicial, foram encontrados e resolvidos os seguintes problemas:

*   **Erro de Build (BuildConfig.java):** A geração automática do `BuildConfig.java` falhava devido à formatação incorreta de strings no `buildConfigField` dentro de `app/build.gradle.kts`. A formatação correta é `"\"${variavel}\"". Além disso, foi necessário executar `./gradlew clean :app:generateDebugBuildConfig --rerun-tasks` para forçar a regeneração e limpar o cache do Gradle.
*   **Erro de Runtime (Ktor Engine):** O aplicativo falhava na inicialização com `IllegalStateException: Failed to find HTTP client engine`. Isso ocorreu devido à declaração de múltiplos engines Ktor (`android`, `okhttp`, `cio`) no `app/build.gradle.kts`. A solução foi remover os engines extras e manter apenas `implementation(libs.ktor.client.okhttp)`.
*   **Warnings de Depreciação:** Corrigidos warnings relacionados ao uso de `Icons.Filled.Send` (substituído por `Icons.AutoMirrored.Filled.Send` em `ConversationScreen.kt`) e `window.statusBarColor` (removido de `Theme.kt`, preferindo a gestão pelo `WindowCompat` e tema do Compose).
*   **Performance Emulador vs. Dispositivo Físico:** Observou-se **lentidão extrema e frames pulados** (e.g., `Skipped 195 frames!`) ao rodar o aplicativo no Android Emulator, podendo levar a ANRs no sistema. No entanto, **o aplicativo apresentou performance aceitável em um dispositivo físico**. Isso destaca a importância de:
    *   Usar o emulador principalmente para testes funcionais e detecção de crashes/erros lógicos.
    *   **Realizar testes de performance e avaliação de UX em dispositivos físicos reais.**
    *   Se a performance for um problema *também* em dispositivos físicos, usar o Profiler do Android Studio para identificar gargalos na thread principal. 