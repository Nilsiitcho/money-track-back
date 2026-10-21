-- Categoria passa a ser opcional no lancamento.
ALTER TABLE entry ALTER COLUMN category_id DROP NOT NULL;
