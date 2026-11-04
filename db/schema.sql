-- AgentForge 业务库 schema（幂等；与 setup 阶段所建 agenthub 一致）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS agent (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    system_prompt TEXT,
    model TEXT,
    allowed_tools JSONB,
    allowed_kbs JSONB,
    chat_options JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID REFERENCES agent(id) ON DELETE SET NULL,
    title TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role TEXT NOT NULL,
    content TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS knowledge_base (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kb_id UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    filetype TEXT,
    size BIGINT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 分块 + 向量（智谱 embedding-3, 1024 维）
CREATE TABLE IF NOT EXISTS chunk_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kb_id UUID NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    doc_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1024) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chunk_embedding
ON chunk_embedding
USING ivfflat (embedding vector_l2_ops)
WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_chat_message_session
ON chat_message (session_id, created_at);
