package kr.or.thejejachurch.api.media.infrastructure.scheduler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Scheduled

class YoutubeSyncSchedulerContractTest {

    @Test
    fun `youtube sync scheduler should run at 6am and 11pm daily`() {
        val method = YoutubeSyncScheduler::class.java.getDeclaredMethod("sync")
        val scheduled = method.getAnnotation(Scheduled::class.java)

        assertThat(scheduled).isNotNull
        assertThat(scheduled!!.cron).isEqualTo("0 0 6,23 * * *")
    }
}
