package com.stackr.api.migration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:migrations;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
        "spring.jpa.hibernate.ddl-auto=none"
})
class MigrationIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Test void liquibaseCreatesSchemaAndQueueIndex() {
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class)).isEqualTo(5);
        jdbc.queryForList("SELECT id, verdict, submitted_at FROM submissions");
        jdbc.queryForList("SELECT invite_token FROM contests");
    }
}
