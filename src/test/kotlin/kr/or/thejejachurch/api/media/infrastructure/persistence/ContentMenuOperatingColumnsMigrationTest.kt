package kr.or.thejejachurch.api.media.infrastructure.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ContentMenuOperatingColumnsMigrationTest {

    @Test
    fun `content menu operating columns migration adds sermon navigation fields`() {
        val resource = javaClass.getResource("/db/migration/V12__extend_content_menu_for_sermon_navigation.sql")

        assertThat(resource).isNotNull()

        val sql = resource!!.readText()

        assertThat(sql).contains("alter table content_menu")
        assertThat(sql).contains("add column status varchar(20) not null default 'DRAFT'")
        assertThat(sql).contains("add column navigation_visible boolean not null default true")
        assertThat(sql).contains("add column sort_order integer not null default 0")
        assertThat(sql).contains("add column description text")
        assertThat(sql).contains("add column discovered_at timestamptz")
        assertThat(sql).contains("add column published_at timestamptz")
        assertThat(sql).contains("add column last_modified_by bigint")
        assertThat(sql).contains("check (status in ('DRAFT', 'PUBLISHED', 'INACTIVE'))")
        assertThat(sql).contains("create index idx_content_menu_status_sort")
        assertThat(sql).contains("create index idx_content_menu_navigation_visible")
    }
}
