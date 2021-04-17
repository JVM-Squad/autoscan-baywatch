package fr.ght1pc9kc.baywatch.api;

import fr.ght1pc9kc.baywatch.api.model.Feed;
import fr.ght1pc9kc.baywatch.api.model.RawFeed;
import fr.ght1pc9kc.baywatch.api.model.request.PageRequest;
import fr.ght1pc9kc.baywatch.api.security.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface FeedService {
    Mono<Feed> get(String id);

    Flux<Feed> list();

    Flux<Feed> list(PageRequest pageRequest);

    /**
     * List {@link Feed} independently of the {@link User} or any other entity.
     *
     * @param pageRequest The query parameters
     * @return The {@link RawFeed} version of the {@link Feed}
     */
    Flux<RawFeed> raw(PageRequest pageRequest);

    Mono<Void> persist(Collection<Feed> toPersist);

    Mono<Integer> delete(Collection<String> toDelete);
}
