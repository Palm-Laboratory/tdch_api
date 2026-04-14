package kr.or.thejejachurch.api.navigation.interfaces.dto

data class NavigationItemDto(
    val key: String,
    val label: String,
    val href: String,
    val matchPath: String?,
    val linkType: String,
    val contentSiteKey: String? = null,
    val visible: Boolean,
    val headerVisible: Boolean,
    val mobileVisible: Boolean,
    val lnbVisible: Boolean,
    val breadcrumbVisible: Boolean,
    val defaultLanding: Boolean,
)

data class NavigationGroupDto(
    val key: String,
    val label: String,
    val href: String,
    val matchPath: String?,
    val linkType: String,
    val contentSiteKey: String? = null,
    val visible: Boolean,
    val headerVisible: Boolean,
    val mobileVisible: Boolean,
    val lnbVisible: Boolean,
    val breadcrumbVisible: Boolean,
    val defaultLandingHref: String?,
    val items: List<NavigationItemDto>,
)

data class NavigationResponse(
    val groups: List<NavigationGroupDto>,
)
