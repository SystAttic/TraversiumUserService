package travesium.userservice.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import travesium.userservice.security.FirebaseAuthenticationFilter

/**
 * @author Maja Razinger
 */
@Configuration
@EnableWebSecurity
//@Profile("auth")
class FirebaseFilterConfig {

    @Bean
    fun firebaseAuthFilter(
        firebaseAuthenticationFilter: FirebaseAuthenticationFilter
    ): FilterRegistrationBean<FirebaseAuthenticationFilter> {
        val registration = FilterRegistrationBean(
            firebaseAuthenticationFilter
        )
        registration.addUrlPatterns("/rest/*")
        registration.order = 1
        return registration
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        firebaseAuthenticationFilter: FirebaseAuthenticationFilter
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/rest/**").authenticated()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .securityMatcher("/rest/**")

        return http.build()
    }
}