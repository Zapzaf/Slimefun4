package io.github.thebusybiscuit.slimefun4.api.items;

import java.util.Locale;

import javax.annotation.Nonnull;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Slimefun items that are turned off, from {@code options.disabled-items} in the config.
 * <p>
 * A listed item is disabled exactly like {@code enabled: false} in Items.yml: it can't be crafted, is
 * hidden from the guide, and copies players already have do nothing when used. The list is read when the
 * items are registered, so changes need a restart.
 */
public final class DisabledItems {

    /** The config list of Slimefun item ids that are disabled. */
    public static final String CONFIG_KEY = "options.disabled-items";

    private DisabledItems() {}

    /**
     * Whether this Slimefun item id is on the list of disabled items (any case).
     *
     * @param id
     *            The id of a {@link SlimefunItem}
     * @return Whether the item is disabled in the config
     */
    public static boolean isListed(@Nonnull String id) {
        for (String entry : Slimefun.getCfg().getStringList(CONFIG_KEY)) {
            if (entry != null && entry.trim().toUpperCase(Locale.ROOT).equals(id)) {
                return true;
            }
        }
        return false;
    }
}
