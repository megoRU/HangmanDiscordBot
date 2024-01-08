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
@Table(name = "games")
public class Game {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "result", nullable = false)
    private Boolean result;

    @Column(name = "is_competitive", nullable = false)
    private Boolean isCompetitive;

    @Column(name = "game_date", nullable = false)
    private Timestamp gameDate;

    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;
}