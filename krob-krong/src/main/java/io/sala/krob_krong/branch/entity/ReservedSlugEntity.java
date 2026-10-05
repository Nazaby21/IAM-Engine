package io.sala.krob_krong.branch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reserved_slug")
public class ReservedSlugEntity {

    @Id
    private String slug;
}
