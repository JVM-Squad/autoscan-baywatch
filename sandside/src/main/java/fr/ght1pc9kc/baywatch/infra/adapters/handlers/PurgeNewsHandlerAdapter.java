package fr.ght1pc9kc.baywatch.infra.adapters.handlers;

import fr.ght1pc9kc.baywatch.api.scrapper.ScrappingHandler;
import fr.ght1pc9kc.baywatch.domain.ports.NewsPersistencePort;
import fr.ght1pc9kc.baywatch.domain.scrapper.actions.PurgeNewsHandler;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurgeNewsHandlerAdapter implements ScrappingHandler {
    @Delegate
    private final PurgeNewsHandler delegate;

    @Autowired
    public PurgeNewsHandlerAdapter(NewsPersistencePort newsPersistencePort) {
        this.delegate = new PurgeNewsHandler(newsPersistencePort);
    }
}
