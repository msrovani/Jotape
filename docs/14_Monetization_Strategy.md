# KITT - Estratégia de Monetização

Este documento descreve a estratégia de monetização para o assistente virtual KITT, focando em um modelo híbrido que combina acesso gratuito (Freemium), uma assinatura paga (Premium) e compras no aplicativo (In-App Purchases - IAPs), **considerando a arquitetura com Backend de IA dedicado**.

## Fase 1: Modelo Híbrido (Freemium + Premium + IAPs)

Esta fase inicial visa construir uma base de usuários, oferecer valor claro e gerar receita sustentável.

### 1. Tier Gratuito (Free)

**Objetivo:** Atrair usuários, permitir a experimentação do core da IA e servir como funil para o Premium e IAPs.

**Recursos Incluídos:**
*   **Interação Básica com IA:** Funcionalidades de chat e resposta via texto e voz.
    *   *Limitação:* Respostas de voz podem ter qualidade/velocidade padrão ou limite de uso via **Backend IA TTS**.
*   **Modelo de IA:** Acesso ao modelo LLM padrão configurado no **Backend de IA** (e.g., Gemini versão Free).
*   **Histórico de Conversa:** Limitado (gerenciado pelo Supabase/App).
*   **Personalização:**
    *   Acesso à voz padrão do KITT (fornecida pelo **Backend IA TTS**).
    *   Possibilidade de comprar vozes, sotaques ou pacotes de personalidade adicionais via IAP (desbloqueados no **Backend IA**).
*   **Sincronização:** Limitada a 1 dispositivo ativo por vez (gerenciado via App/Supabase).
*   **Funcionalidades Excluídas:** Sem acesso a pesquisas na internet via IA, sem possibilidade de trocar o modelo de IA subjacente (controle no **Backend IA**).

### 2. Tier Premium (Assinatura Mensal/Anual)

**Objetivo:** Oferecer uma experiência completa e avançada para usuários engajados, cobrindo custos de API e gerando receita recorrente.

**Recursos Incluídos:**
*   **Todos os benefícios do Tier Gratuito.**
*   **Interação Avançada com IA (via Backend IA):**
    *   Respostas de voz com qualidade/velocidade premium ou sem limites de uso via **Backend IA TTS**.
    *   Acesso a pesquisas na internet através da IA (funcionalidade do **Backend IA**).
    *   **Acesso a Modelos de IA Avançados (via Backend IA):**
        *   Acesso a versões LLM superiores/mais recentes configuradas no **Backend IA** (e.g., Gemini Pro).
        *   **(BYOK - Bring Your Own Key):** Possibilidade de configurar chaves de API próprias (OpenAI, Anthropic) no **Backend IA** (requer UI segura no App e armazenamento seguro no Backend).
    *   Limites de uso da API do modelo padrão (Gemini Free) significativamente maiores no **Backend IA**.
*   **Histórico de Conversa:** Ilimitado (Supabase/App).
*   **Personalização Avançada (via Backend IA TTS):**
    *   Acesso a um conjunto selecionado de **3-5 vozes e 3 sotaques premium** (fornecidas pelo **Backend IA TTS**).
    *   Acesso a lançamentos periódicos/temáticos de personalização.
    *   Possibilidade de comprar vozes/sotaques/personalidades adicionais via IAP.
*   **Sincronização:** Múltiplos dispositivos (App/Supabase).
*   **Funcionalidades Adicionais:**
    *   **Análises e Insights:** Recursos para analisar padrões de conversa (requer consentimento explícito e design focado em privacidade).
    *   **Modo Offline Aprimorado:** Capacidades offline estendidas (ex: cache maior, acesso a funcionalidades que não dependem de conexão constante), conforme viabilidade técnica.
*   **Suporte:** Acesso a canais de suporte prioritário.

### 3. Loja de Personalização (In-App Purchases - IAPs)

**Objetivo:** Oferecer opções de personalização granular e gerar receita adicional de usuários Free e Premium.

**Itens Disponíveis (Entregues pelo Backend IA):**
*   **Vozes Individuais:** Comprar acesso permanente a vozes específicas no **Backend IA TTS**.
*   **Sotaques Individuais:** Comprar acesso permanente a sotaques no **Backend IA TTS**.
*   **Pacotes de Personalidade:** Comprar personas que afetam o estilo de resposta (lógica no **Backend IA**) e talvez a voz (**Backend IA TTS**).

**Implementação:**
*   IAPs nativos (Google Play Billing, etc.).
*   Validar compras no backend (webhook do Stripe/lojas -> atualizar `entitlements` do usuário no **Supabase DB**).
*   **Backend IA** consulta `entitlements` do usuário antes de gerar TTS ou aplicar personalidade.

## Fase 2: Marketplace para Criadores

**Objetivo:** Expandir exponencialmente o catálogo de personalização, criar um ecossistema e gerar uma nova fonte de receita (potencialmente com divisão de receita).

**Conceito:**
*   Desenvolver ferramentas e especificações para que terceiros (dubladores, desenvolvedores, sound designers) possam criar e submeter pacotes de voz/sotaque/personalidade para o KITT.
*   Criar um portal/processo para submissão, revisão e aprovação desses pacotes.
*   Integrar esses pacotes de terceiros na Loja de Personalização (IAP).
*   Implementar um modelo de divisão de receita com os criadores.

**Viabilidade:** A ser explorada após a maturação e sucesso da Fase 1.

## Considerações Técnicas e de Negócio

*   **Implementação de Pagamentos:** Integração Stripe/IAP, webhooks, atualização de `entitlements` no Supabase DB.
*   **Gerenciamento de Direitos (`Entitlements`):** Tabela no Supabase DB consultada pelo **Backend IA** e App Android para controlar acesso a funcionalidades/conteúdo.
*   **Medição de Uso (API):** Implementar no **Backend IA** para limites do Free tier.
*   **Segurança (BYOK):** Implementação segura no **Backend IA**.
*   **Comunicação:** UI no App Android clara sobre tiers/IAPs.
*   **Preços:** Definir preços. 