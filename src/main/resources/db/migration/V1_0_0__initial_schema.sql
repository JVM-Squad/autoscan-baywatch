CREATE TABLE FEEDS
(
    FEED_ID         VARCHAR(64)  NOT NULL PRIMARY KEY,
    FEED_NAME       VARCHAR(60),
    FEED_URL        VARCHAR(500) NOT NULL,
    FEED_LAST_WATCH DATETIME
);

CREATE TABLE NEWS
(
    NEWS_ID          VARCHAR(64)  NOT NULL PRIMARY KEY,
    NEWS_TITLE       VARCHAR(250) NOT NULL,
    NEWS_DESCRIPTION TEXT,
    NEWS_PUBLICATION DATETIME     NOT NULL,
    NEWS_LINK        VARCHAR(500) NOT NULL
);

CREATE TABLE USERS
(
    USER_ID    VARCHAR(64) NOT NULL PRIMARY KEY,
    USER_LOGIN VARCHAR(50) NOT NULL,
    USER_EMAIL VARCHAR(250),
    USER_NAME  VARCHAR(250)
);

CREATE TABLE NEWS_FEEDS
(
    NEFE_NEWS_ID VARCHAR(64) NOT NULL,
    NEFE_FEED_ID VARCHAR(64) NOT NULL,

    CONSTRAINT PK_NEWS_FEEDS PRIMARY KEY (NEFE_NEWS_ID, NEFE_FEED_ID),
    CONSTRAINT FK_NEFE_NEWS_ID FOREIGN KEY (NEFE_NEWS_ID) REFERENCES NEWS (NEWS_ID),
    CONSTRAINT FK_NEFE_FEED_ID FOREIGN KEY (NEFE_FEED_ID) REFERENCES FEEDS (FEED_ID)
);

CREATE TABLE FEEDS_USERS
(
    FEUS_FEED_ID VARCHAR(64) NOT NULL,
    FEUS_USER_ID VARCHAR(64) NOT NULL,

    CONSTRAINT PK_FEEDS_USER PRIMARY KEY (FEUS_FEED_ID, FEUS_USER_ID),
    CONSTRAINT FK_FEUS_FEED_ID FOREIGN KEY (FEUS_FEED_ID) REFERENCES FEEDS (FEED_ID),
    CONSTRAINT FK_FEUS_USER_ID FOREIGN KEY (FEUS_USER_ID) REFERENCES USERS (USER_ID)
);

CREATE TABLE NEWS_USER_STATE
(
    NURS_NEWS_ID VARCHAR(64) NOT NULL,
    NURS_USER_ID VARCHAR(64) NOT NULL,
    NURS_STATE   INTEGER     NOT NULL,

    CONSTRAINT PK_NEWS_USER_STATE PRIMARY KEY (NURS_NEWS_ID, NURS_USER_ID),
    CONSTRAINT FK_NURS_NEWS_ID FOREIGN KEY (NURS_NEWS_ID) REFERENCES NEWS (NEWS_ID)
);

INSERT INTO FEEDS
VALUES ('076b66c63e571578374ffa8e3197554cd185c6911658747e52373358e938b5cf', 'Test',
        'https://www.clubic.com/feed/news.rss', null),
       ('4ebc63c73907d3c10b7e672b0b6857f8c53ebc3b6a5eda8104dff5af1cbebf41', 'Reddit',
        'https://www.reddit.com/r/programming/top/.rss', null);
