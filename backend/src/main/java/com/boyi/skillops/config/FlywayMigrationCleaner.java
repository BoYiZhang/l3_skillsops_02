package com.boyi.skillops.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@Component
@Order(1)
@Profile("!test")
public class FlywayMigrationCleaner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(FlywayMigrationCleaner.class);

    @Autowired private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        // 只在表不存在时做首次迁移，后续启动保留数据
        if (tablesExist()) {
            log.info("Tables already exist, skipping migration");
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate(); // 只跑新增的迁移，不 clean
            return;
        }
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
        log.info("Flyway initialized with clean + migrate");
    }

    private boolean tablesExist() {
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "users", null)) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }
}
