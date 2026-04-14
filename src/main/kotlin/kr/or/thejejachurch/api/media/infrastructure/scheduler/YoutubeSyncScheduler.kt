package kr.or.thejejachurch.api.media.infrastructure.scheduler

import kr.or.thejejachurch.api.media.application.YoutubeSyncService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class YoutubeSyncScheduler(
    private val youtubeSyncService: YoutubeSyncService,
) {

    @Scheduled(cron = "0 */30 * * * *")
    fun sync() {
        youtubeSyncService.syncAllMenus()
    }
}
