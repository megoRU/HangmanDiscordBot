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
@Table(name = "player")
public class Player {

    @Id
    @Column(nullable = false)
    private Integer id;

    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "games_id")
    private Game games_id;
}
