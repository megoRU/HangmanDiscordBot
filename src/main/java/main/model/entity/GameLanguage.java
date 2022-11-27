package main.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "game_language")
public class GameLanguage {

    @Id
    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;

    @Column(name = "language", nullable = false)
    private String language;
}