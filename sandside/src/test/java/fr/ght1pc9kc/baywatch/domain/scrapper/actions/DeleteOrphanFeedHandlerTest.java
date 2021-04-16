package fr.ght1pc9kc.baywatch.domain.scrapper.actions;

import fr.ght1pc9kc.baywatch.api.FeedService;
import fr.ght1pc9kc.baywatch.api.NewsService;
import fr.ght1pc9kc.baywatch.api.model.Feed;
import fr.ght1pc9kc.baywatch.domain.samples.FeedSamples;
import fr.ght1pc9kc.baywatch.domain.samples.NewsSamples;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeleteOrphanFeedHandlerTest {

    private DeleteOrphanFeedHandler tested;

    @BeforeEach
    void setUp() {
        FeedService feedServiceMock = mock(FeedService.class);
        when(feedServiceMock.raw(any())).thenReturn(Flux.fromIterable(FeedSamples.SAMPLES).map(Feed::getRaw));
        when(feedServiceMock.delete(anyCollection())).thenReturn(Mono.just(2));

        NewsService newsServiceMock = mock(NewsService.class);
        when(newsServiceMock.list(any())).thenReturn(Flux.fromIterable(NewsSamples.SAMPLES));
        when(newsServiceMock.orphanize(anyCollection())).thenReturn(Mono.just(1));
        when(newsServiceMock.delete(anyCollection())).thenReturn(Mono.just(1));

        tested = new DeleteOrphanFeedHandler(feedServiceMock, newsServiceMock);
    }

    @Test
    void should_delete_orphans_feed() {
        tested.before().block();
    }
}