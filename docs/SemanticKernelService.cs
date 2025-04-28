using System;
using System.Threading.Tasks;
using Microsoft.SemanticKernel;
using Microsoft.SemanticKernel.Connectors.Google;
using Microsoft.SemanticKernel.Memory;
using Microsoft.SemanticKernel.ChatCompletion;
using Microsoft.SemanticKernel.Connectors.OpenAI;
using JARVIS.Models;
using JARVIS.Services.Plugins;
using System.Text;
using System.Linq;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;

namespace JARVIS.Services
{
    /// <summary>
    /// Implementa a interação com a IA usando o Microsoft Semantic Kernel.
    /// </summary>
    [SuppressMessage("Semantics", "SKEXP0001:Type is for evaluation purposes only", Justification = "Uso consciente de API experimental")]
    public class SemanticKernelService : IKernelService
    {
        [SuppressMessage("Semantics", "SKEXP0001:Type is for evaluation purposes only", Justification = "Uso consciente de API experimental")]
        private readonly ISemanticTextMemory _memory;
        
        private readonly Kernel _kernel;
        
        private readonly DatabaseService _databaseService;
        private readonly PromptService _promptService;
        private readonly string _geminiApiKey;
        private readonly string _geminiModelId;
        private readonly IChatCompletionService _chatCompletionService;
        private readonly ChatHistory _chatHistory;
        private const int MaxHistoryMessages = 20; // Definir um limite máximo razoável
        private readonly IMemoryStore _memoryStore = new VolatileMemoryStore();

        // TODO: Injetar outras dependências necessárias (PromptService)
        // Nota: PersonalityAnalysisService não é mais necessário aqui diretamente

        public SemanticKernelService(DatabaseService databaseService, PromptService promptService, string geminiApiKey, string geminiModelId = "gemini-1.5-pro-latest")
        {
            ArgumentNullException.ThrowIfNull(databaseService);
            ArgumentNullException.ThrowIfNull(promptService);
            ArgumentNullException.ThrowIfNull(geminiApiKey);
            ArgumentNullException.ThrowIfNull(geminiModelId);

            _databaseService = databaseService;
            _promptService = promptService;
            _geminiApiKey = geminiApiKey;
            _geminiModelId = geminiModelId;

            if (string.IsNullOrEmpty(_geminiApiKey) || _geminiApiKey.Contains("SUA_CHAVE_API_AQUI") || _geminiApiKey.Contains("YOUR_ACTUAL"))
            {
                throw new ArgumentException("Chave API do Gemini não configurada.", nameof(geminiApiKey));
            }

            var kernelBuilder = Kernel.CreateBuilder();

            kernelBuilder.AddGoogleAIGeminiChatCompletion(
                modelId: _geminiModelId,
                apiKey: _geminiApiKey
            );

            var memoryBuilder = new MemoryBuilder();
            memoryBuilder.WithMemoryStore(new VolatileMemoryStore());
            _memory = memoryBuilder.Build();

            // Registrar Plugins passando PromptService
            var personalityPlugin = new PersonalityPlugin(_databaseService, _promptService);
            kernelBuilder.Plugins.AddFromObject(personalityPlugin, nameof(PersonalityPlugin));

            _kernel = kernelBuilder.Build();

            _chatCompletionService = _kernel.GetRequiredService<IChatCompletionService>();

            _chatHistory = new ChatHistory();
            _chatHistory.AddSystemMessage("Você é JARVIS, um assistente de IA avançado que utiliza o modelo Gemini 1.5 Pro. Você deve ser prestativo, informativo e adaptar seu estilo de comunicação ao usuário. Sempre forneça respostas precisas e úteis, mantendo um tom amigável e profissional.");

            // Carregar histórico do banco de dados
            try
            {
                LoadChatHistoryFromDbAsync().ConfigureAwait(false).GetAwaiter().GetResult();
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erro ao carregar histórico do DB na inicialização: {ex.Message}");
                // Continuar mesmo se o histórico não puder ser carregado
            }

            Console.WriteLine("Semantic Kernel Service Initialized with PersonalityPlugin.");
        }

        public async Task<string> ProcessUserInputAsync(string userInput)
        {
            ChatMessage? profileContextMessage = null;
            try
            {
                // Gerenciamento do Tamanho do Histórico
                // Manter a mensagem do sistema (índice 0) e as N últimas mensagens
                if (_chatHistory.Count > MaxHistoryMessages)
                {
                    int removeCount = _chatHistory.Count - MaxHistoryMessages;
                    // Remover mensagens do usuário/assistente mais antigas (começando do índice 1)
                    _chatHistory.RemoveRange(1, removeCount);
                    Console.WriteLine($"[History] Histórico truncado. {removeCount} mensagens removidas.");
                }

                var userMessage = new Message(userInput, true);
                await _databaseService.SaveMessageAsync(userMessage);

                _chatHistory.AddUserMessage(userInput);

                // Invocar a análise de personalidade e obter perfil
                PersonalityAnalyzer.CommunicationProfile? userProfile = null;
                try 
                {
                    var arguments = new KernelArguments { ["userInput"] = userInput };
                    var analysisResult = await _kernel.InvokeAsync<PersonalityAnalyzer.CommunicationProfile>(
                        nameof(PersonalityPlugin), "AnalyzeAndSaveProfileAsync", arguments);
                    userProfile = analysisResult.GetValue<PersonalityAnalyzer.CommunicationProfile>();
                }
                catch (Exception pluginEx)
                {
                    Console.WriteLine($"Erro ao invocar PersonalityPlugin.AnalyzeAndSaveProfileAsync: {pluginEx.Message}");
                }

                // Adicionar contexto do perfil ao histórico ANTES de chamar a IA
                if (userProfile != null)
                {
                    string profileSummary = FormatProfileSummary(userProfile);
                    if (!string.IsNullOrWhiteSpace(profileSummary))
                    {
                        // Adiciona como mensagem do sistema para contexto imediato
                        _chatHistory.AddSystemMessage($"[Contexto Interno] {profileSummary}");
                        Console.WriteLine($"[Context] Adicionado ao histórico: {profileSummary}");
                    }
                }

                // Obter resposta principal
                var responseMessage = await _chatCompletionService.GetChatMessageContentAsync(_chatHistory, kernel: _kernel);
                string response = responseMessage.Content ?? "Desculpe, não consegui processar sua solicitação.";

                // Remover contexto do perfil do histórico DEPOIS de obter a resposta
                if (profileContextMessage != null)
                {
                    _chatHistory.Remove(profileContextMessage);
                    Console.WriteLine("[Context] Removido do histórico.");
                }

                // Adicionar resposta da IA ao histórico persistente
                _chatHistory.AddAssistantMessage(response);
                var aiMessage = new Message(response, false);
                await _databaseService.SaveMessageAsync(aiMessage); // Salva a resposta principal

                // Gerar e anexar pergunta de acompanhamento, se houver perfil
                string followUpQuestion = string.Empty;
                if (userProfile != null)
                {
                   try
                    {
                        var followUpArgs = new KernelArguments { ["profile"] = userProfile };
                        var followUpResult = await _kernel.InvokeAsync<string>(
                            nameof(PersonalityPlugin), "GenerateFollowUpQuestion", followUpArgs);
                        followUpQuestion = followUpResult.GetValue<string>() ?? string.Empty;

                        if (!string.IsNullOrWhiteSpace(followUpQuestion))
                        {
                            response += "\n\n" + followUpQuestion; // Anexa a pergunta
                        }
                    }
                    catch (Exception pluginEx)
                    {
                        Console.WriteLine($"Erro ao invocar PersonalityPlugin.GenerateFollowUpQuestion: {pluginEx.Message}");
                    }
                }

                return response; // Retorna resposta principal + pergunta de acompanhamento
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Erro no SemanticKernelService: {ex}");
                 // Remover contexto do perfil do histórico em caso de erro também
                if (profileContextMessage != null && _chatHistory.Contains(profileContextMessage))
                {
                    _chatHistory.Remove(profileContextMessage);
                    Console.WriteLine("[Context] Removido do histórico após erro.");
                }
                _chatHistory.AddAssistantMessage("Desculpe, ocorreu um erro interno ao processar sua solicitação.");
                return "Ocorreu um erro ao processar sua solicitação.";
            }
        }

        // Método auxiliar para formatar o resumo do perfil
        private string FormatProfileSummary(PersonalityAnalyzer.CommunicationProfile profile)
        {
            var summary = new StringBuilder();
            summary.Append($"Estilo: {profile.CommunicationStyle}. ");
            summary.Append($"Formalidade: {profile.FormalityLevel:F2}. ");
            summary.Append($"Complexidade: {profile.CommunicationComplexity:F2}. ");
            if (profile.PreferredTopics != null && profile.PreferredTopics.Any())
            {
                 summary.Append($"Tópicos: {string.Join(", ", profile.PreferredTopics.Take(3))}. ");
            }
            // Adicione outras características relevantes se necessário
            return summary.ToString().Trim();
        }

        private async Task LoadChatHistoryFromDbAsync()
        {
            Console.WriteLine("Carregando histórico do chat do banco de dados...");
            List<Message> messages = await _databaseService.GetMessagesAsync(); // Assumindo que este método existe e retorna List<Message>
            
            if (messages != null && messages.Any())
            {
                // Ordenar por timestamp para garantir a ordem correta
                messages = messages.OrderBy(m => m.Timestamp).ToList();

                // Limpar histórico atual (exceto mensagem do sistema) antes de adicionar?
                // Se não limpar, pode duplicar. Se limpar, perde a mensagem do sistema se ela não estiver no DB.
                // Por segurança, vamos manter a mensagem do sistema e adicionar as outras.
                // _chatHistory.RemoveRange(1, _chatHistory.Count - 1); // Cuidado com isso

                foreach (var message in messages)
                {
                    if (message.IsUserMessage)
                    {
                        _chatHistory.AddUserMessage(message.Text);
                    }
                    else
                    {
                        _chatHistory.AddAssistantMessage(message.Text);
                    }
                }
                Console.WriteLine($"{messages.Count} mensagens carregadas do histórico.");
            }
            else
            {
                Console.WriteLine("Nenhuma mensagem encontrada no histórico do banco de dados.");
            }
        }

        public void ClearMemory()
        {
            // Limpa o histórico de chat na memória,
            // mantendo a mensagem do sistema inicial.
            var systemMessage = _chatHistory.FirstOrDefault(m => m.Role == ChatRole.System);
            _chatHistory.Clear();
            if (systemMessage != null)
            {
                _chatHistory.Add(systemMessage);
            }
            Console.WriteLine("[History] Memória do chat limpa.");

            // TODO: Considerar se a memória semântica (_memory) também deve ser limpa.
            //       Isso dependeria do caso de uso.
        }

        public List<Message> GetConversationHistory()
        {
            // Converte o ChatHistory (que usa AuthorRole) para List<Message> (que usa bool IsUserMessage)
            var historyMessages = new List<Message>();
            // Pular a mensagem do sistema
            foreach (var message in _chatHistory.Skip(1)) 
            {
                historyMessages.Add(new Message
                {
                    Text = message.Content ?? string.Empty,
                    IsUserMessage = message.Role == ChatRole.User,
                    // Timestamp precisaria ser recuperado do DB ou armazenado no ChatHistory se necessário
                    Timestamp = DateTime.UtcNow 
                });
            }
            return historyMessages;
        }

        private async Task<string> GenerateResponseAsync(string userInput, PersonalityAnalyzer.CommunicationProfile profile)
        {
            var kernel = Kernel.CreateBuilder()
                .AddOpenAIChatCompletion(
                    modelId: "gpt-4",
                    apiKey: _geminiApiKey)
                .Build();

            var messages = new List<ChatMessageContent>
            {
                new ChatMessageContent(AuthorRole.System, GetSystemPrompt(profile)),
                new ChatMessageContent(AuthorRole.User, userInput)
            };

            var chatCompletionService = kernel.GetRequiredService<IChatCompletionService>();
            var response = await chatCompletionService.GetChatMessageContentAsync(messages);
            return response.Content;
        }

        // TODO: Reintegrar Geração de Pergunta de Acompanhamento
    }
} 