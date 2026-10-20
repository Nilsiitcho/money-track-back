# money-track-back

API REST de controle financeiro pessoal. Java 21 + Spring Boot 3, PostgreSQL, Flyway, JWT.

Este repositorio contem o backend **e** o `docker-compose.yml` que sobe o ambiente
completo (banco + backend + frontend). O frontend fica no repositorio irmao
`../money-track-front`.

## Stack

- Java 21, Spring Boot 3.3
- Spring Web, Data JPA, Validation, Security
- PostgreSQL 16 + Flyway (migrations versionadas, sem `ddl-auto`)
- JWT (jjwt)
- OpenAPI/Swagger (springdoc)
- Testes: JUnit 5 (regras de recorrencia, saldo/acumulado e alcance de edicao/exclusao)

## Estrutura

```
money-track-back/
├─ docker-compose.yml        # sobe db + backend + frontend
├─ Dockerfile                # build multi-stage (Maven -> JRE 21)
├─ .env.example              # variaveis de ambiente
├─ pom.xml
└─ src/main/
   ├─ java/com/moneytrack/
   │  ├─ MoneyTrackApplication.java
   │  ├─ common/             # erros, helpers de mes
   │  ├─ config/             # OpenAPI
   │  ├─ security/           # JWT, filtro, login, seed do usuario
   │  ├─ user/               # entidade User
   │  ├─ category/           # CRUD de categorias
   │  ├─ entry/              # lancamentos
   │  │  ├─ domain/          # LOGICA PURA (testavel): ocorrencias, saldo, edicao com alcance
   │  │  └─ dto/
   │  └─ dashboard/          # painel mensal
   └─ resources/
      ├─ application.yml
      └─ db/migration/       # V1 schema, V2 usuario, V3 categorias padrao
```

### Modelo de recorrencia (resumo)

Nao materializamos ocorrencias. Guardamos a **regra** em `entry` e calculamos as
ocorrencias de cada mes sob demanda. Edicoes/exclusoes "somente este mes" viram
linhas em `entry_override`. "Este e os seguintes" encerra a regra e cria outra a
partir do mes de referencia. Detalhes em `entry/domain/`.

## Subir o ambiente completo (recomendado)

Pre-requisitos: Docker + Docker Compose.

```bash
cp .env.example .env         # ajuste segredos antes de usar em rede/producao
docker compose up --build
```

Sobe:

| Servico   | URL                                   |
|-----------|---------------------------------------|
| Frontend  | http://localhost:5173                 |
| Backend   | http://localhost:8080                 |
| Swagger   | http://localhost:8080/swagger-ui.html |
| Postgres  | localhost:5432                        |

Login inicial: usuario `APP_SEED_USER` / senha `APP_SEED_PASSWORD` do `.env`
(defaults `admin` / `changeme`). A senha e aplicada no startup; troque-a.

Parar: `docker compose down` (dados do banco ficam no volume `db_data`;
`docker compose down -v` apaga tudo).

## Rodar so o backend (sem Docker)

Pre-requisitos: JDK 21, Maven 3.9+, um PostgreSQL acessivel.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/moneytrack
export SPRING_DATASOURCE_USERNAME=moneytrack
export SPRING_DATASOURCE_PASSWORD=moneytrack
export APP_JWT_SECRET=$(openssl rand -base64 48)
export APP_SEED_USER=admin
export APP_SEED_PASSWORD=changeme
mvn spring-boot:run
```

## Testes

```bash
mvn test
```

Cobrem as regras criticas: geracao de recorrencias e numeracao de parcelas
(`OccurrenceCalculatorTest`), saldo mensal e acumulado com projecao futura
(`BalanceCalculatorTest`) e alcance de edicao/exclusao
(`RecurrenceEditorTest`).

## Variaveis de ambiente

Veja `.env.example`. Principais:

| Variavel                  | Descricao                                         | Default                 |
|---------------------------|---------------------------------------------------|-------------------------|
| `SPRING_DATASOURCE_URL`   | URL JDBC do Postgres                               | localhost:5432          |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | credenciais do banco             | moneytrack              |
| `APP_JWT_SECRET`          | segredo HMAC do JWT (>= 32 bytes)                 | (troque!)               |
| `APP_JWT_EXPIRATION_HOURS`| validade do token em horas                        | 168 (7 dias)            |
| `APP_SEED_USER` / `_PASSWORD` | credenciais do usuario unico (aplicadas no boot) | admin / changeme     |
| `APP_CORS_ORIGINS`        | origens permitidas (CSV)                           | http://localhost:5173   |

## API (resumo)

Autenticacao via `Authorization: Bearer <token>` (obtido em `/api/auth/login`).

```
POST   /api/auth/login
GET    /api/categories?includeArchived=false
POST   /api/categories
PUT    /api/categories/{id}
POST   /api/categories/{id}/archive
DELETE /api/categories/{id}                      # 409 se em uso
GET    /api/entries?month=YYYY-MM&type&categoryId&recurrence&q
POST   /api/entries
GET    /api/entries/{id}
PUT    /api/entries/{id}                          # body: scope, referenceMonth
DELETE /api/entries/{id}?scope&referenceMonth
GET    /api/dashboard?month=YYYY-MM
```

Documentacao completa e interativa no Swagger.
