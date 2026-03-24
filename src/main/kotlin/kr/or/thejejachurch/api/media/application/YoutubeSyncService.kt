package kr.or.thejejachurch.api.media.application

import kr.or.thejejachurch.api.common.config.YoutubeProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class YoutubeSyncService(
    private val youtubeProperties: YoutubeProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun syncAllMenus() {
        log.info(
            "YouTube sync bootstrap invoked. messages={}, betterDevotion={}, itsOkay={}",
            youtubeProperties.playlists.messages.isNotBlank(),
            youtubeProperties.playlists.betterDevotion.isNotBlank(),
            youtubeProperties.playlists.itsOkay.isNotBlank(),
        )
    }
}
