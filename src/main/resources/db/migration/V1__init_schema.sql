-- 탁구 대회 관리 시스템 초기 스키마 (MariaDB 10.6+ / H2 MariaDB 모드 공용)

CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    email       VARCHAR(120) NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE clubs (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    region      VARCHAR(100),
    description VARCHAR(1000),
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_clubs_name UNIQUE (name)
);

CREATE TABLE players (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    name          VARCHAR(50) NOT NULL,
    gender        VARCHAR(10) NOT NULL,
    birth_year    INT,
    division      INT,
    rating        INT         NOT NULL DEFAULT 1500,
    rated_matches INT         NOT NULL DEFAULT 0,
    phone         VARCHAR(30),
    club_id       BIGINT,
    user_id       BIGINT,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_players_user UNIQUE (user_id),
    CONSTRAINT fk_players_club FOREIGN KEY (club_id) REFERENCES clubs (id),
    CONSTRAINT fk_players_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_players_name ON players (name);
CREATE INDEX idx_players_rating ON players (rating);

CREATE TABLE tournaments (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    title                 VARCHAR(200)  NOT NULL,
    description           VARCHAR(4000),
    venue                 VARCHAR(200),
    start_at              DATETIME(6)   NOT NULL,
    end_at                DATETIME(6)   NOT NULL,
    registration_deadline DATETIME(6),
    status                VARCHAR(20)   NOT NULL,
    format                VARCHAR(30)   NOT NULL,
    entry_type            VARCHAR(20)   NOT NULL,
    best_of               INT           NOT NULL,
    max_entries           INT,
    group_size            INT           NOT NULL,
    advance_per_group     INT           NOT NULL,
    table_count           INT           NOT NULL,
    entry_fee             INT           NOT NULL DEFAULT 0,
    division_min          INT,
    division_max          INT,
    gender_restriction    VARCHAR(10)   NOT NULL,
    auto_confirm          BOOLEAN       NOT NULL DEFAULT FALSE,
    require_check_in      BOOLEAN       NOT NULL DEFAULT FALSE,
    rating_applied        BOOLEAN       NOT NULL DEFAULT FALSE,
    champion_entry_id     BIGINT,
    creator_id            BIGINT        NOT NULL,
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    version               BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tournaments_creator FOREIGN KEY (creator_id) REFERENCES users (id)
);
CREATE INDEX idx_tournaments_status ON tournaments (status);
CREATE INDEX idx_tournaments_start ON tournaments (start_at);

CREATE TABLE match_groups (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT      NOT NULL,
    name          VARCHAR(20) NOT NULL,
    ordinal       INT         NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_match_groups_ordinal UNIQUE (tournament_id, ordinal),
    CONSTRAINT fk_match_groups_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id)
);

CREATE TABLE entries (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT       NOT NULL,
    player_id     BIGINT       NOT NULL,
    partner_id    BIGINT,
    applicant_id  BIGINT,
    status        VARCHAR(20)  NOT NULL,
    checked_in    BOOLEAN      NOT NULL DEFAULT FALSE,
    paid          BOOLEAN      NOT NULL DEFAULT FALSE,
    seed          INT,
    group_id      BIGINT,
    memo          VARCHAR(200),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_entries_player UNIQUE (tournament_id, player_id),
    CONSTRAINT fk_entries_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id),
    CONSTRAINT fk_entries_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_entries_partner FOREIGN KEY (partner_id) REFERENCES players (id),
    CONSTRAINT fk_entries_applicant FOREIGN KEY (applicant_id) REFERENCES users (id),
    CONSTRAINT fk_entries_group FOREIGN KEY (group_id) REFERENCES match_groups (id)
);
CREATE INDEX idx_entries_tournament_status ON entries (tournament_id, status);

ALTER TABLE tournaments
    ADD CONSTRAINT fk_tournaments_champion FOREIGN KEY (champion_entry_id) REFERENCES entries (id);

CREATE TABLE matches (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT       NOT NULL,
    stage         VARCHAR(10)  NOT NULL,
    group_id      BIGINT,
    round_no      INT          NOT NULL,
    bracket_size  INT,
    bracket_pos   INT          NOT NULL,
    play_order    INT          NOT NULL,
    entry1_id     BIGINT,
    entry2_id     BIGINT,
    winner_id     BIGINT,
    next_match_id BIGINT,
    next_slot     INT,
    status        VARCHAR(20)  NOT NULL,
    result_type   VARCHAR(20),
    games1        INT,
    games2        INT,
    game_scores   VARCHAR(200),
    table_number  INT,
    started_at    DATETIME(6),
    finished_at   DATETIME(6),
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_matches_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id),
    CONSTRAINT fk_matches_group FOREIGN KEY (group_id) REFERENCES match_groups (id),
    CONSTRAINT fk_matches_entry1 FOREIGN KEY (entry1_id) REFERENCES entries (id),
    CONSTRAINT fk_matches_entry2 FOREIGN KEY (entry2_id) REFERENCES entries (id),
    CONSTRAINT fk_matches_winner FOREIGN KEY (winner_id) REFERENCES entries (id),
    CONSTRAINT fk_matches_next FOREIGN KEY (next_match_id) REFERENCES matches (id)
);
CREATE INDEX idx_matches_tournament_status ON matches (tournament_id, status);
CREATE INDEX idx_matches_tournament_stage ON matches (tournament_id, stage, round_no, bracket_pos);

CREATE TABLE rating_history (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    player_id     BIGINT      NOT NULL,
    tournament_id BIGINT      NOT NULL,
    before_rating INT         NOT NULL,
    after_rating  INT         NOT NULL,
    match_count   INT         NOT NULL,
    wins          INT         NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rating_history_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT fk_rating_history_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id)
);
CREATE INDEX idx_rating_history_player ON rating_history (player_id);

CREATE TABLE posts (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    category      VARCHAR(20)    NOT NULL,
    title         VARCHAR(200)   NOT NULL,
    content       VARCHAR(10000) NOT NULL,
    author_id     BIGINT         NOT NULL,
    view_count    INT            NOT NULL DEFAULT 0,
    like_count    INT            NOT NULL DEFAULT 0,
    comment_count INT            NOT NULL DEFAULT 0,
    pinned        BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id)
);
CREATE INDEX idx_posts_category ON posts (category, pinned, created_at);

CREATE TABLE post_likes (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    post_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_post_likes UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE comments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    post_id    BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(100) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    link       VARCHAR(200),
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_notifications_user ON notifications (user_id, is_read);
