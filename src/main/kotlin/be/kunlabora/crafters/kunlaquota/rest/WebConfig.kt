package be.kunlabora.crafters.kunlaquota.rest

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.function.router

@Configuration
@EnableWebMvc
class WebConfig : WebMvcConfigurer {

    @Bean
    fun apiRoutes() = router { "/api".nest(apiRoutes) }

    override fun configureMessageConverters(converters: List<HttpMessageConverter<*>>) {
        // configure message conversion...
    }


}