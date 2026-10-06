package io.sala.krob_krong.iam.security.dto;

import io.sala.krob_krong.account.entity.RefreshTokenEntity;
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
