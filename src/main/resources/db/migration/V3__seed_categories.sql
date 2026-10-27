-- Categorias padrao para o usuario semente (id = 1).
-- NORMAL = categorias comuns; RESERVA = linha especial de reserva.
INSERT INTO category (user_id, name, kind) VALUES
    (1, 'Salario',     'NORMAL'),
    (1, 'Mercado',     'NORMAL'),
    (1, 'Casa',        'NORMAL'),
    (1, 'Carro',       'NORMAL'),
    (1, 'Lazer',       'NORMAL'),
    (1, 'Jogos',       'NORMAL'),
    (1, 'Saude',       'NORMAL'),
    (1, 'Educacao',    'NORMAL'),
    (1, 'Emprestimos', 'NORMAL'),
    (1, 'Outros',      'NORMAL'),
    (1, 'Reserva',     'RESERVA');
