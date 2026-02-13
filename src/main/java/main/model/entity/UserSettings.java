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
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_language", columnDefinition = "enum ('EN', 'RU'", nullable = false)
    private BotLanguage botLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_language", columnDefinition = "enum ('EN', 'RU'", nullable = false)
    private GameLanguage gameLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", columnDefinition = "enum ('FRUITS', 'FLOWERS', 'ALL', 'COLORS')", nullable = false)
    private Category category;

    public enum BotLanguage {
        RU,
        EN
    }

    public enum GameLanguage {
        RU,
        EN
    }

    public enum Category {
        FRUITS,
        FLOWERS,
        ALL,
        COLORS
    }
}