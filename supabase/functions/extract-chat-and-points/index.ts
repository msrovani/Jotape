// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { serve } from 'https://deno.land/std@0.177.0/http/server.ts'
import { corsHeaders } from '../_shared/cors.ts'
import { GoogleGenerativeAI } from 'npm:@google/generative-ai'; // Import Gemini SDK

console.log("Hello from Functions!")

// Tipagem para o histórico e a resposta esperada da Gemini
interface HistoryEntry {
  role: 'user' | 'model';
  parts: { text: string }[];
}

interface GeminiResponse {
  chatResponse: string;
  attentionPoints: {
    type: string; // e.g., 'PESSOA', 'TAREFA'
    content: string;
    context?: string; // Opcional: trecho da conversa
  }[];
}

// --- Função Principal ---
serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // 1. Obter dados da requisição
    const { sessionId, userMessage, history } = await req.json()
    if (!sessionId || !userMessage || !history) {
      throw new Error("Missing required fields: sessionId, userMessage, history");
    }

    // 2. Obter chave da API Gemini (GUARDADA COMO SEGREDO NO SUPABASE)
    const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY');
    if (!GEMINI_API_KEY) {
      throw new Error('Missing environment variable: GEMINI_API_KEY');
    }

    // 3. Inicializar cliente Gemini
    const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" }); // Ou outro modelo

    // 4. Construir o prompt para Gemini (CRUCIAL!)
    //    Instrua a Gemini a responder E extrair pontos em JSON
    const systemInstruction = `
      Você é Jotape, um assistente prestativo. Responda à última mensagem do usuário considerando o histórico fornecido.
      ALÉM DISSO, analise a MENSAGEM DO USUÁRIO e o CONTEXTO DO HISTÓRICO RECENTE para identificar Pontos de Atenção (tarefas, decisões, pessoas, locais, datas, tópicos).
      RETORNE SUA RESPOSTA EXCLUSIVAMENTE NO SEGUINTE FORMATO JSON VÁLIDO:
      {
        "chatResponse": "Aqui vai a sua resposta para a conversa...",
        "attentionPoints": [
          { "type": "TIPO_DO_PONTO", "content": "Texto do ponto extraído", "context": "Trecho relevante da conversa..." }
          // Adicione mais objetos aqui se encontrar mais pontos. Retorne array vazio [] se nenhum ponto for encontrado.
        ]
      }

      Tipos de pontos de atenção possíveis: TAREFA, DECISAO, PESSOA, LOCAL, DATA, TOPICO, OUTRO. Use o tipo que melhor se encaixar.
      Seja conciso no 'content'. O 'context' ajuda a entender onde o ponto foi mencionado.
      NÃO inclua nenhuma explicação fora do formato JSON. Apenas o JSON.
    `;

    // Formatar histórico para a API da Gemini
    const geminiHistory = history.map((entry: any) => ({
        role: entry.role === 'user' ? 'user' : 'model', // Garante role correto
        parts: [{ text: entry.message }] // Ajuste baseado na estrutura real do seu histórico
    }));

    // Combinar instrução de sistema, histórico e nova mensagem
    const chat = model.startChat({
        history: [
             // Adiciona a instrução como parte do histórico inicial se a API suportar "system instruction" diretamente, senão no primeiro turno.
            // { role: 'user', parts: [{ text: systemInstruction }] }, // Método alternativo se não houver system role
            // { role: 'model', parts: [{ text: "Entendido. Estou pronto." }] },
             ...geminiHistory
        ],
        generationConfig: {
            //maxOutputTokens: 200, // Ajuste conforme necessário
            responseMimeType: "application/json", // Instruindo a saída JSON
        },
        // systemInstruction: systemInstruction, // Use se o SDK suportar
    });

     // Adiciona a instrução de sistema no prompt do usuário se não houver system role nativo
    const prompt = `${systemInstruction}\n\nHistórico:\n${JSON.stringify(history)}\n\nMensagem do Usuário:\n${userMessage}`;

    // 5. Chamar a API Gemini
    console.log("Chamando Gemini API...");
    // const result = await chat.sendMessage(userMessage); // Use se 'startChat' configurado com system instruction
    const result = await model.generateContent(prompt); // Use se não usou 'startChat' ou systemInstruction

    // const response = result.response;
    // const text = response.text(); // Obtém a string JSON

     const response = await result.response;
     const text = response.text(); // Obter a string JSON

    console.log("Resposta Gemini (raw):", text);


    // 6. Parsear a resposta JSON da Gemini
    let geminiData: GeminiResponse;
    try {
      geminiData = JSON.parse(text);
      if (!geminiData.chatResponse || !Array.isArray(geminiData.attentionPoints)) {
         throw new Error("Formato JSON inválido recebido da Gemini.");
      }
    } catch (parseError) {
      console.error("Erro ao parsear JSON da Gemini:", parseError);
      console.error("Resposta recebida:", text);
      throw new Error(`Falha ao processar resposta da IA: ${parseError.message}`);
    }

    console.log("Resposta Gemini (parsed):", geminiData);

    // 7. Retornar APENAS a chatResponse para o App Android
    //    Os attentionPoints serão processados pelo trigger depois
    return new Response(JSON.stringify({ chatResponse: geminiData.chatResponse }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 200,
    })

  } catch (error) {
    console.error('Erro na Edge Function:', error)
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    })
  }
})

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/extract-chat-and-points' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
