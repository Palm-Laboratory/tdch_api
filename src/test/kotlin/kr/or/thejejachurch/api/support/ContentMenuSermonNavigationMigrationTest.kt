package kr.or.thejejachurch.api.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ContentMenuSermonNavigationMigrationTest {

    @Test
    fun `content menu sermon navigation migration adds operating columns and indexes`() {
        val resource = javaClass.classLoader.getResourceAsStream(
            "db/migration/V18__extend_content_menu_sermon_navigation.sql",
        )

        assertThat(resource)
            .describedAs("V18 migration should exist on the test classpath")
            .isNotNull()

        val sql = resource!!.bufferedReader().use { it.readText() }.lowercase()

        assertThat(sql).contains("alter table content_menu add column status")
        assertThat(sql).contains("alter table content_menu add column navigation_visible")
        assertThat(sql).contains("alter table content_menu add column sort_order")
        assertThat(sql).contains("alter table content_menu add column description")
        assertThat(sql).contains("alter table content_menu add column discovered_at")
        assertThat(sql).contains("alter table content_menu add column published_at")
        assertThat(sql).contains("alter table content_menu add column last_modified_by")
        assertThat(sql).contains("check (status in")
        assertThat(sql).contains("create index")
    }
}
