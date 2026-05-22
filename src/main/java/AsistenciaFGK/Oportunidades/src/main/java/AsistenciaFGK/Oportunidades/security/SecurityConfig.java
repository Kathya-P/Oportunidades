/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AsistenciaFGK.Oportunidades.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // habilita @PreAuthorize en los controllers
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
            .csrf(csrf -> csrf.disable()) 
.authorizeHttpRequests(auth -> auth

    // Público
    .requestMatchers("/login", "/images/**").permitAll()

    // Lector
    .requestMatchers(
        "/asistencia/lector",
        "/asistencia/registrar"
    ).permitAll()

    // Docente
    .requestMatchers("/docente/**")
    .hasRole("DOCENTE")

    // Supervisor
    .requestMatchers("/supervisor/**")
    .hasRole("SUPERVISOR")

    // Admin
    .requestMatchers("/admin/**")
    .hasRole("ADMIN")

    // Calendario
    .requestMatchers("/calendario")
    .authenticated()

    .requestMatchers("/calendario/**")
    .authenticated()

    // FINAL SIEMPRE
    .anyRequest().authenticated()
)
                
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {

                    var roles = authentication.getAuthorities();

                    if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"))) {
                        response.sendRedirect("/admin");
                    } else if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_DOCENTE"))) {
                        response.sendRedirect("/docente");
                    } else if (roles.stream().anyMatch(r -> r.getAuthority().equals("ROLE_SUPERVISOR"))) {
                        response.sendRedirect("/supervisor");
                    } else {
                        response.sendRedirect("/dashboard");
                    }

                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/login?logout=true")
    .invalidateHttpSession(true)
    .deleteCookies("JSESSIONID")
    .permitAll()
)
.exceptionHandling(ex -> ex
    .accessDeniedPage("/403")
);  

return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}