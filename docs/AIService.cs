using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using JARVIS.Models;
using Microsoft.Extensions.Logging;

namespace JARVIS.Services;

public class AIService
{
    private readonly string _apiKey;
    private readonly ILogger<AIService> _logger;
    private readonly HttpClient _httpClient;
    private readonly VersionService _versionService;
    private readonly DatabaseService _databaseService;

    public AIService(ILogger<AIService> logger, HttpClient httpClient, VersionService versionService, DatabaseService databaseService)
    {
        _logger = logger;
        _httpClient = httpClient;
        _versionService = versionService;
        _databaseService = databaseService;
        _apiKey = Environment.GetEnvironmentVariable("OPENAI_API_KEY") ?? throw new InvalidOperationException("API Key não encontrada");
    }

    public async Task<string> ProcessUserInputAsync(string input)
    {
        // Salvar mensagem do usuário no banco de dados
        var userMessage = new Message(input, true);
        await _databaseService.SaveMessageAsync(userMessage);
        
        // Analisar o perfil de personalidade do usuário
        var userProfile = await _databaseService.GetPersonalityProfileAsync();
        var updatedProfile = PersonalityAnalyzer.AnalyzeMessage(input, userProfile);
        await _databaseService.SavePersonalityProfileAsync(updatedProfile);
        
        // Verificar comandos de versão
        if (input.ToLower().Contains("versão funcionou"))
        {
            _versionService.IncrementMinor();
        }
        else if (input.ToLower().Contains("versão final"))
        {
            _versionService.IncrementMajor();
        }

        // Gerar instruções personalizadas com base no perfil
        string personalizedInstructions = PersonalityAnalyzer.GeneratePersonalizedInstructions(updatedProfile);
        
        // Gerar uma pergunta reflexiva se apropriado
        string followUpQuestion = "";
        if (updatedProfile.AnalysisCount > 3 && 
            (updatedProfile.HasSarcasm || updatedProfile.EmotionalAmbivalence || updatedProfile.CommunicationComplexity > 0.6f))
        {
            // Remover criação manual antiga:
            // var kernelService = new MicrosoftSemanticKernelService(_apiKey, _versionService, _databaseService);
            // followUpQuestion = kernelService.GenerateFollowUpQuestion(updatedProfile);
        }
        
        // Preparar o prompt com as instruções personalizadas
        string systemPrompt = "Você é um assistente de IA sofisticado que adapta seu estilo de comunicação ao usuário. ";
        if (!string.IsNullOrEmpty(personalizedInstructions))
        {
            systemPrompt += personalizedInstructions;
        }

        // Formato atualizado para Gemini 2.5 Pro com SystemInstruction e Content.Parts
        var request = new
        {
            contents = new[] 
            {
                new { 
                    role = "user",
                    parts = new[] { new { text = input } }
                }
            },
            systemInstruction = new { text = systemPrompt },
            generationConfig = new {
                maxOutputTokens = 8192,
                temperature = 0.7,
                topK = 40,
                topP = 0.95
            },
            safetySettings = new[] {
                new { category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_MEDIUM_AND_ABOVE" },
                new { category = "HARM_CATEGORY_HATE_SPEECH", threshold = "BLOCK_MEDIUM_AND_ABOVE" },
                new { category = "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold = "BLOCK_MEDIUM_AND_ABOVE" },
                new { category = "HARM_CATEGORY_DANGEROUS_CONTENT", threshold = "BLOCK_MEDIUM_AND_ABOVE" }
            }
        };

        var json = JsonConvert.SerializeObject(request);
        var content = new StringContent(json, Encoding.UTF8, "application/json");
        
        // Adicionar cabeçalho de API Key conforme especificação
        _httpClient.DefaultRequestHeaders.Clear();
        _httpClient.DefaultRequestHeaders.Add("X-Goog-Api-Key", _apiKey);
        _httpClient.DefaultRequestHeaders.Add("Content-Type", "application/json");

        var response = await _httpClient.PostAsync("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent", content);
        response.EnsureSuccessStatusCode();

        var responseJson = await response.Content.ReadAsStringAsync();
        dynamic data = JsonConvert.DeserializeObject(responseJson);
        
        // Processamento atualizado para o formato de resposta do Gemini 2.5 Pro
        string responseText = data.candidates[0].content.parts[0].text;
        
        // Adicionar pergunta reflexiva se disponível
        if (!string.IsNullOrEmpty(followUpQuestion))
        {
            responseText += "\n\n" + followUpQuestion;
        }
        
        // Salvar resposta da IA no banco de dados
        var aiMessage = new Message(responseText, false);
        await _databaseService.SaveMessageAsync(aiMessage);
        
        return responseText;
    }

    public async Task<string> AnalyzeUserPersonalityAsync(string input)
    {
        try
        {
            var currentProfile = await _databaseService.GetPersonalityProfileAsync() ?? new PersonalityAnalyzer.CommunicationProfile();
            var updatedProfile = PersonalityAnalyzer.AnalyzeMessage(input, currentProfile);
            await _databaseService.SavePersonalityProfileAsync(updatedProfile);

            // Comentar a geração da pergunta de acompanhamento por enquanto
            // string followUpQuestion = string.Empty;
            // if (_kernelService != null) // Verificar se IKernelService foi injetado
            // {
            //     // Idealmente, chamar um método em IKernelService para isso
            //     // Ex: followUpQuestion = await _kernelService.GenerateFollowUpQuestionAsync(updatedProfile);
            // }
            // else {
                 // Remover criação manual antiga:
                 // var kernelService = new MicrosoftSemanticKernelService(_apiKey, _versionService, _databaseService);
                 // followUpQuestion = kernelService.GenerateFollowUpQuestion(updatedProfile);
            // }

            _logger.LogInformation("Perfil de personalidade atualizado.");
            // return followUpQuestion; // Retornar string vazia por enquanto
            return string.Empty;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Erro ao analisar personalidade do usuário.");
            return "Ocorreu um erro ao analisar sua mensagem.";
        }
    }
}