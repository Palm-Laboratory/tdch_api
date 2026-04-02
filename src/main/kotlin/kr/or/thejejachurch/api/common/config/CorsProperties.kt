package kr.or.thejejachurch.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf(
        "http://localhost:3000",
        "http://127.0.0.1:3000",
    ),
)
