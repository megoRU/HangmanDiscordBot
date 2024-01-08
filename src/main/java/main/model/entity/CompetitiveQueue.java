package main.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "competitive_queue")
public class CompetitiveQueue {

    @Id
    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_language", columnDefinition = "enum ('EN', 'RU'")
    private UserSettings.GameLanguage gameLanguage;

    @Column(name = "message_channel", nullable = false)
    private Long messageChannel;
}