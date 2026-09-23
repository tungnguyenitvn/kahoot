package dev.sample.quiz.identity;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import jakarta.servlet.http.HttpServletResponse;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(JdbcTemplate jdbc) {
        return name -> jdbc.query("select * from app_user where username=?",(rs,n)-> new HostPrincipal(rs.getString("id"),rs.getString("username"),rs.getString("display_name"),rs.getString("password_hash")),name)
            .stream().findFirst().orElseThrow(()->new UsernameNotFoundException("Unknown account"));
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax"));
        http.csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            // WS still requires Origin + existing session + membership in its handshake interceptor.
            .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health","/error","/api/auth/**","/api/rooms/**","/ws/rooms/**").permitAll()
                .requestMatchers("/api/quizzes/**","/api/history/**").hasRole("HOST").anyRequest().denyAll())
            .formLogin(f -> f.loginProcessingUrl("/api/auth/login")
                .successHandler((req,res,auth)-> { res.setContentType("application/json");res.getWriter().write("{\"ok\":true}"); })
                .failureHandler((req,res,e)-> { res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"code\":\"INVALID_CREDENTIALS\"}"); }))
            .logout(l -> l.logoutUrl("/api/auth/logout").deleteCookies("SESSION")
                .logoutSuccessHandler((req,res,a)->res.setStatus(204)))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,x)->jsonError(res,401,"SESSION_REQUIRED"))
                .accessDeniedHandler((req,res,x)->jsonError(res,403,x instanceof CsrfException ? "CSRF_REQUIRED" : "ACCESS_DENIED")));
        return http.build();
    }
    private static void jsonError(HttpServletResponse response,int status,String code) throws java.io.IOException {
        response.setStatus(status);response.setContentType("application/json");response.getWriter().write("{\"code\":\""+code+"\"}");
    }
}
