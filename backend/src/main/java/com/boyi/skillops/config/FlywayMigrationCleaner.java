package com.boyi.skillops.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import org.springframework.core.annotation.Order;

@Component
@Order(1)
public class FlywayMigrationCleaner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationCleaner.class);

    @Autowired private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
            flyway.migrate();
            log.info("Flyway cleaned and re-migrated successfully");
        } catch (Exception e) {
            log.warn("FlywayMigrationCleaner: {}", e.getMessage());
        }
    }
}
