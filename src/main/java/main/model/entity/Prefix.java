package main.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "prefixs")
public class Prefix {
    @Id
    @Column(name="server_id", nullable = false)
    private String serverId;

    @Column(name="prefix", nullable = false)
    private String prefix;
}