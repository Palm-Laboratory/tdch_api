package kr.or.thejejachurch.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf(
        "https://tdch.co.kr",
        "https://www.tdch.co.kr",
    ),
)
