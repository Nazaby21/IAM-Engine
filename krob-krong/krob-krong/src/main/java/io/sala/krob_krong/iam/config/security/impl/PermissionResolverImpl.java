package io.sala.krob_krong.iam.config.security.impl;

import io.sala.krob_krong.iam.config.security.PermissionResolver;
import io.sala.krob_krong.iam.entity.PermissionEntity;
import io.sala.krob_krong.iam.entity.RoleEntity;
import io.sala.krob_krong.iam.repository.RoleRepository;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionResolverImpl implements PermissionResolver {

    private final RoleRepository roleRepository;

    @Override
    public Set<String> effectivePermissionCodes(Collection<RoleEntity> roles) {
        Set<String> codes = new TreeSet<>();
        Set<String> visited = new HashSet<>();
        Deque<RoleEntity> stack = new ArrayDeque<>(roles);
        while (!stack.isEmpty()) {
            RoleEntity role = stack.pop();
            if (!visited.add(role.getId())) {
                continue;
            }
            for (PermissionEntity permission : role.getPermissions()) {
                codes.add(permission.getCode());
            }
            if (role.getParentRoleId() != null) {
                roleRepository.findById(role.getParentRoleId()).ifPresent(stack::push);
            }
        }
        return codes;
    }

    @Override
    public Set<String> toAuthorities(Collection<String> permissionCodes) {
        Set<String> authorities = new LinkedHashSet<>();
        for (String code : permissionCodes) {
            authorities.add(code + ":*");
        }
        return authorities;
    }

    @Override
    public int highestRank(Collection<RoleEntity> roles) {
        return roles.stream().mapToInt(RoleEntity::getRank).max().orElse(0);
    }
}
