package fr.ght1pc9kc.baywatch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import fr.ght1pc9kc.baywatch.api.model.News;
import fr.ght1pc9kc.baywatch.domain.ports.JwtTokenProvider;
import fr.ght1pc9kc.baywatch.domain.security.JwtTokenProviderImpl;
import fr.ght1pc9kc.baywatch.infra.config.jackson.NewsMixin;
import fr.ght1pc9kc.baywatch.infra.security.JwtTokenAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
public class BaywatchApplication {

    public static final String APPLICATION_NAME = "Baywatch";

    public static void main(String[] args) {
        SpringApplication.run(BaywatchApplication.class, args);
    }

    @Bean
    Clock getSystemUTCClock() {
        return Clock.systemUTC();
    }

    @Bean
    Scheduler getDatabaseScheduler() {
        return Schedulers.newBoundedElastic(5, Integer.MAX_VALUE, "database");
    }

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProviderImpl(Duration.ofHours(1));
        return http
//                .csrf().disable()
                .httpBasic().disable()

//                .formLogin().disable()
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange()
                .pathMatchers("/**").permitAll()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
//                .pathMatchers(HttpMethod.POST, "/login").permitAll()
//                .pathMatchers(HttpMethod.PUT, "/refresh").hasAuthority(TokenAuthority.ROLE_REFRESH.toString())
//                .pathMatchers(HttpMethod.GET, "/session/validate").hasAuthority(TokenAuthority.ROLE_REFRESH.toString())
//                .pathMatchers(HttpMethod.DELETE, "/logout").hasAuthority(TokenAuthority.ROLE_REFRESH.toString())
//                .anyExchange().hasAuthority(TokenAuthority.ROLE_ACCESS.toString())
//
//                .and()
//                .addFilterAt(apiAuthenticationWebFilter(), SecurityWebFiltersOrder.AUTHORIZATION)
//                .exceptionHandling()
//                .authenticationEntryPoint((webFilterExchange, e) -> statusEntryPointHandler())

                .and()
                .addFilterAt(new JwtTokenAuthenticationFilter(jwtTokenProvider), SecurityWebFiltersOrder.HTTP_BASIC)
                .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonMapperCustomizer() {
        return builder -> {
            log.debug("Configure Jackson");
            builder.findModulesViaServiceLoader(true);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.filters(new SimpleFilterProvider().setFailOnUnknownId(false));
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.mixIn(News.class, NewsMixin.class);
        };
    }
}
