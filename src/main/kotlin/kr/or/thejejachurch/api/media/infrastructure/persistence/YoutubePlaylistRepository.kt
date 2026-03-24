package kr.or.thejejachurch.api.media.infrastructure.persistence

import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import org.springframework.data.jpa.repository.JpaRepository

interface YoutubePlaylistRepository : JpaRepository<YoutubePlaylist, Long> {
    fun findAllBySyncEnabledTrueOrderByIdAsc(): List<YoutubePlaylist>
    fun findByContentMenuId(contentMenuId: Long): YoutubePlaylist?
    fun findByContentMenuIdAndSyncEnabledTrue(contentMenuId: Long): YoutubePlaylist?
    fun findByYoutubePlaylistId(youtubePlaylistId: String): YoutubePlaylist?
}
