package com.moneytrack.security;

import com.moneytrack.user.User;
import com.moneytrack.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Aplica username/senha do usuario semente (id=1) a partir das variaveis de ambiente
 * APP_SEED_USER / APP_SEED_PASSWORD. O schema e as categorias padrao vem das migrations.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final long SEED_USER_ID = 1L;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String seedUsername;
    private final String seedPassword;

    public DataInitializer(UserRepository users, PasswordEncoder encoder,
                           @Value("${app.seed.username}") String seedUsername,
                           @Value("${app.seed.password}") String seedPassword) {
        this.users = users;
        this.encoder = encoder;
        this.seedUsername = seedUsername;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        User user = users.findById(SEED_USER_ID).orElseGet(() -> {
            User u = new User();
            u.setId(SEED_USER_ID);
            return u;
        });
        user.setUsername(seedUsername);
        user.setPasswordHash(encoder.encode(seedPassword));
        users.save(user);
        log.info("Usuario semente pronto: '{}'", seedUsername);
    }
}
