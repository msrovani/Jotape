using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace JARVIS.Services
{
    /// <summary>
    /// Serviço responsável por gerenciar prompts dinâmicos para a IA em diferentes idiomas
    /// </summary>
    public class PromptService
    {
        private readonly LocalizationService _localizationService;
        private readonly ICacheService _cacheService;
        private readonly Dictionary<string, Dictionary<string, string>> _prompts;
        
        private const string CACHE_KEY = "PromptCache";
        
        public PromptService(LocalizationService localizationService, ICacheService cacheService)
        {
            _localizationService = localizationService;
            _cacheService = cacheService;
            _prompts = new Dictionary<string, Dictionary<string, string>>();
            
            // Registrar para eventos de mudança de idioma
            _localizationService.LanguageChanged += OnLanguageChanged;
            
            // Inicializar prompts
            InitializePromptsAsync().ConfigureAwait(false);
        }
        
        private async Task InitializePromptsAsync()
        {
            // Tentar carregar do cache primeiro
            if (await _cacheService.TryGetValueAsync<Dictionary<string, Dictionary<string, string>>>(CACHE_KEY, out var cachedPrompts) && 
                cachedPrompts != null)
            {
                _prompts = cachedPrompts;
                return;
            }
            
            // Definir prompts padrão em português
            var ptBR = new Dictionary<string, string>
            {
                { "SystemPrompt", "Você é um assistente de IA sofisticado que adapta seu estilo de comunicação ao usuário. Seja útil, preciso e amigável." },
                { "FollowUpQuestion", "Gostaria de saber mais sobre algum aspecto específico deste assunto?" },
                { "PersonalityAnalysis", "Analisando seu estilo de comunicação para oferecer uma experiência mais personalizada." },
                { "WelcomeMessage", "Olá! Sou o JARVIS, seu assistente pessoal. Como posso ajudar hoje?" }
            };
            
            // Definir prompts em inglês
            var enUS = new Dictionary<string, string>
            {
                { "SystemPrompt", "You are a sophisticated AI assistant that adapts your communication style to the user. Be helpful, accurate, and friendly." },
                { "FollowUpQuestion", "Would you like to know more about any specific aspect of this topic?" },
                { "PersonalityAnalysis", "Analyzing your communication style to offer a more personalized experience." },
                { "WelcomeMessage", "Hello! I'm JARVIS, your personal assistant. How can I help you today?" }
            };
            
            // Definir prompts em espanhol
            var esES = new Dictionary<string, string>
            {
                { "SystemPrompt", "Eres un sofisticado asistente de IA que adapta su estilo de comunicación al usuario. Sé útil, preciso y amigable." },
                { "FollowUpQuestion", "¿Te gustaría saber más sobre algún aspecto específico de este tema?" },
                { "PersonalityAnalysis", "Analizando tu estilo de comunicación para ofrecer una experiencia más personalizada." },
                { "WelcomeMessage", "¡Hola! Soy JARVIS, tu asistente personal. ¿Cómo puedo ayudarte hoy?" }
            };
            
            // Adicionar todos os idiomas ao dicionário principal
            _prompts.Add("pt-BR", ptBR);
            _prompts.Add("en-US", enUS);
            _prompts.Add("es-ES", esES);
            
            // Salvar no cache
            await _cacheService.SetValueAsync(CACHE_KEY, _prompts);
        }
        
        /// <summary>
        /// Obtém um prompt específico no idioma atual
        /// </summary>
        /// <param name="promptKey">A chave do prompt desejado</param>
        /// <returns>O texto do prompt no idioma atual</returns>
        public string GetPrompt(string promptKey)
        {
            string currentLanguage = _localizationService.CurrentLanguage;
            
            // Verificar se o idioma existe
            if (!_prompts.ContainsKey(currentLanguage))
            {
                // Fallback para inglês ou português
                currentLanguage = _prompts.ContainsKey("en-US") ? "en-US" : "pt-BR";
            }
            
            // Verificar se o prompt existe para o idioma
            if (_prompts[currentLanguage].ContainsKey(promptKey))
            {
                return _prompts[currentLanguage][promptKey];
            }
            
            // Retornar uma string vazia se o prompt não for encontrado
            return string.Empty;
        }
        
        /// <summary>
        /// Adiciona ou atualiza um prompt para todos os idiomas suportados
        /// </summary>
        /// <param name="promptKey">A chave do prompt</param>
        /// <param name="promptTextPtBR">Texto em português</param>
        /// <param name="promptTextEnUS">Texto em inglês</param>
        /// <param name="promptTextEsES">Texto em espanhol</param>
        public async Task AddOrUpdatePromptAsync(string promptKey, string promptTextPtBR, string promptTextEnUS, string promptTextEsES)
        {
            if (_prompts.ContainsKey("pt-BR"))
            {
                _prompts["pt-BR"][promptKey] = promptTextPtBR;
            }
            
            if (_prompts.ContainsKey("en-US"))
            {
                _prompts["en-US"][promptKey] = promptTextEnUS;
            }
            
            if (_prompts.ContainsKey("es-ES"))
            {
                _prompts["es-ES"][promptKey] = promptTextEsES;
            }
            
            // Atualizar o cache
            await _cacheService.SetValueAsync(CACHE_KEY, _prompts);
        }
        
        /// <summary>
        /// Formata um prompt com parâmetros dinâmicos
        /// </summary>
        /// <param name="promptKey">A chave do prompt</param>
        /// <param name="parameters">Dicionário de parâmetros para substituição</param>
        /// <returns>O prompt formatado</returns>
        public string FormatPrompt(string promptKey, Dictionary<string, string> parameters)
        {
            string prompt = GetPrompt(promptKey);
            
            if (string.IsNullOrEmpty(prompt) || parameters == null)
            {
                return prompt;
            }
            
            // Substituir cada parâmetro no prompt
            foreach (var param in parameters)
            {
                prompt = prompt.Replace($"{{{param.Key}}}", param.Value);
            }
            
            return prompt;
        }
        
        private void OnLanguageChanged()
        {
            // Quando o idioma mudar, podemos realizar ações específicas se necessário
            Console.WriteLine($"Idioma alterado para: {_localizationService.CurrentLanguage}");
        }

        public async Task<(bool exists, T value)> TryGetValueAsync<T>(string key)
        {
            return await _cacheService.TryGetValueAsync<T>(key);
        }
    }
}