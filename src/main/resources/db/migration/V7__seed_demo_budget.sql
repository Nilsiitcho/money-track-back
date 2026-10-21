-- Seed demonstrativo: orcamento realista de ~3 anos (2026-2028) para o usuario 1.
-- Inclui receitas, despesas fixas, parceladas, unicas e uma categoria de Emprestimos.
-- Aditivo (nao remove dados existentes). Referencia categorias por nome.

-- Categoria especial de Emprestimos (se ainda nao existir).
INSERT INTO category (user_id, name, kind)
SELECT 1, 'Emprestimos', 'NORMAL'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE user_id = 1 AND lower(name) = 'emprestimos'
);

-- Lancamentos. Colunas de v: categoria, tipo, descricao, valor, recorrencia,
-- mes inicial, mes final, parcela inicial, total de parcelas.
INSERT INTO entry (user_id, category_id, type, description, amount, recurrence,
                   start_month, end_month, installment_first, installment_total, updated_at)
SELECT 1, c.id, v.type, v.descr, v.amount::numeric(12,2), v.rec,
       v.start::date, v.endm::date, v.ifirst::int, v.itotal::int, now()
FROM (
    VALUES
        -- ===== Receitas =====
        ('salario',  'RECEITA', 'Salario mensal',   6500, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('salario',  'RECEITA', '13o salario',      6500, 'UNICO',     '2026-12-01', '2026-12-01', NULL, NULL),
        ('salario',  'RECEITA', '13o salario',      6700, 'UNICO',     '2027-12-01', '2027-12-01', NULL, NULL),
        ('salario',  'RECEITA', '13o salario',      6900, 'UNICO',     '2028-12-01', '2028-12-01', NULL, NULL),
        ('outros',   'RECEITA', 'Freelance',        2000, 'UNICO',     '2026-04-01', '2026-04-01', NULL, NULL),
        ('outros',   'RECEITA', 'Freelance',        2500, 'UNICO',     '2026-09-01', '2026-09-01', NULL, NULL),
        ('outros',   'RECEITA', 'Freelance',        2200, 'UNICO',     '2027-03-01', '2027-03-01', NULL, NULL),

        -- ===== Despesas fixas =====
        ('casa',     'DESPESA', 'Aluguel',          1800, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('mercado',  'DESPESA', 'Mercado',          1300, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('casa',     'DESPESA', 'Energia',           280, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('casa',     'DESPESA', 'Internet e telefone', 150, 'FIXO',    '2026-01-01', NULL,         NULL, NULL),
        ('carro',    'DESPESA', 'Combustivel',       450, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('saude',    'DESPESA', 'Plano de saude',    620, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('saude',    'DESPESA', 'Academia',          120, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('lazer',    'DESPESA', 'Streaming',          60, 'FIXO',      '2026-01-01', NULL,         NULL, NULL),
        ('lazer',    'DESPESA', 'Restaurantes e lazer', 500, 'FIXO',   '2026-01-01', NULL,         NULL, NULL),

        -- ===== Despesas parceladas =====
        ('educacao', 'DESPESA', 'Notebook 10x',      450, 'PARCELADO', '2026-03-01', '2026-12-01', 1, 10),
        ('lazer',    'DESPESA', 'Viagem ferias 12x', 700, 'PARCELADO', '2026-06-01', '2027-05-01', 1, 12),
        ('casa',     'DESPESA', 'Geladeira 6x',      500, 'PARCELADO', '2027-02-01', '2027-07-01', 1, 6),
        ('outros',   'DESPESA', 'Celular 12x',       350, 'PARCELADO', '2027-01-01', '2027-12-01', 1, 12),

        -- ===== Emprestimos (parcelados) =====
        ('emprestimos', 'DESPESA', 'Emprestimo pessoal 36x',    900, 'PARCELADO', '2026-01-01', '2028-12-01', 1, 36),
        ('emprestimos', 'DESPESA', 'Consignado 24x',            650, 'PARCELADO', '2026-03-01', '2028-02-01', 1, 24),
        ('emprestimos', 'DESPESA', 'Financiamento carro 48x',   780, 'PARCELADO', '2026-01-01', '2029-12-01', 1, 48),

        -- ===== Despesas unicas (anuais) =====
        ('carro',    'DESPESA', 'IPVA',             2200, 'UNICO',     '2026-01-01', '2026-01-01', NULL, NULL),
        ('carro',    'DESPESA', 'IPVA',             2300, 'UNICO',     '2027-01-01', '2027-01-01', NULL, NULL),
        ('carro',    'DESPESA', 'IPVA',             2400, 'UNICO',     '2028-01-01', '2028-01-01', NULL, NULL),
        ('carro',    'DESPESA', 'Seguro do carro',  1600, 'UNICO',     '2026-03-01', '2026-03-01', NULL, NULL),
        ('carro',    'DESPESA', 'Seguro do carro',  1650, 'UNICO',     '2027-03-01', '2027-03-01', NULL, NULL),
        ('educacao', 'DESPESA', 'Material escolar',  800, 'UNICO',     '2026-02-01', '2026-02-01', NULL, NULL),
        ('educacao', 'DESPESA', 'Material escolar',  850, 'UNICO',     '2027-02-01', '2027-02-01', NULL, NULL),
        ('lazer',    'DESPESA', 'Presentes de Natal', 1500, 'UNICO',   '2026-12-01', '2026-12-01', NULL, NULL),
        ('lazer',    'DESPESA', 'Presentes de Natal', 1600, 'UNICO',   '2027-12-01', '2027-12-01', NULL, NULL)
) AS v(cat, type, descr, amount, rec, start, endm, ifirst, itotal)
JOIN category c ON c.user_id = 1 AND lower(c.name) = v.cat;
