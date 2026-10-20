-- Usuario unico (semente). Username e senha reais sao aplicados no startup
-- a partir das variaveis APP_SEED_USER / APP_SEED_PASSWORD (DataInitializer).
-- O hash abaixo e um placeholder que sera sobrescrito no primeiro boot.
INSERT INTO app_user (id, username, password_hash)
VALUES (1, 'admin', 'PLACEHOLDER_SET_ON_STARTUP');
