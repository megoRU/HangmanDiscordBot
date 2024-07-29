package main.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "active_hangman")
public class ActiveHangman {

    @Id
    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;

    @Column(name = "players_list")
    private String playersList;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "channel_id")
    private Long channelId;

    @Column(name = "guild_id")
    private Long guildId;

    @Column(name = "word")
    private String word;

    @Column(name = "current_hidden_word")
    private String currentHiddenWord;

    @Column(name = "guesses")
    private String guesses;

    @Column(name = "hangman_errors", nullable = false)
    private Integer hangmanErrors;

    @Column(name = "game_created_time", nullable = false)
    private Timestamp gameCreatedTime;

    @Column(name = "is_competitive", nullable = false)
    private Boolean isCompetitive;

    @Column(name = "against_player_id")
    private Long againstPlayerId;

    @Column(name = "is_opponent_lose")
    private Boolean isOpponentLose;
}