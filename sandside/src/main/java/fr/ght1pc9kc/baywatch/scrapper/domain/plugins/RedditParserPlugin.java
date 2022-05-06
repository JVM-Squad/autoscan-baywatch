package fr.ght1pc9kc.baywatch.scrapper.domain.plugins;

import fr.ght1pc9kc.baywatch.scrapper.api.FeedScrapperPlugin;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public final class RedditParserPlugin implements FeedScrapperPlugin {

    private static final URI REDDIT = URI.create("https://www.reddit.com");

    @Override
    public String pluginForDomain() {
        return REDDIT.getHost();
    }

    @Override
    public URI uriModifier(URI original) {
        if (original.getQuery() == null || original.getQuery().isBlank()) {
            return URI.create(original + "?sort=new");
        } else {
            return URI.create(original + "&sort=new");
        }
    }
}
