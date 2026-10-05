package io.sala.krob_krong.iam.dto.security;

import io.sala.krob_krong.iam.entity.RefreshTokenEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuedRefresh {
    private String rawValue;
    private RefreshTokenEntity entity;
}
