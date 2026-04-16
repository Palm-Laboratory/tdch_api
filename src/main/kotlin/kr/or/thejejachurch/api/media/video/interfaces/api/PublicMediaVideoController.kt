package kr.or.thejejachurch.api.media.video.interfaces.api

import kr.or.thejejachurch.api.media.video.application.MediaVideoService
import kr.or.thejejachurch.api.media.video.interfaces.dto.toDto
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/public/media/videos")
class PublicMediaVideoController(
    private val mediaVideoService: MediaVideoService,
) {
    @GetMapping
    fun getVideos(
        @RequestParam(required = false, defaultValue = "LONGFORM") form: YouTubeContentForm,
    ) = mediaVideoService.getPublicMediaVideos(form).toDto()

    @GetMapping("/{videoId}")
    fun getVideoDetail(
        @PathVariable videoId: String,
    ) = mediaVideoService.getPublicMediaVideoDetail(videoId).toDto()
}
