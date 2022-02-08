package fr.ght1pc9kc.baywatch.infra.security.adapters;

import fr.ght1pc9kc.baywatch.api.security.UserService;
import fr.ght1pc9kc.baywatch.domain.ports.AuthenticationFacade;
import fr.ght1pc9kc.baywatch.domain.ports.UserPersistencePort;
import fr.ght1pc9kc.baywatch.domain.security.UserServiceImpl;
import fr.ght1pc9kc.baywatch.infra.security.model.BaywatchUserDetails;
import fr.ght1pc9kc.juery.api.Criteria;
import fr.ght1pc9kc.juery.api.PageRequest;
import lombok.experimental.Delegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;

@Service
@Qualifier("Baywatch")
public class UserServiceAdapter implements UserService, ReactiveUserDetailsService {
    @Delegate
    private final UserService delegate;

    @Autowired
    public UserServiceAdapter(UserPersistencePort userPersistencePort, AuthenticationFacade authFacade) {
        this.delegate = new UserServiceImpl(userPersistencePort, authFacade, Clock.systemUTC());
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return delegate.list(PageRequest.one(Criteria.property("login").eq(username)))
                .next()
                .map(BaywatchUserDetails::new);
    }
}
