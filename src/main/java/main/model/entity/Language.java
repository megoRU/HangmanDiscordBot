package main.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "language")
public final class Language {

    @Id
    @Column(name = "user_id_long", nullable = false)
    private Long userIdLong;

    @Column(name = "language", nullable = false)
    private String language;
}
