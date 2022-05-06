package fr.ght1pc9kc.baywatch.scrapper.infra.config;

import fr.ght1pc9kc.baywatch.scrapper.api.NewsFilter;
import fr.ght1pc9kc.baywatch.scrapper.domain.filters.ImageLinkValidationFilter;
import fr.ght1pc9kc.baywatch.scrapper.domain.filters.NewsSanitizerFilter;
import fr.ght1pc9kc.baywatch.scrapper.domain.filters.OpenGraphFilter;
import fr.ght1pc9kc.baywatch.scrapper.domain.filters.RedditNewsFilter;
import fr.ght1pc9kc.scraphead.core.HeadScraper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NewsFiltersConfiguration {
    @Bean
    @Order(1)
    public NewsFilter redditNews() {
        return new RedditNewsFilter();
    }

    @Bean
    @Order(2)
    public NewsFilter openGraph(HeadScraper headScraper) {
        return new OpenGraphFilter(headScraper);
    }

    @Bean
    @Order(3)
    public NewsFilter imageLinkValidation(WebClient http) {
        return new ImageLinkValidationFilter(http);
    }

    @Bean
    @Order(4)
    public NewsFilter newsSanitizer() {
        return new NewsSanitizerFilter();
    }
}
