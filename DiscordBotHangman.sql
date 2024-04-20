-- MariaDB dump 10.19  Distrib 10.11.6-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: DiscordBotHangman
-- ------------------------------------------------------
-- Server version	10.11.6-MariaDB-0+deb12u1

DROP TABLE IF EXISTS `active_hangman`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `active_hangman` (
  `user_id_long` bigint(30) NOT NULL,
  `second_user_id_long` bigint(30) DEFAULT NULL,
  `message_id_long` bigint(30) NOT NULL,
  `channel_id_long` bigint(30) NOT NULL,
  `guild_long_id` bigint(30) DEFAULT NULL,
  `word` varchar(255) DEFAULT NULL,
  `current_hidden_word` varchar(255) DEFAULT NULL,
  `guesses` varchar(255) DEFAULT NULL,
  `hangman_errors` int(3) NOT NULL,
  `game_created_time` datetime(6) NOT NULL DEFAULT utc_timestamp(),
  `against_player_id` bigint(20) DEFAULT NULL,
  `is_competitive` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`user_id_long`),
  UNIQUE KEY `user_id_long` (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `competitive_queue`
--

DROP TABLE IF EXISTS `competitive_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `competitive_queue` (
  `user_id_long` bigint(20) NOT NULL,
  `game_language` enum('EN','RU') NOT NULL DEFAULT 'EN',
  `message_channel` bigint(20) NOT NULL,
  PRIMARY KEY (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `games` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `result` tinyint(1) NOT NULL,
  `game_date` datetime NOT NULL,
  `user_id_long` bigint(30) NOT NULL,
  `is_competitive` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38347 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_settings`
--

DROP TABLE IF EXISTS `user_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_settings` (
  `user_id_long` bigint(20) NOT NULL,
  `bot_language` enum('EN','RU') NOT NULL DEFAULT 'EN',
  `game_language` enum('EN','RU') NOT NULL DEFAULT 'EN',
  `category` enum('FRUITS','FLOWERS','ALL','COLORS') NOT NULL DEFAULT 'ALL',
  PRIMARY KEY (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
