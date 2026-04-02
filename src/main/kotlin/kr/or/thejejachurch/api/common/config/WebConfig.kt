package kr.or.thejejachurch.api.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val corsProperties: CorsProperties,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val allowedOrigins = corsProperties.allowedOrigins
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

        if (allowedOrigins.isEmpty()) {
            return
        }

        registry.addMapping("/api/**")
            .allowedOrigins(*allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600)
    }
}
