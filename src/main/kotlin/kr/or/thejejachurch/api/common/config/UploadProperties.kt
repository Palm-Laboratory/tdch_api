package kr.or.thejejachurch.api.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tdch.uploads")
data class UploadProperties(
    val rootPath: String = "/opt/tdch/uploads",
    val publicBaseUrl: String = "https://api.tdch.co.kr/upload",
)
