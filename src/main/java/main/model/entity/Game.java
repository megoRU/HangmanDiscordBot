package main.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "result", nullable = false)
    private Boolean result;

    @Column(name = "is_competitive", nullable = false)
    private Boolean isCompetitive;

    @Column(name = "game_date", nullable = false)
    private Instant gameDate;

    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;
}