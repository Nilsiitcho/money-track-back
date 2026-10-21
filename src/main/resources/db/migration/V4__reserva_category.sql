-- Categoria especial "Reserva de Gastos" (despesa de gastos variaveis do mes).
-- kind=NORMAL para as comuns; RESERVA para a especial.
ALTER TABLE category ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Semente da categoria especial para o usuario 1.
INSERT INTO category (user_id, name, kind) VALUES (1, 'Reserva de Gastos', 'RESERVA');
