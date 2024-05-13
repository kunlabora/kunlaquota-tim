package be.kunlabora.crafters.kunlaquota.rest

import be.kunlabora.crafters.kunlaquota.service.IQuotes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.function.router
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.spring6.view.ThymeleafViewResolver
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

@Configuration
@EnableWebMvc
class WebConfig : WebMvcConfigurer {

    @Bean
    fun apiRoutesBean(quotes: IQuotes) = router {
        "/api".nest(apiRoutes(quotes))
    }

    @Bean
    fun viewResolver(): ViewResolver {
        val resolver = ThymeleafViewResolver()
        resolver.templateEngine = templateEngine()
        resolver.characterEncoding = "UTF-8"
        return resolver
    }

    private fun templateEngine(): SpringTemplateEngine {
        val engine = SpringTemplateEngine()
        engine.setTemplateResolver(templateResolver())
        return engine
    }

    private fun templateResolver(): ClassLoaderTemplateResolver {
        val resolver = ClassLoaderTemplateResolver()
        resolver.prefix = "/templates/" // Your template directory
        resolver.suffix = ".html"
        resolver.templateMode = TemplateMode.HTML
        return resolver
    }

    override fun configureViewResolvers(registry: ViewResolverRegistry) {
        registry.viewResolver(viewResolver())
    }
}