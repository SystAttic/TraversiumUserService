package travesium.userservice.config

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
class FirebaseFilterConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        firebaseAuthenticationFilter: FirebaseAuthenticationFilter
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/rest/v1/users/exists").permitAll()
                    .requestMatchers("/rest/**").authenticated()
                    .requestMatchers("/graphql").authenticated()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/swagger-ui.html"
                    ).permitAll()
                    .requestMatchers("/internal/**").permitAll()
                    .anyRequest().permitAll()
            }
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

}