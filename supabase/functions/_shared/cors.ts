/**
 * Standard CORS headers for Supabase Edge Functions.
 * Allows requests from any origin during development (adjust for production).
 */
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*", // Allow requests from any origin
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
}; 