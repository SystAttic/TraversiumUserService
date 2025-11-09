-- Create user_table
CREATE TABLE user_table
(
    user_id                BIGSERIAL PRIMARY KEY,
    username               VARCHAR(255) UNIQUE,
    email                  VARCHAR(255) UNIQUE,
    description            TEXT,
    display_name           VARCHAR(255),
    avatar_photo_reference VARCHAR(255),
    cover_photo_reference  VARCHAR(255),
    firebase_id            VARCHAR(255) UNIQUE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_name             VARCHAR(255),
    last_name              VARCHAR(255),
    country_of_origin      VARCHAR(255),
    gender                 VARCHAR(255),
    deleted                BOOLEAN                  NOT NULL DEFAULT FALSE
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_user_username ON user_table (username);
CREATE INDEX idx_user_email ON user_table (email);
CREATE INDEX idx_user_firebase_id ON user_table (firebase_id);
CREATE INDEX idx_user_deleted ON user_table (deleted);

-- Create user_followers junction table
CREATE TABLE user_followers
(
    follower_id BIGINT NOT NULL,
    followed_id BIGINT NOT NULL,
    PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES user_table (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_followed FOREIGN KEY (followed_id) REFERENCES user_table (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_follow CHECK (follower_id != followed_id
)
    );

-- Create indexes for user_followers
CREATE INDEX idx_followers_follower ON user_followers (follower_id);
CREATE INDEX idx_followers_followed ON user_followers (followed_id);

-- Create blocked junction table
CREATE TABLE blocked
(
    user_id          BIGINT NOT NULL,
    blocked_user__id BIGINT NOT NULL,
    PRIMARY KEY (user_id, blocked_user__id),
    CONSTRAINT fk_blocking_user FOREIGN KEY (user_id) REFERENCES user_table (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_user FOREIGN KEY (blocked_user__id) REFERENCES user_table (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_no_self_block CHECK (user_id != blocked_user__id
)
    );

-- Create indexes for blocked
CREATE INDEX idx_blocked_user ON blocked (user_id);
CREATE INDEX idx_blocked_blocked_user ON blocked (blocked_user__id);