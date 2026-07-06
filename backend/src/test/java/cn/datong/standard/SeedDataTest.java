package cn.datong.standard;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SeedDataTest {

    @Test
    void seedsAllDocumentSectionsForSidebar() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V17__seed_missing_sections.sql"));
        String initData = Files.readString(Path.of("../deploy/mysql-init/02-init-data.sql"));
        String deployMigration = Files.readString(Path.of("../deploy/mysql-init/17-seed-missing-sections.sql"));
        String runScript = Files.readString(Path.of("../run.sh"));

        assertThat(migration).contains("计划财务科", "劳动人事科（党委组织科）");
        assertThat(initData).contains("计划财务科", "劳动人事科（党委组织科）");
        assertThat(deployMigration).contains("计划财务科", "劳动人事科（党委组织科）");
        assertThat(runScript).contains("17-seed-missing-sections.sql");
    }
}
