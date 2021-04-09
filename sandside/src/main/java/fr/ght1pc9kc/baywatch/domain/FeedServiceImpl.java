package fr.ght1pc9kc.baywatch.domain;

import fr.ght1pc9kc.baywatch.api.FeedService;
import fr.ght1pc9kc.baywatch.api.model.Feed;
import fr.ght1pc9kc.baywatch.api.model.request.PageRequest;
import fr.ght1pc9kc.baywatch.api.model.request.filter.Criteria;
import fr.ght1pc9kc.baywatch.domain.exceptions.UnauthenticatedUser;
import fr.ght1pc9kc.baywatch.domain.ports.AuthenticationFacade;
import fr.ght1pc9kc.baywatch.domain.ports.FeedPersistencePort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FeedPersistencePort feedRepository;
    private final AuthenticationFacade authFacade;

    @Override
    public Mono<Feed> get(String id) {
        return feedRepository.get(id);
    }

    @Override
    public Flux<Feed> list() {
        return list(PageRequest.all());
    }

    @Override
    public Flux<Feed> list(PageRequest pageRequest) {
        return authFacade.getConnectedUser()
                .switchIfEmpty(Mono.error(new UnauthenticatedUser("Authentication not found !")))
                .map(u -> pageRequest.and(Criteria.property("userId").eq(u.id)))
                .onErrorResume(UnauthenticatedUser.class, (e) -> Mono.just(pageRequest))
                .flatMapMany(feedRepository::list);
    }

    @Override
    public Mono<Void> persist(Collection<Feed> toPersist) {
        return null;
    }

    @Override
    public Mono<Integer> delete(Collection<String> toDelete) {
        return null;
    }
}
