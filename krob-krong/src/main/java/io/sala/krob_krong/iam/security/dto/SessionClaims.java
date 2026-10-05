package io.sala.krob_krong.iam.security.dto;

import java.util.List;
import java.util.Set;
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
public class SessionClaims {
    private String userId;
    private String displayName;
    private String email;
    private boolean platformAdmin;
    private String tenantId;
    private List<String> rolesKeys;
    private Set<String> permissionAuthorities;
}
