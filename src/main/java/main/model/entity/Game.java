package main.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
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

    @Column(name = "game_date", nullable = false)
    private Timestamp gameDate;
}