-- Tabela: user_profiles
-- Armazena informações adicionais do perfil do usuário, vinculadas ao usuário autenticado.

CREATE TABLE IF NOT EXISTS public.user_profiles (
    id uuid PRIMARY KEY DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE, -- Chave primária vinculada ao user_id da autenticação
    updated_at timestamp with time zone,
    username text UNIQUE,
    full_name text,
    avatar_url text,
    -- Adicione outras colunas de perfil conforme necessário (e.g., tier, preferences)

    CONSTRAINT username_length CHECK (char_length(username) >= 3)
);

-- Habilitar Row Level Security (RLS) para user_profiles
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;

-- Políticas RLS para user_profiles:
-- 1. Usuários podem ler seu próprio perfil.
CREATE POLICY "Allow individual user select access"
ON public.user_profiles
FOR SELECT
USING (auth.uid() = id);

-- 2. Usuários podem inserir seu próprio perfil (geralmente feito via trigger ou função após signup).
CREATE POLICY "Allow individual user insert access"
ON public.user_profiles
FOR INSERT
WITH CHECK (auth.uid() = id);

-- 3. Usuários podem atualizar seu próprio perfil.
CREATE POLICY "Allow individual user update access"
ON public.user_profiles
FOR UPDATE
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- (Opcional) Permitir leitura pública de usernames e avatares, se necessário para alguma funcionalidade.
-- CREATE POLICY "Allow public read access for username/avatar"
-- ON public.user_profiles
-- FOR SELECT
-- USING (true); -- Ajuste conforme necessário

-- Tabela: interactions
-- Armazena o histórico de conversas, vinculadas ao usuário.

CREATE TABLE IF NOT EXISTS public.interactions (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY, -- Chave primária sequencial para a tabela
    user_id uuid NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE, -- Chave estrangeira para o usuário dono
    user_input text, -- Texto da mensagem do usuário (pode ser null se a entrada foi só voz)
    assistant_response text, -- Texto da resposta do assistente
    timestamp timestamp with time zone DEFAULT timezone('utc'::text, now()) NOT NULL, -- Timestamp da interação
    -- Colunas adicionais planejadas (podem ser adicionadas depois):
    -- user_audio_ref text, -- Referência ao áudio do usuário no Storage
    -- assistant_audio_ref text, -- Referência ao áudio do assistente no Storage
    -- embedding vector(384), -- Embedding para RAG (ajuste a dimensão conforme o modelo)
    -- metadata jsonb -- Para informações extras (e.g., score de feedback, intenção)
    feedback_rating smallint -- Ex: 1 para upvote, -1 para downvote, 0 ou NULL para sem voto
);

-- Criar um índice na coluna user_id para otimizar buscas por usuário
CREATE INDEX IF NOT EXISTS idx_interactions_user_id ON public.interactions(user_id);

-- Criar um índice na coluna timestamp para otimizar ordenação
CREATE INDEX IF NOT EXISTS idx_interactions_timestamp ON public.interactions(timestamp DESC);

-- (Opcional, se for usar busca vetorial na Fase 3+) Habilitar extensão pgvector (FAÇA ISSO UMA VEZ NO EDITOR SQL)
-- CREATE EXTENSION IF NOT EXISTS vector;
-- Criar um índice vetorial (exemplo IVFFlat, ajuste parâmetros conforme seu volume de dados)
-- CREATE INDEX IF NOT EXISTS idx_interactions_embedding ON public.interactions USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100); -- Ajuste 'lists'

-- Habilitar Row Level Security (RLS) para interactions
ALTER TABLE public.interactions ENABLE ROW LEVEL SECURITY;

-- Políticas RLS para interactions:
-- 1. Usuários podem ler SUAS PRÓPRIAS interações.
CREATE POLICY "Allow individual user select access"
ON public.interactions
FOR SELECT
USING (auth.uid() = user_id);

-- 2. Usuários podem inserir interações PARA SI MESMOS.
CREATE POLICY "Allow individual user insert access"
ON public.interactions
FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- 3. Usuários podem atualizar o feedback (rating) de SUAS PRÓPRIAS interações.
CREATE POLICY "Allow individual user update feedback access"
ON public.interactions
FOR UPDATE
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id AND id = id); -- Permite atualizar apenas colunas como feedback_rating

-- 4. (Opcional) Permitir que usuários deletem SUAS PRÓPRIAS interações. Considere as implicações.
-- CREATE POLICY "Allow individual user delete access"
-- ON public.interactions
-- FOR DELETE
-- USING (auth.uid() = user_id);

-- Garante que administradores/serviços de backend (com role apropriada) possam acessar tudo (se necessário)
-- CREATE POLICY "Allow full access for admins"
-- ON public.interactions
-- FOR ALL
-- USING (check_if_user_is_admin()); -- Substitua por sua lógica de verificação de admin/role

-- Garante que administradores/serviços de backend (com role apropriada) possam acessar tudo (se necessário)
-- CREATE POLICY "Allow full access for admins on profiles"
-- ON public.user_profiles
-- FOR ALL
-- USING (check_if_user_is_admin()); -- Substitua por sua lógica de verificação de admin/role
