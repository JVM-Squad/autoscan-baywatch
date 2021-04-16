package fr.ght1pc9kc.baywatch.domain.ports;

import fr.ght1pc9kc.baywatch.api.model.Flags;
import fr.ght1pc9kc.baywatch.api.model.News;
import fr.ght1pc9kc.baywatch.api.model.State;
import fr.ght1pc9kc.baywatch.api.model.request.PageRequest;
import fr.ght1pc9kc.baywatch.api.model.request.filter.Criteria;
import org.intellij.lang.annotations.MagicConstant;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map.Entry;

public interface NewsPersistencePort {
    Mono<News> get(String id);

    default Flux<News> list() {
        return list(PageRequest.all());
    }

    Flux<News> list(PageRequest pageRequest);

    Mono<Void> persist(Collection<News> toCreate);

    Flux<Entry<String, State>> listState(Criteria searchCriteria);

    Mono<Integer> addStateFlag(
            String newsId, String userId, @MagicConstant(flagsFromClass = Flags.class) int flag);

    Mono<Integer> removeStateFlag(
            String newsId, String userId, @MagicConstant(flagsFromClass = Flags.class) int flag);

    Mono<Integer> deleteFeedLink(Collection<String> ids);

    Mono<Integer> delete(Collection<String> ids);

    Mono<Integer> count();
}
