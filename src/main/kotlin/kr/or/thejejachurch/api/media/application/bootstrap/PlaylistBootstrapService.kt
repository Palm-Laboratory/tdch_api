package kr.or.thejejachurch.api.media.application.bootstrap

import kr.or.thejejachurch.api.common.config.YoutubeProperties
import kr.or.thejejachurch.api.media.domain.ContentMenu
import kr.or.thejejachurch.api.media.domain.YoutubePlaylist
import kr.or.thejejachurch.api.media.infrastructure.persistence.ContentMenuRepository
import kr.or.thejejachurch.api.media.infrastructure.persistence.YoutubePlaylistRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class PlaylistBootstrapService(
    private val youtubeProperties: YoutubeProperties,
    private val contentMenuRepository: ContentMenuRepository,
    private val youtubePlaylistRepository: YoutubePlaylistRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun bootstrap() {
        upsertPlaylist(siteKey = "messages", playlistId = youtubeProperties.playlists.messages, fallbackTitle = "말씀/설교")
        upsertPlaylist(
            siteKey = "better-devotion",
            playlistId = youtubeProperties.playlists.betterDevotion,
            fallbackTitle = "더 좋은 묵상",
        )
        upsertPlaylist(siteKey = "its-okay", playlistId = youtubeProperties.playlists.itsOkay, fallbackTitle = "그래도 괜찮아")
    }

    private fun upsertPlaylist(
        siteKey: String,
        playlistId: String,
        fallbackTitle: String,
    ) {
        if (playlistId.isBlank()) {
            log.info("Playlist bootstrap skipped for siteKey={} because env value is blank.", siteKey)
            return
        }

        val menu = contentMenuRepository.findBySiteKey(siteKey)
            ?: throw IllegalStateException("content_menu not found for siteKey=$siteKey")

        val existingByMenu = youtubePlaylistRepository.findByContentMenuId(menu.id!!)
        val existingByPlaylistId = youtubePlaylistRepository.findByYoutubePlaylistId(playlistId)

        if (existingByPlaylistId != null && existingByPlaylistId.contentMenuId != menu.id && existingByMenu == null) {
            throw IllegalStateException(
                "youtube_playlist_id=$playlistId is already mapped to content_menu_id=${existingByPlaylistId.contentMenuId}",
            )
        }

        if (existingByMenu != null && existingByPlaylistId != null && existingByMenu.id != existingByPlaylistId.id) {
            throw IllegalStateException(
                "Conflicting youtube_playlist rows for siteKey=$siteKey and playlistId=$playlistId",
            )
        }

        val now = OffsetDateTime.now()
        val target = existingByMenu ?: existingByPlaylistId

        if (target == null) {
            youtubePlaylistRepository.save(
                YoutubePlaylist(
                    contentMenuId = menu.id,
                    youtubePlaylistId = playlistId,
                    title = fallbackTitle,
                    syncEnabled = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            log.info("Playlist bootstrap created mapping siteKey={} playlistId={}", siteKey, playlistId)
            return
        }

        applyBootstrapValues(
            playlist = target,
            menu = menu,
            playlistId = playlistId,
            fallbackTitle = fallbackTitle,
            now = now,
        )
        log.info("Playlist bootstrap updated mapping siteKey={} playlistId={}", siteKey, playlistId)
    }

    private fun applyBootstrapValues(
        playlist: YoutubePlaylist,
        menu: ContentMenu,
        playlistId: String,
        fallbackTitle: String,
        now: OffsetDateTime,
    ) {
        playlist.contentMenuId = menu.id!!
        playlist.youtubePlaylistId = playlistId
        playlist.syncEnabled = true
        if (playlist.title.isBlank()) {
            playlist.title = fallbackTitle
        }
        playlist.updatedAt = now
    }
}
