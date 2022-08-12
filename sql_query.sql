INSERT INTO games(id, result, game_date)
VALUES (1, true, '2021-05-23 14:25:10');
SELECT *
FROM player,
     games
WHERE player.games_id = games.id;
SELECT *
FROM player,
     games
WHERE player.user_id_long = '250699265389625347'
  AND player.games_id = games.id;


SELECT COUNT(games_id)                             AS COUNT_GAMES,
       SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS,
       SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES

FROM player,
     games
WHERE player.user_id_long = '250699265389625347'
  AND player.games_id = games.id;



#
сделано:
CREATE TABLE games
(
    id        int NOT NULL AUTO_INCREMENT,
    result    BOOLEAN,
    game_date DATETIME,
    PRIMARY KEY (id)
);

CREATE TABLE player
(
    `user_id_long` bigint(30) NOT NULL,
    games_id       int,
    FOREIGN KEY (games_id) REFERENCES games (id)
);


-- Сделано:
CREATE TABLE `ActiveHangman`
(
    `user_id_long`        bigint(30) NOT NULL,
    `message_id_long`     bigint(30) NOT NULL,
    `channel_id_long`     bigint(30) NOT NULL,
    `guild_long_id`       bigint(30) NOT NULL,
    `word`                varchar(255),
    `current_hidden_word` varchar(255),
    `guesses`             varchar(255),
    `hangman_errors`      int(3) NOT NULL,
    `game_created_time`   DATETIME(6),
    PRIMARY KEY (`user_id_long`),
    UNIQUE KEY `user_id_long` (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `language`
(
    `user_id_long` varchar(255) NOT NULL,
    `language`     varchar(255) NOT NULL,
    PRIMARY KEY (user_id_long),
    UNIQUE KEY `user_id_long` (user_id_long)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `game_mode`
(
    `user_id_long` varchar(255) NOT NULL,
    `mode`         varchar(255) NOT NULL,
    PRIMARY KEY (user_id_long),
    UNIQUE KEY `user_id_long` (user_id_long)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


SELECT COUNT(games_id)                             AS COUNT_GAMES,
       user_id_long                                AS USER_ID_LONG,
       SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS,
       SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES
FROM player,
     games
WHERE player.user_id_long = 575694550517940245
  AND player.games_id = games.id

UNION

SELECT COUNT(games_id)                             AS COUNT_GAMES,
       user_id_long                                AS USER_ID_LONG,
       SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) AS TOTAL_ZEROS,
       SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) AS TOTAL_ONES
FROM player,
     games
WHERE player.user_id_long = 250699265389625347
  AND player.games_id = games.id