# Especificação — Lançamento Rápido (Gasto de Reserva)

> Rascunho para validação **antes de implementar**. Pontos marcados com ⚠️ são
> suposições minhas ou perguntas em aberto — confirme ou corrija.

## 1. Objetivo

Registrar de forma rápida, pelo botão **+**, uma compra paga com o dinheiro da
**Reserva** (gastos variáveis do mês). O lançamento rápido é **sempre DESPESA**.
Ao lançar, o usuário vê o impacto no orçamento (saldo do mês e saldo diário).
Quando os gastos do mês passam da Reserva, o **excedente** aparece
automaticamente na tabela e entra nos totais.

## 2. Decisões confirmadas

1. **Botão +** → só lançamento rápido (DESPESA). Os lançamentos completos
   (receita, fixo, parcelado) passam a ser criados por um botão **"Novo
   lançamento"** dentro da aba **Lançamentos**.
2. **Contabilização** → a Reserva mensal (ex.: 1.200) continua contando como
   despesa fixa. Os lançamentos rápidos **consomem** a Reserva e **não entram um
   a um** nos totais. Se a soma dos rápidos do mês passar da Reserva, o
   **excedente** vira uma linha **"Excedente de Reserva"** e **só o excedente**
   soma aos totais.
3. **Orçamento diário** → pool mensal rebalanceado:
   `saldo diário = (Reserva − gasto do mês) / dias restantes`.

## 3. Modelo de dados

Reaproveita a categoria especial **Reserva** (kind `RESERVA`). Dentro dela
convivem dois tipos de lançamento, diferenciados pela recorrência:

| Papel            | Recorrência | Quantidade | Significado                         |
|------------------|-------------|------------|-------------------------------------|
| Orçamento mensal | `FIXO`      | 1          | Valor da Reserva (aba Reserva)      |
| Gasto rápido     | `UNICO`     | N          | Cada compra lançada pelo botão +    |

- O gasto rápido é um `Entry` DESPESA `UNICO`, `category_id` = categoria Reserva,
  `start_month` = mês da data informada.
- ⚠️ Campos do rápido: **valor** (obrigatório, > 0), **data** (default hoje,
  editável), **descrição** (opcional — default "Gasto rápido"). Sem escolher
  tipo/categoria/recorrência (tudo implícito).
- ⚠️ Os rápidos aparecem na aba **Lançamentos** (podem ser editados/excluídos lá);
  na **Tabela** eles **não** viram linhas individuais (ver §5).

Nenhuma migração nova de schema é necessária (reusa `entry` + categoria RESERVA).

## 4. Regras de cálculo

Para um mês com Reserva `R` e dias `D`:

- `S` = soma dos gastos rápidos do mês (todos os `UNICO` da categoria Reserva no mês).
- `Excedente E = max(0, S − R)`.
- Linha **Reserva** (tabela) = `R` (inalterada, conta nos totais).
- Linha **Excedente de Reserva** = `E`, **só aparece quando E > 0**, conta nos totais.
- Total despesas do mês = (demais despesas) + `R` + `E`. Os rápidos crus não somam.
  - Equivale a dizer: a despesa ligada à Reserva no mês = `max(R, S)`.

### Orçamento diário (painel do lançamento rápido)

Baseado na **data** do lançamento. Seja `dia` o dia do mês da data:

- `Dr` (dias restantes) = `D − dia + 1`  (inclui o próprio dia). ⚠️
- `S_antes` = soma dos rápidos já lançados no mês (antes deste). ⚠️
- Antes de adicionar:
  - Restante do mês = `R − S_antes`
  - Saldo diário disponível = `(R − S_antes) / Dr`
- Lançando o valor `V`:
  - Restante do mês (depois) = `R − S_antes − V`
  - Saldo diário ajustado (depois) = `(R − S_antes − V) / Dr`
- Valores podem ficar **negativos** (excede a Reserva) → exibidos em vermelho,
  sinalizando que vira excedente.

### Exemplo

Reserva `R = 1.200`, mês com `D = 31`, hoje é dia 10 → `Dr = 22`.
Já gasto no mês `S_antes = 300`. Novo lançamento `V = 50`.

| Item                          | Valor                    |
|-------------------------------|--------------------------|
| Reserva do mês                | 1.200,00                 |
| Já gasto no mês               | 300,00                   |
| Restante do mês (antes)       | 900,00                   |
| Saldo diário disponível (antes) | 900 / 22 = **40,91**   |
| Este lançamento               | − 50,00                  |
| Restante do mês (depois)      | 850,00                   |
| Saldo diário ajustado (depois)| 850 / 22 = **38,64**     |

Se ao longo do mês os rápidos somarem 1.500 (R = 1.200):
a tabela mostra **Reserva 1.200** + **Excedente de Reserva 300**, e os totais
sobem 300.

## 5. UX

### Botão +
- **Mobile**: o Fab **+** abre o **Lançamento rápido**.
- **Desktop**: o botão do menu lateral vira **"+ Gasto rápido"** (abre o rápido).
- **Aba Lançamentos**: ganha um botão **"Novo lançamento"** → formulário completo
  atual (tipo/descrição/valor/data/categoria/recorrência).

### Diálogo de Lançamento Rápido
Campos: **Valor** (R$), **Data** (default hoje) ⚠️, **Descrição** (opcional) ⚠️.
Painel de orçamento (atualiza ao digitar o valor):
- Saldo diário disponível (antes)
- Saldo diário ajustado (depois) ← quanto sobra por dia se confirmar
- Restante da Reserva do mês (depois)
- Aviso quando ficar negativo ("excede a Reserva — vira excedente").
- ⚠️ Se não houver Reserva definida (`R = 0`): aviso "defina sua Reserva"; o
  lançamento ainda é permitido e conta integralmente como excedente.

### Tabela
- Linha **Reserva** = `R` (como hoje, linha única).
- Nova linha **Excedente de Reserva** logo abaixo, só nos meses com `E > 0`,
  com distinção visual (vermelho mais forte). Conta nos totais.
- ⚠️ A linha de excedente é **derivada** (não é ocultável individualmente na
  simulação). Confirmar se deve ou não ter o olho.

## 6. API (proposta)

- `POST /api/entries/quick` → body `{ amount, date (YYYY-MM-DD, default hoje),
  description? }`. Cria o DESPESA UNICO na categoria Reserva. Retorna o status
  atualizado (abaixo).
- `GET /api/reserva/status?date=YYYY-MM-DD` → `{ reserva R, daysInMonth D,
  daysRemaining Dr, spentMonth S, remainingMonth, dailyAvailable }` para montar o
  painel. O "depois" é calculado no front ao digitar `V`.
- `GET /api/report/grid` → passa a devolver, na categoria Reserva, também a linha
  **Excedente de Reserva** (ou um campo separado) por mês.

## 7. Casos de borda

- Excluir/editar um rápido pela aba Lançamentos → recalcula `S`, excedente e
  totais naturalmente.
- Rápido com data em mês diferente do atual → entra no `S` daquele mês; o painel
  usa a data informada.
- `R = 0` → todo rápido é excedente.
- Simulação (olho): a linha Reserva segue ocultável; excedente ⚠️ (ver §5).

## 8. Fora de escopo (desta entrega)

- Relatório/visão separada de gastos rápidos por dia.
- Alertas/push ao estourar a reserva.
- Categorização dos gastos rápidos (todos caem em "Reserva").

## 9. Pontos confirmados

1. Data do rápido: default hoje, **editável**. ✅
2. Descrição: **opcional**. ✅
3. `Dr` = `D − dia + 1` (inclui o dia do lançamento). ✅
4. Linha **Excedente de Reserva**: **não** ocultável na simulação (derivada). ✅
5. "Saldo diário disponível" exibido = **antes** do lançamento. ✅
6. Gasto rápido **aparece** na lista da aba Lançamentos. ✅
