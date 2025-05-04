import { serve } from 'https://deno.land/std@0.177.0/http/server.ts'
import { createClient, SupabaseClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { corsHeaders } from '../_shared/cors.ts'

// Interface para o corpo da requisição esperado
interface RequestPayload {
  prompt: string;
  userId: string; // Assumindo que você enviará o ID do usuário autenticado
}

// Interface para representar uma interação no banco de dados
// Ajuste os tipos conforme sua tabela 'interactions'
interface Interaction {
  id?: number;
  user_id: string;
  user_input: string | null;
  assistant_response: string | null;
  timestamp?: string;
}

// Gemini API Interfaces
interface GeminiContent {
    parts: { text: string }[];
    role: 'user' | 'model';
}

interface GeminiSafetyRating {
    category: string;
    probability: string;
}

interface GeminiResponse {
    candidates: {
        content: GeminiContent;
        finishReason: string;
        index: number;
        safetyRatings: GeminiSafetyRating[];
    }[];
    promptFeedback?: { // Optional based on potential blocking
        safetyRatings: GeminiSafetyRating[];
    };
}

// --- Constantes ---
const GEMINI_API_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent';
const HISTORY_LIMIT = 20; // Número de interações passadas a serem consideradas

// --- Funções Auxiliares ---

/**
 * Busca o histórico de interações recentes para um usuário.
 */
async function getInteractionHistory(supabaseClient: SupabaseClient, userId: string): Promise<Interaction[]> {
    const { data, error } = await supabaseClient
        .from('interactions')
        .select('user_input, assistant_response, timestamp')
        .eq('user_id', userId)
        .order('timestamp', { ascending: false })
        .limit(HISTORY_LIMIT);

    if (error) {
        console.error('Error fetching history:', error);
        return []; // Retorna vazio em caso de erro, pode ser ajustado
    }
    // Inverte para ter a ordem cronológica correta (mais antigo primeiro)
    return (data || []).reverse();
  }

/**
 * Formata o histórico de interações para a API do Gemini.
 */
function formatHistoryForGemini(history: Interaction[], currentPrompt: string): GeminiContent[] {
    const contents: GeminiContent[] = [];
    history.forEach(interaction => {
        if (interaction.user_input) {
            contents.push({ role: 'user', parts: [{ text: interaction.user_input }] });
        }
        if (interaction.assistant_response) {
            // Verifica se a última mensagem foi do bot para evitar duas seguidas
            if (contents.length === 0 || contents[contents.length - 1].role === 'user') {
                 contents.push({ role: 'model', parts: [{ text: interaction.assistant_response }] });
            } else {
                // Caso raro: duas respostas de bot seguidas? Concatena ou ignora.
                console.warn("Skipping consecutive assistant message in history formatting.");
            }
        }
    });
    // Adiciona o prompt atual do usuário
    contents.push({ role: 'user', parts: [{ text: currentPrompt }] });
    return contents;
}

/**
 * Chama a API do Gemini para gerar uma resposta.
 */
async function generateGeminiResponse(apiKey: string, history: GeminiContent[]): Promise<string | null> {
    try {
        const response = await fetch(`${GEMINI_API_ENDPOINT}?key=${apiKey}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                contents: history,
                // Adicionar generationConfig se necessário (temperature, maxOutputTokens, etc.)
                // "generationConfig": {
                //   "temperature": 0.7,
                //   "maxOutputTokens": 1000
                // }
            }),
        });

        if (!response.ok) {
            const errorBody = await response.text();
            console.error(`Gemini API request failed: ${response.status} ${response.statusText}`, errorBody);
            throw new Error(`Gemini API request failed: ${response.status}`);
        }

        const data = await response.json() as GeminiResponse;

        // Verifica se houve resposta e se não foi bloqueada
        if (data.candidates && data.candidates.length > 0 && data.candidates[0].content) {
             // Tratamento simples pegando o texto da primeira parte do primeiro candidato
             if (data.candidates[0].content.parts && data.candidates[0].content.parts.length > 0){
                return data.candidates[0].content.parts[0].text;
             } else {
                 console.warn("Gemini response candidate has no parts.");
                 return null; // Ou uma resposta padrão de erro
             }
        } else if (data.promptFeedback?.safetyRatings) {
             console.warn("Gemini response blocked due to safety concerns:", data.promptFeedback.safetyRatings);
             return "Desculpe, não posso responder a isso devido às políticas de segurança."; // Mensagem genérica
        } else {
            console.warn("No valid candidates found in Gemini response:", data);
            return null; // Ou uma resposta padrão de erro
        }

    } catch (error) {
        console.error('Error calling Gemini API:', error);
        return null; // Retorna null em caso de erro na chamada
    }
}

console.log(`Function "process-user-command" up and running!`)

serve(async (req) => {
  // Tratamento para requisições OPTIONS (preflight)
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  let userId: string | null = null; // Para usar no catch se o parse falhar

  try {
    // 1. Validar e parsear o corpo da requisição
    if (!req.body) {
        return new Response(JSON.stringify({ error: 'Missing request body' }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 400,
        })
    }
    const payload = await req.json() as RequestPayload;
    if (!payload.prompt || !payload.userId) {
      return new Response(JSON.stringify({ error: 'Missing prompt or userId in request body' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
        });
    }
    const { prompt, userId: payloadUserId } = payload;

    userId = payloadUserId; // Atribui aqui para usar no log/salvamento

    // 2. Criar cliente Supabase
    // As variáveis de ambiente SUPABASE_URL e SUPABASE_ANON_KEY são injetadas automaticamente
    // A SUPABASE_SERVICE_ROLE_KEY é necessária para bypassar RLS, se aplicável
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '', // Usar a ANON key para respeitar RLS
       { global: { headers: { Authorization: req.headers.get('Authorization')! } } } // Repassa o Auth do usuário
    )

    // 3. Salvar a mensagem do usuário
    const userInteraction: Omit<Interaction, 'id' | 'timestamp'> = {
      user_id: userId,
      user_input: prompt,
      assistant_response: null
    };
    const { error: insertUserMsgError } = await supabaseClient.from('interactions').insert(userInteraction);

    if (insertUserMsgError) {
      console.error('Error inserting user message:', insertUserMsgError);
      return new Response(JSON.stringify({ error: 'Failed to save user message', details: insertUserMsgError.message }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500,
      });
    }
    console.log('User message saved:', prompt);

    // 4. Buscar e Formatar Histórico
    const history = await getInteractionHistory(supabaseClient, userId);
    const formattedHistory = formatHistoryForGemini(history, prompt);

    // 5. Gerar Resposta com Gemini
    const botMessageContent = await generateGeminiResponse(Deno.env.get('GEMINI_API_KEY') ?? '', formattedHistory);

    if (!botMessageContent) {
         // Tratar erro da API Gemini - talvez salvar um erro no DB?
         console.error('Failed to generate Gemini response.');
         // Por enquanto, retorna um erro genérico, mas salva a falha
         const errorBotInteraction: Interaction = { user_id: userId, user_input: null, assistant_response: "[Erro ao gerar resposta]" };
         await supabaseClient.from('interactions').insert(errorBotInteraction);

         return new Response(JSON.stringify({ error: 'Failed to get response from AI model' }), {
             headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 500 });
    }
     console.log(`Gemini response generated for ${userId}: "${botMessageContent.substring(0, 50)}..."`);

    // 6. Salvar a resposta do bot
    const botInteraction: Omit<Interaction, 'id' | 'timestamp'> = {
      user_id: userId,
      user_input: null,
      assistant_response: botMessageContent
    };
    const { error: insertBotMsgError } = await supabaseClient.from('interactions').insert(botInteraction);
    if (insertBotMsgError) {
        // Logar o erro, mas retornar a resposta ao usuário mesmo assim é provavelmente o melhor UX
        console.error('Error inserting bot response:', insertBotMsgError);
    } else {
         console.log(`Bot response saved for ${userId}.`);
    }

    // 7. Retornar a resposta do bot para o app
    return new Response(JSON.stringify({ response: botMessageContent }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })
  } catch (error) {
    console.error('Internal Server Error:', error);
     // Tenta salvar uma interação de erro se tivermos o userId
     if (userId && error instanceof Error) {
         try {
             // Cria um cliente Supabase temporário com service_role se precisar bypassar RLS para salvar erro
             const serviceClient = createClient(
                  Deno.env.get('SUPABASE_URL') ?? '',
                  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''); // Usa Service Role para garantir log
             const errorBotInteraction: Interaction = {
                  user_id: userId,
                  user_input: null,
                  assistant_response: `[Erro interno no servidor: ${error.message}]`
             };
             await serviceClient.from('interactions').insert(errorBotInteraction);
             console.log(`Server error logged to interactions for user ${userId}`);
         } catch (dbError) {
             console.error("Failed to log server error to database:", dbError);
         }
     }
    return new Response(JSON.stringify({ error: 'Internal Server Error' }), { // Mensagem genérica para o cliente
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    });
  }
})
