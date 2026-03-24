package kr.or.thejejachurch.api.navigation.interfaces.api

import kr.or.thejejachurch.api.navigation.application.NavigationQueryService
import kr.or.thejejachurch.api.navigation.interfaces.dto.NavigationResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/navigation")
class NavigationController(
    private val navigationQueryService: NavigationQueryService,
) {

    @GetMapping
    fun getNavigation(): NavigationResponse = navigationQueryService.getNavigation()
}
