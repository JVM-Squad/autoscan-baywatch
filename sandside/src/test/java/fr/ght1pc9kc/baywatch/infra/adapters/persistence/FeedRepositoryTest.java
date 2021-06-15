package fr.ght1pc9kc.baywatch.infra.adapters.persistence;

import fr.ght1pc9kc.baywatch.api.model.Feed;
import fr.ght1pc9kc.baywatch.api.model.RawFeed;
import fr.ght1pc9kc.baywatch.domain.techwatch.model.QueryContext;
import fr.ght1pc9kc.baywatch.domain.utils.Hasher;
import fr.ght1pc9kc.baywatch.dsl.tables.records.FeedsRecord;
import fr.ght1pc9kc.baywatch.dsl.tables.records.FeedsUsersRecord;
import fr.ght1pc9kc.baywatch.infra.mappers.BaywatchMapper;
import fr.ght1pc9kc.baywatch.infra.mappers.DateUtils;
import fr.ght1pc9kc.baywatch.infra.samples.FeedRecordSamples;
import fr.ght1pc9kc.baywatch.infra.samples.NewsRecordSamples;
import fr.ght1pc9kc.baywatch.infra.samples.UsersRecordSamples;
import fr.ght1pc9kc.juery.api.Criteria;
import fr.irun.testy.core.extensions.ChainedExtension;
import fr.irun.testy.jooq.WithDatabaseLoaded;
import fr.irun.testy.jooq.WithDslContext;
import fr.irun.testy.jooq.WithInMemoryDatasource;
import fr.irun.testy.jooq.WithSampleDataLoaded;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mapstruct.factory.Mappers;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static fr.ght1pc9kc.baywatch.api.model.EntitiesProperties.FEED_ID;
import static fr.ght1pc9kc.baywatch.dsl.tables.Feeds.FEEDS;
import static fr.ght1pc9kc.baywatch.dsl.tables.FeedsUsers.FEEDS_USERS;
import static fr.ght1pc9kc.baywatch.dsl.tables.NewsFeeds.NEWS_FEEDS;
import static fr.ght1pc9kc.baywatch.infra.samples.UsersRecordSamples.OKENOBI;
import static org.assertj.core.api.Assertions.assertThat;

class FeedRepositoryTest {
    private static final WithInMemoryDatasource wDs = WithInMemoryDatasource.builder().build();
    private static final WithDatabaseLoaded wBaywatchDb = WithDatabaseLoaded.builder()
            .setDatasourceExtension(wDs)
            .useFlywayDefaultLocation()
            .build();
    private static final WithDslContext wDslContext = WithDslContext.builder()
            .setDatasourceExtension(wDs).build();
    private static final WithSampleDataLoaded wSamples = WithSampleDataLoaded.builder(wDslContext)
            .addDataset(FeedRecordSamples.SAMPLE)
            .addDataset(UsersRecordSamples.SAMPLE)
            .addDataset(NewsRecordSamples.SAMPLE)
            .addDataset(NewsRecordSamples.NewsFeedsRecordSample.SAMPLE)
            .addDataset(FeedRecordSamples.FeedUserRecordSamples.SAMPLE)
            .build();

    @RegisterExtension
    @SuppressWarnings("unused")
    static ChainedExtension chain = ChainedExtension.outer(wDs)
            .append(wBaywatchDb)
            .append(wDslContext)
            .append(wSamples)
            .register();

    private FeedRepository tested;

    @BeforeEach
    void setUp(DSLContext dslContext) {
        BaywatchMapper baywatchMapper = Mappers.getMapper(BaywatchMapper.class);
        tested = new FeedRepository(Schedulers.immediate(), dslContext, baywatchMapper);
    }

    @Test
    void should_get_user_feed() {
        FeedsRecord expected = FeedRecordSamples.SAMPLE.records().get(0);
        Feed actual = tested.get(QueryContext.id(expected.getFeedId())).block();

        assertThat(actual).isEqualTo(Feed.builder()
                .raw(RawFeed.builder()
                        .id(expected.getFeedId())
                        .url(URI.create(expected.getFeedUrl()))
                        .name(expected.getFeedName())
                        .lastWatch(DateUtils.toInstant(expected.getFeedLastWatch()))
                        .build())
                .tags(Set.of())
                .build());
    }

    @Test
    void should_list_all_feeds(WithSampleDataLoaded.Tracker dbTracker) {
        dbTracker.skipNextSampleLoad();
        List<Feed> actuals = tested.list().collectList().block();
        assertThat(actuals).isNotEmpty();
    }

    @Test
    void should_manage_backpressure(WithSampleDataLoaded.Tracker dbTracker) {
        dbTracker.skipNextSampleLoad();
        List<Feed> actuals = tested.list().limitRate(2).collectList().block();
        assertThat(actuals).hasSize(FeedRecordSamples.SAMPLE.records().size());
    }

    @Test
    void should_persist_feeds(DSLContext dsl) {
        URI uri = URI.create("https://obiwan.kenobi.jedi/.rss");
        Feed expected = Feed.builder()
                .raw(RawFeed.builder()
                        .id(Hasher.identify(uri))
                        .url(uri)
                        .name("Obiwan Kenobi")
                        .build())
                .tags(Set.of("jedi", "light"))
                .build();

        tested.persist(Collections.singleton(expected)).block();

        FeedsRecord actual = dsl.selectFrom(FEEDS).where(FEEDS.FEED_ID.eq(expected.getId())).fetchOne();
        assertThat(actual).isNotNull();
        assertThat(actual.getFeedName()).isEqualTo(expected.getName());
    }

    @Test
    void should_persist_feeds_to_user(DSLContext dsl) {
        URI uri = URI.create("https://obiwan.kenobi.jedi/.rss");
        Feed expected = Feed.builder()
                .raw(RawFeed.builder()
                        .id(Hasher.identify(uri))
                        .url(uri)
                        .name("Obiwan Kenobi")
                        .build())
                .tags(Set.of("jedi", "light"))
                .build();

        tested.persist(Collections.singleton(expected), OKENOBI.getUserId()).block();

        {
            FeedsRecord actual = dsl.selectFrom(FEEDS).where(FEEDS.FEED_ID.eq(expected.getId())).fetchOne();
            assertThat(actual).isNotNull();
            assertThat(actual.getFeedName()).isEqualTo(expected.getName());
        }
        {
            FeedsUsersRecord actual = dsl.selectFrom(FEEDS_USERS).where(FEEDS_USERS.FEUS_FEED_ID.eq(expected.getId()))
                    .fetchOne();
            assertThat(actual).isNotNull();
            assertThat(actual.getFeusUserId()).isEqualTo(OKENOBI.getUserId());
            assertThat(actual.getFeusTags()).isEqualTo(String.join(",", expected.getTags()));
        }
    }

    @Test
    void should_delete_feeds(DSLContext dsl) {
        String feedOwnedByObiwanAndSkywalker = Hasher.identify(FeedRecordSamples.JEDI_BASE_URI.resolve("01"));
        String feedOwnedOnlyByObywan = Hasher.identify(FeedRecordSamples.JEDI_BASE_URI.resolve("03"));
        List<String> ids = List.of(feedOwnedByObiwanAndSkywalker, feedOwnedOnlyByObywan);

        {
            int countUser = dsl.fetchCount(FEEDS_USERS, FEEDS_USERS.FEUS_FEED_ID.in(ids));
            assertThat(countUser).isEqualTo(3);
        }

        tested.delete(QueryContext.all(Criteria.property(FEED_ID).in(ids))).block();

        {
            int countUser = dsl.fetchCount(FEEDS_USERS, FEEDS_USERS.FEUS_FEED_ID.in(ids));
            assertThat(countUser).isEqualTo(0);
        }
    }

    @Test
    void should_delete_feeds_for_user(DSLContext dsl) {
        String feedOwnedByObiwanAndSkywalker = Hasher.identify(FeedRecordSamples.JEDI_BASE_URI.resolve("01"));
        String feedOwnedOnlyByObywan = Hasher.identify(FeedRecordSamples.JEDI_BASE_URI.resolve("03"));
        List<String> ids = List.of(feedOwnedByObiwanAndSkywalker, feedOwnedOnlyByObywan);

        // Clean news -> feed link
        dsl.deleteFrom(NEWS_FEEDS).where(NEWS_FEEDS.NEFE_FEED_ID.in(ids)).execute();

        {
            int countUser = dsl.fetchCount(FEEDS_USERS, FEEDS_USERS.FEUS_FEED_ID.in(ids)
                    .and(FEEDS_USERS.FEUS_USER_ID.eq(OKENOBI.getUserId())));
            assertThat(countUser).isEqualTo(2);
        }

        tested.delete(QueryContext.builder()
                .filter(Criteria.property(FEED_ID).in(ids))
                .userId(OKENOBI.getUserId())
                .build()).block();

        {
            int countUser = dsl.fetchCount(FEEDS_USERS, FEEDS_USERS.FEUS_FEED_ID.in(ids));
            assertThat(countUser)
                    .describedAs("Feed 02 was remove, feed 01, owned by LSKYWALKER was kept")
                    .isEqualTo(1);
        }
    }

    @Test
    void should_list_orphan_feed() {
        List<Feed> actuals = tested.list(QueryContext.all(Criteria.property(FEED_ID).isNull())).collectList().block();
        assertThat(actuals).extracting(Feed::getId).containsExactly("a6e68b72e99e2e9e54258d98f175f504ad128bf2142597815c6c116f1925411c");
    }
}