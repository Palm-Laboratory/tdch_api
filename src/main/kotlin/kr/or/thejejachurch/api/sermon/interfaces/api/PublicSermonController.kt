package kr.or.thejejachurch.api.sermon.interfaces.api

import kr.or.thejejachurch.api.sermon.application.SermonService
import kr.or.thejejachurch.api.sermon.interfaces.dto.toDto
import kr.or.thejejachurch.api.youtube.domain.YouTubeContentForm
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/public/sermons")
class PublicSermonController(
    private val sermonService: SermonService,
) {
    @GetMapping
    fun getSermons(
        @RequestParam(required = false, defaultValue = "LONGFORM") form: YouTubeContentForm,
    ) = sermonService.getPublicSermons(form).toDto()

    @GetMapping("/{videoId}")
    fun getSermonDetail(
        @PathVariable videoId: String,
    ) = sermonService.getPublicSermonDetail(videoId).toDto()
}
