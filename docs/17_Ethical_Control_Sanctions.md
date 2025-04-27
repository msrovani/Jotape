# KITT - Controle Ético e Sistema de Sanções

## 1. Visão Geral

Este documento descreve o sistema de controle ético implementado no KITT para detectar e mitigar interações inadequadas, prejudiciais ou que violem os termos de uso. O objetivo é garantir um ambiente seguro e positivo para todos os usuários, ao mesmo tempo que se oferece um grau de flexibilidade e a possibilidade de revisão para casos mais graves.

A lógica principal reside no **Backend de IA**.

## 2. Mecanismo de Detecção

- **Fonte:** A detecção primária ocorre durante o processamento da interação pelo **`EthicalControlService` (componente do Backend de IA)**, invocado tipicamente **após a geração da resposta do LLM** e **antes do envio da resposta final ao usuário** (e potencialmente também na entrada do usuário).
- **Método:**
    - **Análise via LLM:** O próprio LLM principal ou um modelo secundário no **Backend IA** pode ser instruído a avaliar o texto.
    - **Modelos de Classificação Dedicados:** O **Backend IA** pode usar modelos específicos para moderação.
    - **APIs Externas:** O **Backend IA** pode chamar APIs de moderação de terceiros.
    - **Contexto:** A análise no **Backend IA** leva em conta o contexto da conversa.
- **Categorias (Exemplos):** Discurso de Ódio, Assédio, Conteúdo Sexual Explícito, Conteúdo Perigoso/Ilegal, Violência Gráfica, Desinformação Prejudicial, Spam/Abuso.

## 3. Níveis de Violação e Pontuação

As violações detectadas são classificadas em 8 níveis de severidade crescente. A pontuação exata pode ser ajustada, mas a classificação no nível é o principal.

1.  **Nível 1: Levemente Inadequado/Impolido**
    *   **Descrição:** Uso de linguagem borderline, levemente rude ou sarcasmo que pode ser mal interpretado, mas sem intenção clara de ofensa grave.
    *   **Exemplos:** Palavrões de baixo calão usados casualmente, reclamações rudes sobre a IA.
2.  **Nível 2: Claramente Impróprio/Ofensivo**
    *   **Descrição:** Linguagem claramente desrespeitosa, insultos leves, conteúdo questionável mas não perigoso.
    *   **Exemplos:** Insultos diretos à IA ou a grupos genéricos (sem ser discurso de ódio), piadas de mau gosto.
3.  **Nível 3: Conteúdo Problemático Menor**
    *   **Descrição:** Promoção de estereótipos leves, desinformação de baixo impacto, conselhos questionáveis (não perigosos), spam leve.
    *   **Exemplos:** Repetir a mesma pergunta várias vezes rapidamente, compartilhar links não solicitados de forma branda, generalizações incorretas sobre grupos.
4.  **Nível 4: Violação Moderada**
    *   **Descrição:** Conteúdo sexualmente sugestivo, assédio leve, discurso de ódio codificado/implícito, desinformação mais significativa (mas não imediatamente perigosa).
    *   **Exemplos:** Insinuações sexuais, comentários depreciativos persistentes, teorias da conspiração sem incitação direta à violência.
---
*Usuário pode optar por ignorar sanções automáticas para Níveis 1-4.*
---
5.  **Nível 5: Violação Séria**
    *   **Descrição:** Discurso de ódio mais explícito (mas sem ameaça direta), assédio direcionado, promoção de atos perigosos não letais, conteúdo adulto não consensual (geração/descrição).
    *   **Exemplos:** Uso de termos pejorativos claros contra grupos protegidos, bullying direcionado, instruções para práticas inseguras.
6.  **Nível 6: Violação Grave**
    *   **Descrição:** Assédio grave, ameaças veladas, promoção de automutilação, desinformação com potencial claro de dano, geração de conteúdo ilegal (excluindo CSAM).
    *   **Exemplos:** Doxxing leve, instruções detalhadas para burlar sistemas, glorificação de violência, negação de eventos trágicos.
7.  **Nível 7: Violação Crítica**
    *   **Descrição:** Ameaças críveis de violência, assédio severo e persistente, instruções para atividades ilegais perigosas, tentativa de usar a IA para atividades fraudulentas graves.
    *   **Exemplos:** Planejamento de atos violentos, perseguição obsessiva, instruções para criar explosivos ou drogas.
8.  **Nível 8: Violação Intolerável**
    *   **Descrição:** Conteúdo relacionado à exploração sexual infantil (CSAM), ameaças diretas e explícitas de violência grave/morte, coordenação de atividades criminosas graves em tempo real, terrorismo.
    *   **Exemplos:** Qualquer tentativa de gerar, solicitar ou compartilhar CSAM, ameaças de morte específicas.

## 4. Mapeamento de Sanções

| Nível | Sanção Automática                                       | Permite Justificativa? | Ação Adicional Potencial       |
| :---- | :------------------------------------------------------ | :--------------------- | :----------------------------- |
| 1     | Orientação/Alerta Leve na Interface (Ignorável)         | Não                    | Log interno                    |
| 2     | Aviso Claro na Interface (Ignorável)                    | Não                    | Log interno                    |
| 3     | Aviso + Bloqueio da Mensagem/Resposta Específica        | Não                    | Log interno                    |
| 4     | Aviso Forte + Bloqueio + Cooldown Curto (ex: 5 min sem IA) | Não                    | Log interno com metadados      |
| 5     | Aviso Severo + Bloqueio + Cooldown Longo (ex: 1 hora)    | **Sim**                | Log detalhado, Flag no Usuário |
| 6     | Suspensão Temporária da IA (ex: 24 horas) + Bloqueio   | **Sim**                | Log detalhado, Notificação Adm |
| 7     | Suspensão Temporária da Conta (ex: 7 dias) + Bloqueio  | **Sim**                | Log detalhado, Revisão Adm     |
| 8     | Banimento Permanente da Conta + Bloqueio                 | **Sim (Apelação)**     | Log detalhado, Reporte Legal   |

* **Ignorável:** Sanções dos níveis 1-4 podem ser desativadas nas configurações do usuário, mas as violações ainda podem ser logadas internamente para análise.
* **Bloqueio:** A mensagem ofensiva ou a resposta gerada é impedida de ser exibida/processada.
* **Cooldown/Suspensão IA:** O usuário não pode interagir com os recursos de IA do KITT pelo período especificado.
* **Suspensão Conta:** O usuário não pode fazer login ou usar qualquer parte do KITT.
* **Flag/Notificação/Revisão Adm:** Indica que a violação requer atenção da equipe de moderação/administração.
* **Reporte Legal:** Para violações de Nível 8, especialmente CSAM ou ameaças críveis, a KITT pode ser legalmente obrigada a reportar às autoridades competentes.

## 5. Processo de Justificativa e Revisão (Níveis 5-8)

1.  **Notificação ao Usuário:** Ao receber uma sanção de nível 5 ou superior, o usuário é notificado claramente na interface sobre a violação detectada, o nível, a sanção aplicada e a opção de submeter uma justificativa (ou apelação, no caso de banimento).
2.  **Submissão da Justificativa:** O usuário tem um prazo (ex: 48 horas) para acessar um formulário (dentro do app ou via link externo seguro) onde pode explicar o contexto, argumentar contra a detecção ou reconhecer o erro e solicitar reconsideração.
3.  **Revisão Humana:** As justificativas são encaminhadas para uma fila de revisão humana (equipe de moderação/confiança e segurança).
4.  **Critérios de Revisão:** O revisor analisa a justificativa, o histórico da conversa (anonimizado se possível, mas com contexto suficiente), o histórico de violações anteriores do usuário e a gravidade da violação detectada.
5.  **Decisão:** O revisor pode:
    *   **Manter a Sanção:** Se a violação for confirmada e a sanção justificada.
    *   **Reduzir a Sanção:** Se houver atenuantes ou for uma primeira ofensa grave.
    *   **Anular a Sanção:** Se a detecção for considerada um falso positivo ou o contexto justificar.
6.  **Comunicação ao Usuário:** O usuário é notificado sobre a decisão final da revisão.

## 6. Configuração pelo Usuário (Níveis 1-4)

- **Opção:** Nas configurações do KITT, haverá uma opção como "Nível de Sensibilidade da Moderação" ou "Filtragem de Conteúdo Leve".
- **Configurações:**
    - **Padrão:** Aplica todas as sanções conforme a tabela.
    - **Reduzida:** Ignora as *sanções automáticas* (orientação, aviso, bloqueio leve) para violações detectadas nos níveis 1 a 4. A detecção ainda pode ocorrer para fins de log e análise interna, mas o usuário não é interrompido.
- **Importante:** A opção de ignorar **não se aplica** aos níveis 5 a 8, que sempre terão as sanções correspondentes aplicadas devido à sua gravidade.

## 7. Notas Técnicas de Implementação

- **`EthicalControlService`:** Componente chave dentro do **Backend de IA**, responsável pela detecção, pontuação e mapeamento para níveis/sanções.
- **Tabela `ethical_violations` (Supabase DB):** Armazena logs de violações, status de revisão.
- **Tabela `profiles` ou `user_settings` (Supabase DB):** Armazena preferência do usuário (ignorar níveis 1-4), flags de sanção ativa.
- **`EntitlementService` (Backend IA / Supabase):** Consultada pelo **Backend IA** para aplicar restrições de funcionalidade (cooldowns, suspensões).
- **UI (App Android):** Exibe alertas, avisos, bloqueios, informações de sanção e fluxo de justificativa.

## 8. Considerações Futuras

- Refinamento contínuo dos modelos de detecção e pontuação.
- Melhoria da interface de justificativa/apelação.
- Relatórios de transparência sobre ações de moderação.
- Níveis de sanção mais granulares ou adaptativos com base no histórico do usuário. 