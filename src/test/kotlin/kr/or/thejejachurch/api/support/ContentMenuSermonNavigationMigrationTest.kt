package kr.or.thejejachurch.api.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ContentMenuSermonNavigationMigrationTest {

    @Test
    fun `content menu sermon navigation migration adds operating columns and indexes`() {
        val resource = javaClass.classLoader.getResourceAsStream(
            "db/migration/V18__extend_content_menu_for_sermon_navigation.sql",
        )

        assertThat(resource)
            .describedAs("V18 migration should exist on the test classpath")
            .isNotNull()

        val sql = resource!!.bufferedReader().use { it.readText() }.lowercase()

        assertThat(sql).contains("alter table content_menu")
        assertThat(sql).contains("add column status")
        assertThat(sql).contains("add column navigation_visible")
        assertThat(sql).contains("add column sort_order")
        assertThat(sql).contains("add column description")
        assertThat(sql).contains("add column discovered_at")
        assertThat(sql).contains("add column published_at")
        assertThat(sql).contains("add column last_modified_by")
        assertThat(sql).contains("check (status in")
        assertThat(sql).contains("create index")
    }
}
