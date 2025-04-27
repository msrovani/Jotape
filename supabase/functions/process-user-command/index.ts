// Follow this setup guide to integrate the Deno language server with your editor:
// https://deno.land/manual/getting_started/setup_your_environment
// This enables autocomplete, go to definition, etc.

// Setup type definitions for built-in Supabase Runtime APIs
import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { serve } from "jsr:@std/http/server";
import { corsHeaders } from "../_shared/cors.ts"; // Import CORS headers utility

console.log(`Function "process-user-command" up and running!`);

serve(async (req: Request) => {
  // Handle CORS preflight requests
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    // Ensure it's a POST request
    if (req.method !== "POST") {
      return new Response(JSON.stringify({ error: "Method Not Allowed" }), {
        status: 405,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // --- TODO: JWT Authentication ---
    // const authHeader = req.headers.get("Authorization");
    // if (!authHeader || !authHeader.startsWith("Bearer ")) {
    //   return new Response(JSON.stringify({ error: "Missing or invalid Authorization header" }), {
    //     status: 401,
    //     headers: { ...corsHeaders, "Content-Type": "application/json" },
    //   });
    // }
    // const token = authHeader.split(" ")[1];
    // try {
    //   const { data: { user }, error: userError } = await supabaseAdminClient.auth.getUser(token);
    //   if (userError) throw userError;
    //   console.log("Authenticated user:", user.id);
    // } catch (error) {
    //   console.error("JWT validation error:", error);
    //   return new Response(JSON.stringify({ error: "Invalid JWT" }), {
    //     status: 401,
    //     headers: { ...corsHeaders, "Content-Type": "application/json" },
    //   });
    // }
    // --- End TODO ---

    // Parse the request body
    let userMessageText: string | undefined;
    try {
        const body = await req.json();
        userMessageText = body.text; // Expecting { "text": "..." }
        if (typeof userMessageText !== 'string' || userMessageText.trim() === '') {
            throw new Error("Missing or empty 'text' field in request body");
        }
    } catch (error) {
        console.error("Body parsing error:", error.message);
        return new Response(JSON.stringify({ error: "Invalid request body: " + error.message }), {
            status: 400,
            headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
    }

    console.log(`Received text: "${userMessageText}"`);

    // --- TODO: Actual LLM Call & Processing Logic --- 
    // Replace this mock response with the real logic
    const assistantReply = `Edge Function received: "${userMessageText}". This is a mock reply.`;
    // --- End TODO ---
    
    // Prepare the response data according to ProcessCommandResponse DTO
    const responseData = { reply: assistantReply };

    // Return the successful response
    return new Response(JSON.stringify(responseData), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });

  } catch (error) {
    console.error("Unhandled error:", error);
    return new Response(JSON.stringify({ error: "Internal Server Error" }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 500,
    });
  }
});

/* To invoke locally:

  1. Run `supabase start` (see: https://supabase.com/docs/reference/cli/supabase-start)
  2. Make an HTTP request:

  curl -i --location --request POST 'http://127.0.0.1:54321/functions/v1/process-user-command' \
    --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0' \
    --header 'Content-Type: application/json' \
    --data '{"name":"Functions"}'

*/
