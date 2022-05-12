-- MariaDB dump 10.19  Distrib 10.5.15-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: DiscordBotHangman
-- ------------------------------------------------------
-- Server version	10.5.15-MariaDB-0+deb11u1

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `active_hangman`
--

DROP TABLE IF EXISTS `active_hangman`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `active_hangman` (
  `user_id_long` bigint(30) NOT NULL,
  `message_id_long` bigint(30) NOT NULL,
  `channel_id_long` bigint(30) NOT NULL,
  `guild_long_id` bigint(30) DEFAULT NULL,
  `word` varchar(255) DEFAULT NULL,
  `current_hidden_word` varchar(255) DEFAULT NULL,
  `guesses` varchar(255) DEFAULT NULL,
  `hangman_errors` int(3) NOT NULL,
  `game_created_time` datetime(6) NOT NULL DEFAULT utc_timestamp(),
  PRIMARY KEY (`user_id_long`),
  UNIQUE KEY `user_id_long` (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `active_hangman`
--

LOCK TABLES `active_hangman` WRITE;
/*!40000 ALTER TABLE `active_hangman` DISABLE KEYS */;
/*!40000 ALTER TABLE `active_hangman` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `game_language`
--

DROP TABLE IF EXISTS `game_language`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `game_language` (
  `user_id_long` varchar(255) NOT NULL,
  `language` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id_long`),
  UNIQUE KEY `user_id_long` (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `games` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `result` tinyint(1) DEFAULT NULL,
  `game_date` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13672 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `language`
--

DROP TABLE IF EXISTS `language`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `language` (
  `user_id_long` varchar(255) NOT NULL,
  `language` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id_long`),
  UNIQUE KEY `user_id_long` (`user_id_long`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `player`
--

DROP TABLE IF EXISTS `player`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `player` (
  `id` int(11) NOT NULL,
  `games_id` int(11) DEFAULT NULL,
  `user_id_long` bigint(30) NOT NULL,
  KEY `games_id` (`games_id`),
  CONSTRAINT `player_ibfk_1` FOREIGN KEY (`games_id`) REFERENCES `games` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `prefixs`
--

DROP TABLE IF EXISTS `prefixs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `prefixs` (
  `server_id` varchar(255) NOT NULL,
  `prefix` varchar(255) NOT NULL,
  PRIMARY KEY (`server_id`),
  UNIQUE KEY `server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-05-12 14:05:17
