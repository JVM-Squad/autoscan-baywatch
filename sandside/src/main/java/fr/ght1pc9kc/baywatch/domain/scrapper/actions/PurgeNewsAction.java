package fr.ght1pc9kc.baywatch.domain.scrapper.actions;

import fr.ght1pc9kc.baywatch.api.model.RawNews;
import fr.ght1pc9kc.baywatch.api.model.request.PageRequest;
import fr.ght1pc9kc.baywatch.api.model.request.filter.Criteria;
import fr.ght1pc9kc.baywatch.api.scrapper.PreScrappingAction;
import fr.ght1pc9kc.baywatch.domain.ports.NewsPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.With;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurgeNewsAction implements PreScrappingAction {
    private static final int DELETE_BUFFER_SIZE = 500;
    private final NewsPersistencePort newsPersistence;

    @Setter
    @Accessors(fluent = true)
    private Clock clock = Clock.systemUTC();

    @Override
    public Mono<Void> call() {
        LocalDateTime maxPublicationPasDate = LocalDateTime.now(clock).minus(Period.ofMonths(3));
        Criteria criteria = Criteria.property("publication").lt(maxPublicationPasDate);
        return newsPersistence.list(PageRequest.all(criteria))
                .map(RawNews::getId)
                .collectList()
                .flatMapMany(this::keepStaredNewsIds)
                .buffer(DELETE_BUFFER_SIZE)
                .flatMap(newsPersistence::delete)
                .onErrorContinue((t, o) -> {
                    log.error("{}: {}", t.getCause(), t.getLocalizedMessage());
                    log.debug("STACKTRACE", t);
                }).then();
    }

    private Flux<String> keepStaredNewsIds(Collection<String> newsIds) {
        Criteria isStaredCriteria = Criteria.property("newsId").in(newsIds)
                .and(Criteria.property("stared").eq(true));
        return newsPersistence.listState(isStaredCriteria)
                .map(Map.Entry::getKey)
                .collectList()
                .flatMapMany(stareds -> {
                    Collection<String> toBeDeleted = new ArrayList<>(newsIds);
                    toBeDeleted.removeAll(stareds);
                    return Flux.fromIterable(toBeDeleted);
                });
    }

}
