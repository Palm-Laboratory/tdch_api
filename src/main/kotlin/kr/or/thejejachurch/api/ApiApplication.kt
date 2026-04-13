package kr.or.thejejachurch.api

import kr.or.thejejachurch.api.common.config.AdminProperties
import kr.or.thejejachurch.api.common.config.CorsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(
    value = [
        AdminProperties::class,
        CorsProperties::class,
    ],
)
class ApiApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
