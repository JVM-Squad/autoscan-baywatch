package fr.ght1pc9kc.baywatch.security.api.model;

import fr.ght1pc9kc.baywatch.common.api.exceptions.UnauthorizedException;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@UtilityClass
public final class RoleUtils {
    public static boolean hasRole(User user, @NotNull Role expectedRole) {
        Objects.requireNonNull(expectedRole);
        if (Objects.isNull(user)) {
            return false;
        }
        boolean hasRole = false;
        for (Role role : Role.values()) {
            if (role == user.role) {
                hasRole = true;
            }
            if (role == expectedRole) {
                return hasRole;
            }
        }
        return false;
    }

    public static User hasRoleOrThrow(User user, @NotNull Role expectedRole) {
        if (!hasRole(user, expectedRole)) {
            throw new UnauthorizedException(String.format("%s have no authority for getting raw feed !", user.login));
        }
        return user;
    }
}
