package kr.or.thejejachurch.api.media.application.bootstrap

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class PlaylistBootstrapRunner(
    private val playlistBootstrapService: PlaylistBootstrapService,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        playlistBootstrapService.bootstrap()
    }
}
