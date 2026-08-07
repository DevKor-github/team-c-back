package devkor.com.teamcback.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class UpdateNoticeSeedSqlTest {

    private static final Path SEED_SQL = Path.of("docs/sql/2026-08-07-update-notice.sql");

    @Test
    void seedsHistoricalNoticesAndCurrentReleaseIdempotently() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:update_notice_seed;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                "sa",
                ""
        )) {
            createSchema(connection);

            String seedSql = Files.readString(SEED_SQL);
            executeStatements(connection, seedSql);
            executeStatements(connection, seedSql);

            assertThat(queryCount(connection, "SELECT COUNT(*) FROM tb_update_notice"))
                    .isEqualTo(11);
            assertThat(queryCount(connection, "SELECT COUNT(*) FROM tb_update_notice_feature"))
                    .isEqualTo(26);
            assertThat(queryCount(
                    connection,
                    "SELECT COUNT(*) FROM tb_update_notice "
                            + "WHERE show_popup = TRUE AND app_version = '1.1.5'"
            )).isEqualTo(1);
            assertThat(queryCount(
                    connection,
                    "SELECT COUNT(*) FROM tb_update_notice "
                            + "WHERE show_popup = TRUE AND app_version <> '1.1.5'"
            )).isZero();
        }
    }

    private void createSchema(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE tb_update_notice (
                        update_notice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description VARCHAR(2000) NOT NULL,
                        published_at DATETIME NOT NULL,
                        app_version VARCHAR(40) NOT NULL,
                        show_popup BOOLEAN NOT NULL,
                        link_url VARCHAR(1000),
                        link_label VARCHAR(100)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE tb_update_notice_feature (
                        update_notice_id BIGINT NOT NULL,
                        display_order INT NOT NULL,
                        feature VARCHAR(500) NOT NULL,
                        PRIMARY KEY (update_notice_id, display_order)
                    )
                    """);
        }
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        for (String statementSql : sql.split(";")) {
            if (statementSql.isBlank()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private long queryCount(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
