package kz.edu.sdu.advisor.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationPolicyTest {

    @Test
    void defaultProfile_shouldUseSqliteByDefault() throws Exception {
        Properties properties = load("application.properties");

        assertThat(properties.getProperty("spring.datasource.url")).startsWith("jdbc:sqlite:");
        assertThat(properties.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.sqlite.JDBC");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
    }

    @Test
    void dockerProfile_shouldUsePostgres() throws Exception {
        Properties properties = load("application-docker.properties");

        assertThat(properties.getProperty("spring.datasource.url")).contains("jdbc:postgresql://");
        assertThat(properties.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.postgresql.Driver");
        assertThat(properties.getProperty("spring.jpa.database-platform")).isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
    }

    @Test
    void testProfile_shouldUseIsolatedSqliteDatabase() throws Exception {
        Properties properties = load("application-test.properties");

        assertThat(properties.getProperty("spring.datasource.url")).isEqualTo("jdbc:sqlite:target/test-advisor.db");
        assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");
    }

    private Properties load(String resourceName) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(inputStream).as("Resource '%s' should be present on classpath", resourceName).isNotNull();
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        }
    }
}
