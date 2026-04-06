package kr.or.thejejachurch.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "admin")
data class AdminProperties(
    val syncKey: String = "",
    val bootstrap: BootstrapProperties = BootstrapProperties(),
) {
    data class BootstrapProperties(
        val username: String = "",
        val password: String = "",
        val displayName: String = "슈퍼 관리자",
    )
}
