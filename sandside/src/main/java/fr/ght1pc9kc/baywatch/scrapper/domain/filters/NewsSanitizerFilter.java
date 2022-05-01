package fr.ght1pc9kc.baywatch.scrapper.domain.filters;

import fr.ght1pc9kc.baywatch.scrapper.api.NewsFilter;
import fr.ght1pc9kc.baywatch.techwatch.api.model.RawNews;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.html.HtmlStreamRenderer;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Slf4j
public class NewsSanitizerFilter implements NewsFilter {
    private static final int DESCRIPTION_MAX_LENGTH = 3_000;

    private static final Function<HtmlStreamEventReceiver, HtmlSanitizer.Policy> TITLE_POLICY =
            new HtmlPolicyBuilder().toFactory();
    private static final Function<HtmlStreamEventReceiver, HtmlSanitizer.Policy> DESCRIPTION_POLICY = new HtmlPolicyBuilder()
            .allowElements("a", "p", "ul", "li", "pre")
            .allowAttributes("href").onElements("a")
            .toFactory();


    @Override
    public Mono<RawNews> filter(@NotNull RawNews news) {
        String description = news.getDescription();
        String saneDescription = news.getDescription();
        if (!description.isBlank()) {
            String descrEllipsed = description.substring(0, Math.min(DESCRIPTION_MAX_LENGTH, description.length() - 1));
            StringBuilder descrHtml = new StringBuilder();
            HtmlStreamRenderer descrRenderer = HtmlStreamRenderer.create(descrHtml, invalid -> log.trace("Invalid tag detected in description {}", invalid));
            HtmlSanitizer.sanitize(HtmlUtils.htmlUnescape(descrEllipsed), DESCRIPTION_POLICY.apply(descrRenderer));
            saneDescription = HtmlUtils.htmlEscape(descrHtml.toString(), StandardCharsets.UTF_8.name());
        }

        String title = news.getTitle();
        String saneTitle = news.getTitle();
        if (!saneTitle.isBlank()) {
            StringBuilder ttlBuilder = new StringBuilder();
            HtmlStreamRenderer ttlRenderer = HtmlStreamRenderer.create(ttlBuilder, invalid -> log.trace("Invalid tag detected in title {}", invalid));
            HtmlSanitizer.sanitize(HtmlUtils.htmlUnescape(title), TITLE_POLICY.apply(ttlRenderer));
            saneTitle = ttlBuilder.toString();
        }

        return Mono.just(news.withTitle(saneTitle).withDescription(saneDescription));
    }
}
