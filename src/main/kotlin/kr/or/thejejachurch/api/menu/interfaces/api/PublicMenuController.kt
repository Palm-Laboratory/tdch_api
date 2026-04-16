package kr.or.thejejachurch.api.menu.interfaces.api

import kr.or.thejejachurch.api.menu.application.PublicMenuService
import kr.or.thejejachurch.api.menu.interfaces.dto.toDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/public")
class PublicMenuController(
    private val publicMenuService: PublicMenuService,
) {
    @GetMapping("/menu")
    fun getMenu() = publicMenuService.getNavigation().toDto()

    @GetMapping("/menu/resolve")
    fun resolveMenuPath(
        @RequestParam path: String,
    ) = publicMenuService.resolveMenuPath(path).toDto()

    @GetMapping("/videos/{slug}")
    fun getVideoDetail(
        @PathVariable slug: String,
    ) = publicMenuService.getVideoDetail(slug).toDto()

    @GetMapping("/videos")
    fun getVideoDetailByPath(
        @RequestParam path: String,
    ) = publicMenuService.getVideoDetailByPath(path).toDto()
}
