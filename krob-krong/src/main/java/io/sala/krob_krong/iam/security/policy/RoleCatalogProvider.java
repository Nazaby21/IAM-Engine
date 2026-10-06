package io.sala.krob_krong.iam.security.policy;

import io.sala.krob_krong.iam.repository.PermissionRepository;
import io.sala.krob_krong.iam.repository.RoleGrantRuleRepository;
import io.sala.krob_krong.iam.repository.RolePermissionRepository;
import io.sala.krob_krong.iam.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Roles, scopes and grant rules are seed data changed only by migrations, so one load per process is enough.
@Component
@RequiredArgsConstructor
public class RoleCatalogProvider {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleGrantRuleRepository roleGrantRuleRepository;
    private final PermissionRepository permissionRepository;

    private volatile RoleCatalog catalog;

    public RoleCatalog get() {
        RoleCatalog current = catalog;
        if (current == null) {
            current = RoleCatalog.of(
                    roleRepository.findAll(),
                    rolePermissionRepository.findAll(),
                    roleGrantRuleRepository.findAll(),
                    permissionRepository.findAll());
            catalog = current;
        }
        return current;
    }
}
