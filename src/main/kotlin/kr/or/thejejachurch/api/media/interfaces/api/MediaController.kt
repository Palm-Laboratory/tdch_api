package kr.or.thejejachurch.api.media.interfaces.api

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import kr.or.thejejachurch.api.media.application.MediaQueryService
import kr.or.thejejachurch.api.media.interfaces.dto.HomeMediaResponse
import kr.or.thejejachurch.api.media.interfaces.dto.MediaListResponse
import kr.or.thejejachurch.api.media.interfaces.dto.MenuDto
import kr.or.thejejachurch.api.media.interfaces.dto.VideoDetailResponse
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/media")
class MediaController(
    private val mediaQueryService: MediaQueryService,
) {

    @GetMapping("/menus")
    fun getMenus(): List<MenuDto> = mediaQueryService.getMenus()

    @GetMapping("/home")
    fun getHome(): HomeMediaResponse = mediaQueryService.getHome()

    @GetMapping("/menus/{slug}/videos")
    fun getVideos(
        @PathVariable slug: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) size: Int,
    ): MediaListResponse = mediaQueryService.getVideos(slug, page, size)

    @GetMapping("/menus/{slug}/videos/{youtubeVideoId}")
    fun getVideoBySlug(
        @PathVariable slug: String,
        @PathVariable youtubeVideoId: String,
    ): VideoDetailResponse = mediaQueryService.getVideo(slug, youtubeVideoId)

    @GetMapping("/videos/{youtubeVideoId}")
    fun getVideo(
        @PathVariable youtubeVideoId: String,
    ): VideoDetailResponse = mediaQueryService.getVideo(youtubeVideoId)
}
