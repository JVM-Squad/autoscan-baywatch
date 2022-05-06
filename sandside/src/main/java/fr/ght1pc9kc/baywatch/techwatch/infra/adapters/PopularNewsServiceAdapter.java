package fr.ght1pc9kc.baywatch.techwatch.infra.adapters;

import fr.ght1pc9kc.baywatch.techwatch.api.PopularNewsService;
import fr.ght1pc9kc.baywatch.techwatch.domain.PopularNewsServiceImpl;
import fr.ght1pc9kc.baywatch.techwatch.infra.persistence.StateRepository;
import lombok.experimental.Delegate;
import org.springframework.stereotype.Service;

@Service
public class PopularNewsServiceAdapter implements PopularNewsService {
    @Delegate
    private final PopularNewsService delegate;

    public PopularNewsServiceAdapter(StateRepository stateRepository) {
        this.delegate = new PopularNewsServiceImpl(stateRepository);
    }
}
