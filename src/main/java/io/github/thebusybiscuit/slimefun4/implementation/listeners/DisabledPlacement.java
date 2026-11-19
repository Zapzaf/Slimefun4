package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.entity.Player;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Slimefun blocks that may not be placed, from {@code options.disabled-block-placement} in the config.
 * <p>
 * Server owners list blocks that cost too much performance (cargo nodes, some machines) there. The item
 * still exists and works otherwise (crafting, recipes, as an ingredient); it just can't be placed down,
 * neither by a player nor by a Block Placer. Blocks that were placed before stay where they are.
 * Players with {@link #BYPASS_PERMISSION} can still place them.
 */
public final class DisabledPlacement {

    /** The config list of Slimefun item ids that can't be placed. */
    public static final String CONFIG_KEY = "options.disabled-block-placement";

    /** Lets a player place blocks from that list anyway. Nobody has it by default, also not operators. */
    public static final String BYPASS_PERMISSION = "slimefun.place.bypass";

    private DisabledPlacement() {}

    /**
     * Whether this Slimefun item is on the list of blocks that can't be placed.
     *
     * @param item
     *            The {@link SlimefunItem} about to be placed
     * @return Whether placing it is disabled
     */
    public static boolean isDisabled(@Nonnull SlimefunItem item) {
        String id = item.getId();
        for (String entry : Slimefun.getCfg().getStringList(CONFIG_KEY)) {
            if (entry != null && entry.trim().toUpperCase(Locale.ROOT).equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this player may not place this Slimefun item.
     *
     * @param item
     *            The {@link SlimefunItem} about to be placed
     * @param player
     *            The {@link Player} placing it, or {@code null} for a machine such as the Block Placer
     * @return Whether the placement must be stopped
     */
    public static boolean isDisabledFor(@Nonnull SlimefunItem item, @Nullable Player player) {
        return isDisabled(item) && (player == null || !player.hasPermission(BYPASS_PERMISSION));
    }
}
