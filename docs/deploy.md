# Deploy — Cloudflare Pages (front) + Render (back) + Neon (DB)

Uso pessoal, free tier. Cold start aceito. Tempo estimado: ~1h–1h30.

Pré-requisitos: repos no GitHub (ok), contas em **Neon**, **Render** e **Cloudflare**.

> Ordem importa (dependência circular de URLs): backend precisa da URL do
> front no CORS; front precisa da URL do backend. Resolve-se assim:
> Neon → Render (sem CORS ainda) → Pages (com URL do backend) → volta no
> Render e preenche o CORS.

---

## 1. Banco — Neon (~10 min)

1. neon.tech → New Project (região mais perto, ex. AWS us-east).
2. Copie a **connection string** (formato `postgresql://user:pass@HOST/dbname?sslmode=require`).
3. Derive os 3 valores para o Render:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://HOST/dbname?sslmode=require`
     (troque o prefixo `postgresql://` por `jdbc:postgresql://` e **remova** `user:pass@`)
   - `SPRING_DATASOURCE_USERNAME` = `user`
   - `SPRING_DATASOURCE_PASSWORD` = `pass`

> O Flyway cria o schema e roda os seeds (V1–V7) no 1º start. Nada manual no DB.
> Obs.: o seed V7 popula um orçamento demo; depois dá pra limpar pela UI.

## 2. Backend — Render (~25–35 min)

1. Gere os segredos localmente:
   - JWT: `openssl rand -base64 48`
   - Senha do login (APP_SEED_PASSWORD): escolha uma **forte** (não "changeme").
2. Render → **New > Blueprint** → conecte o repo `money-track-back`.
   O Render lê o `render.yaml` e cria o web service `money-track-api` (free, Docker).
3. Preencha as env vars marcadas (sync:false):
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (do Neon)
   - `APP_JWT_SECRET` (o random gerado)
   - `APP_SEED_USER` (ex.: seu email/username) e `APP_SEED_PASSWORD` (a senha forte)
   - `APP_CORS_ORIGINS` → **deixe `https://localhost` por ora**; ajusta no passo 4.
4. Deploy. O 1º build Docker (Maven) demora alguns minutos. Ao terminar, anote a URL:
   `https://money-track-api.onrender.com` (ou similar).
5. Teste rápido (pode ter cold start ~30–50s na 1ª chamada):
   ```
   curl -i -X POST https://money-track-api.onrender.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"<APP_SEED_USER>","password":"<APP_SEED_PASSWORD>"}'
   ```
   Esperado: 200 com token.

## 3. Frontend — Cloudflare Pages (~15–20 min)

1. Cloudflare → Workers & Pages → **Create > Pages > Connect to Git** → repo `money-track-front`.
2. Build settings:
   - Framework preset: **None** (ou Vite)
   - Build command: `npm run build`
   - Build output directory: `dist`
   - Variables (Environment):
     - `VITE_API_BASE_URL` = `https://money-track-api.onrender.com` (URL do passo 2)
     - (se o build reclamar da versão do Node) `NODE_VERSION` = `20`
3. Deploy. Anote a URL: `https://money-track-<algo>.pages.dev`.
   - O `public/_redirects` já garante o fallback de SPA (rotas /report etc.).

## 4. Fechar o CORS (~2 min)

1. Volte no Render → serviço → Environment → `APP_CORS_ORIGINS` =
   `https://money-track-<algo>.pages.dev` (a URL exata do Pages, sem barra no fim).
2. Salve → o Render reinicia o serviço.

## 5. Teste ponta-a-ponta (~10 min)

1. Abra a URL do Pages no navegador (e no iPhone).
2. Login com `APP_SEED_USER` / `APP_SEED_PASSWORD`.
3. Confira dashboard/tabela carregando (1ª vez pode ter cold start do Render + Neon).

---

## Notas / limites do free

- **Render free dorme** após ~15 min sem uso → 1ª request fria (~30–50s). Normal.
- **Neon free** suspende o compute ocioso → também esquenta rápido na 1ª query.
- Troca de senha/segredos: só nas env vars do Render (nunca no repo).
- `.env` segue gitignored; nada de segredo no Git.

## Próximo passo (depois do deploy): PWA

Com HTTPS no ar, dá pra tornar instalável no iPhone (manifest, ícones,
metas iOS, service worker). Fazemos em seguida.
