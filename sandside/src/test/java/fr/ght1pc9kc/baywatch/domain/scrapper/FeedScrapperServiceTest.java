package fr.ght1pc9kc.baywatch.domain.scrapper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.machinezoo.noexception.Exceptions;
import fr.ght1pc9kc.baywatch.domain.ports.FeedPersistencePort;
import fr.ght1pc9kc.baywatch.domain.ports.NewsPersistencePort;
import fr.ght1pc9kc.baywatch.api.scrapper.RssAtomParser;
import fr.ght1pc9kc.baywatch.api.model.*;
import fr.ght1pc9kc.baywatch.domain.scrapper.opengraph.OpenGraphScrapper;
import fr.ght1pc9kc.baywatch.domain.utils.Hasher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import wiremock.org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class FeedScrapperServiceTest {

    static final WireMockServer mockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    private FeedScrapperService tested;

    private OpenGraphScrapper openGraphScrapper;
    private NewsPersistencePort newsPersistenceMock;
    private RssAtomParser rssAtomParserMock;
    private FeedPersistencePort feedPersistenceMock;

    @Test
    void should_start_multi_scrapper() {
        tested.startScrapping();
        tested.shutdownScrapping();

        mockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/feeds/journal_du_hacker.xml")));
        mockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/feeds/spring-blog.xml")));
        verify(rssAtomParserMock, times(2)).parse(any(Feed.class), any());
        verify(newsPersistenceMock,
                times(1).description("Expect only one call because of the buffer to 100")
        ).persist(anyCollection());
    }

    @Test
    void should_fail_scrapper_without_fail_scrapping() {
        URI darthVaderUri = URI.create("http://localhost:" + mockServer.port() + "/error/darth-vader.xml");
        URI springUri = URI.create("http://localhost:" + mockServer.port() + "/feeds/spring-blog.xml");
        String darthVaderSha3 = Hasher.sha3(darthVaderUri.toString());
        String springSha3 = Hasher.sha3(springUri.toString());

        when(feedPersistenceMock.list()).thenReturn(Flux.just(
                Feed.builder().raw(RawFeed.builder()
                        .id(darthVaderSha3)
                        .name("fail")
                        .url(darthVaderUri)
                        .build()).build(),
                Feed.builder().raw(RawFeed.builder()
                        .id(springSha3)
                        .name("Spring")
                        .url(springUri)
                        .build()).build()
        ));

        tested.startScrapping();
        tested.shutdownScrapping();

        mockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/error/darth-vader.xml")));
        mockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/feeds/spring-blog.xml")));
        verify(rssAtomParserMock, times(2)).parse(any(Feed.class), any());
        verify(newsPersistenceMock,
                times(1).description("Expect only one call because of the buffer to 100")
        ).persist(anyCollection());
    }

    @Test
    void should_fail_on_persistence() {
        reset(newsPersistenceMock);
        when(newsPersistenceMock.persist(anyCollection())).thenReturn(Mono.error(new RuntimeException()).then());

        tested.startScrapping();
        tested.shutdownScrapping();

        mockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/error/darth-vader.xml")));
        mockServer.verify(WireMock.getRequestedFor(WireMock.urlEqualTo("/feeds/spring-blog.xml")));
        verify(rssAtomParserMock, times(2)).parse(any(Feed.class), any());
    }

    @BeforeAll
    static void stubAllMockServerRoute() {
        mockServer.stubFor(
                WireMock.get(WireMock.urlMatching("/feeds/.*\\.xml"))
                        .willReturn(WireMock.aResponse()
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_ATOM_XML_VALUE)
                                .withStatus(HttpStatus.OK.value())
                                .withBody("Obiwan Kenobi"))
        );
        mockServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/error/darth-vader.xml"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(HttpStatus.NOT_FOUND.value())));

        mockServer.start();
    }

    @BeforeEach
    void setUp() {
        newsPersistenceMock = mock(NewsPersistencePort.class);
        when(newsPersistenceMock.persist(anyCollection())).thenReturn(Mono.just("").then());

        rssAtomParserMock = spy(new RssAtomParser() {
            @Override
            public Flux<News> parse(Feed feed, InputStream is) {
                // Must consume the inputstream
                Exceptions.wrap().get(() -> IOUtils.toByteArray(is));
                return Flux.just(News.builder()
                        .raw(RawNews.builder()
                                .id(UUID.randomUUID().toString())
                                .link(URI.create("https://practicalprogramming.fr/dbaas-la-base-de-donnees-dans-le-cloud/"))
                                .build())
                        .state(State.NONE)
                        .build());
            }
        });

        URI springUri = URI.create("http://localhost:" + mockServer.port() + "/feeds/spring-blog.xml");
        URI jdhUri = URI.create("http://localhost:" + mockServer.port() + "/feeds/journal_du_hacker.xml");
        String springSha3 = Hasher.sha3(springUri.toString());
        String jdhSha3 = Hasher.sha3(jdhUri.toString());

        openGraphScrapper = mock(OpenGraphScrapper.class);
        feedPersistenceMock = mock(FeedPersistencePort.class);
        when(feedPersistenceMock.list()).thenReturn(Flux.just(
                Feed.builder().raw(RawFeed.builder()
                        .id(jdhSha3)
                        .name("Reddit")
                        .url(jdhUri)
                        .build()).build(),
                Feed.builder().raw(RawFeed.builder()
                        .id(springSha3)
                        .name("Spring")
                        .url(springUri)
                        .build()).build()
        ));

        tested = new FeedScrapperService(Clock.systemUTC(),
                openGraphScrapper, feedPersistenceMock, newsPersistenceMock, rssAtomParserMock, Collections.emptyList());
    }

    @AfterAll
    static void afterAll() {
        mockServer.stop();
    }
}