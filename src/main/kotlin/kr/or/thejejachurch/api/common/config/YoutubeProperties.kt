package kr.or.thejejachurch.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "youtube")
data class YoutubeProperties(
    val apiKey: String = "",
    val channelId: String = "",
)
