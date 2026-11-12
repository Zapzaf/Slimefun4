package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.attribute.Attribute;

/**
 * Minecraft 1.21.3 renamed {@link Attribute} constants, dropping the {@literal "GENERIC_"} prefix
 * (e.g. {@literal GENERIC_MAX_HEALTH} became {@literal MAX_HEALTH}).
 * Minecraft 26.x only has the new names.
 * <p>
 * This class resolves the correct constant at runtime, so the same code works
 * on 1.21.x and 26.x regardless of which API version it was compiled against.
 */
public final class VersionedAttribute {

    /**
     * The maximum health of a living entity.
     */
    public static final Attribute MAX_HEALTH = getKey("MAX_HEALTH", "GENERIC_MAX_HEALTH");

    private VersionedAttribute() {}

    @Nullable
    private static Attribute getKey(@Nonnull String... keys) {
        for (String key : keys) {
            try {
                Field field = Attribute.class.getDeclaredField(key);
                Object value = field.get(null);

                if (value instanceof Attribute attribute) {
                    return attribute;
                }
            } catch (Exception e) {
                // Try the next (older) name
            }
        }

        return null;
    }
}
