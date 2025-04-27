# 📄 Documentação do Projeto Jotape

**Versão:** 0.1.0 (Documentação Revisada)
**Data:** 2024-07-28

## 1. 🎯 Visão Geral e Objetivos

O Jotape é um assistente virtual composto por:
*   Um **App Android Nativo (Kotlin/Compose)** para interação com o usuário.
*   Um **Backend de IA dedicado (Python)** para processamento de voz, RAG, LLM e TTS.
*   **Supabase** como Backend-as-a-Service para autenticação, banco de dados (incluindo vetores com `pgvector`) e storage.

O objetivo é construir um sistema robusto, escalável e seguro, alinhado com o pipeline descrito em `docs/Prompt-AI.txt`, utilizando práticas modernas de engenharia de software.

**Papel do Desenvolvedor (e da IA Assistente):** Atuar como engenheiro de software sênior, aplicando Clean Architecture no Android, desenvolvendo o Backend de IA, configurando Supabase e garantindo a integração entre os componentes para gerar software de alta qualidade, testável e manutenível. Documentar decisões técnicas é parte integral do processo.

## 2. 🧭 Navegação da Documentação

*   **[Arquitetura e Stack Tecnológico](./01-architecture.md):** Detalhes sobre os componentes (App Android, Backend IA, Supabase), camadas e tecnologias chave.
*   **[Funcionalidades Detalhadas](./02-features.md):** Descrição do fluxo de trabalho para as principais funcionalidades (STT, RAG/LLM, TTS, Verificação de Voz) envolvendo App, Backend IA e Supabase.
*   **[UI/UX com Jetpack Compose](./03-ui-ux.md):** Diretrizes para a interface do usuário no App Android.
*   **[Feedback, Métricas & Gamificação](./04-feedback-metrics.md):** Coleta de feedback (para fine-tuning no Backend IA), métricas e gamificação via Supabase.
*   **[Privacidade, Segurança & Ética](./05-security-privacy.md):** Abordagem de segurança (Criptografia, RLS, Storage Policies) e moderação de conteúdo (no Backend IA).
*   **[DevOps & QA](./06-devops-qa.md):** Estratégias de CI/CD e testes para App Android, Backend IA e Supabase.
*   **[Estratégia de Monetização](./14_Monetization_Strategy.md):** Modelo Freemium/Premium/IAP baseado em funcionalidades do Backend IA.
*   **[Controle Ético e Sanções](./17_Ethical_Control_Sanctions.md):** Sistema de detecção e sanções para conteúdo impróprio, implementado no Backend IA.
*   **[Roadmap / TODO](./TODO.md):** Lista detalhada de tarefas pendentes para o desenvolvimento.

## 3. 🛠️ Configuração Inicial (Desenvolvimento)

Para rodar o projeto Android localmente, siga estes passos:

1.  **Clonar o Repositório:** Obtenha o código-fonte.
2.  **Configurar Supabase:**
    *   Crie um projeto no Supabase.
    *   Habilite Autenticação (Email/Senha, Google).
    *   Configure as tabelas necessárias no banco de dados (e.g., `users`, `interactions`).
    *   Ative as políticas de Row Level Security (RLS) apropriadas (ver `docs/05-security-privacy.md`).
    *   **Importante:** Configure o **URI de Redirecionamento** para o Google Sign-In no Google Cloud Console (veja [Fluxo de Processamento](./03_Fluxo_Processamento.md#44-fluxo-de-autenticacao-google-sign-in)).
3.  **Credenciais Locais:**
    *   Crie um arquivo `local.properties` na raiz do projeto Android (`Jotape/`).
    *   Adicione as seguintes chaves com os valores do seu projeto Supabase e Google Cloud:
        ```properties
        supabase.url=https://<SEU_ID_PROJETO>.supabase.co
        supabase.key=<SUA_CHAVE_ANON_SUPABASE>
        google.web.client.id=<SEU_GOOGLE_WEB_CLIENT_ID_PARA_OAUTH>
        sdk.dir=<CAMINHO_PARA_SEU_SDK_ANDROID> #(Ex: C\:\\Compiladores\\SDK)
        ```
    *   **NÃO FAÇA COMMIT DESTE ARQUIVO!** Ele está no `.gitignore`.
4.  **Dependências Gradle:**
    *   Sincronize o projeto com os arquivos Gradle no Android Studio/IntelliJ.
    *   Certifique-se de ter as dependências essenciais, como `kotlin-stdlib` e `material-icons-extended` (gerenciadas via `gradle/libs.versions.toml` e `app/build.gradle.kts`).
5.  **Executar o App:** Compile e execute o módulo `app` em um emulador ou dispositivo físico.

## 4. 📊 Status Atual da Implementação (Supabase)

**(Esta seção foi removida pois o TODO.md agora detalha o status e tarefas pendentes de forma mais completa.)** 