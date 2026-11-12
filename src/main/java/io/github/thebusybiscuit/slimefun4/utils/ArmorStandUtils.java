package io.github.thebusybiscuit.slimefun4.utils;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.core.services.holograms.HologramsService;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.HologramProjector;

/**
 * This class holds utilities for {@link ArmorStand}, useful for classes
 * dealing with {@link ArmorStand}s that are not from {@link HologramsService}
 *
 * @see HologramProjector
 * @see AncientPedestal
 *
 * @author JustAHuman
 */
public class ArmorStandUtils {

    private static volatile Boolean legacySpawn;

    private ArmorStandUtils() {}

    /**
     * Spawns an {@link ArmorStand} at the given {@link Location} with the given custom name
     * <br>
     * Set Properties: Invisible, Silent, Marker, No-Gravity, No Base Plate, Don't Remove When Far Away
     *
     * @param location The {@link Location} to spawn the {@link ArmorStand}
     * @param customName The {@link String} custom name the {@link ArmorStand} should display
     *
     * @return The spawned {@link ArmorStand}
     */
    public static @Nonnull ArmorStand spawnArmorStand(@Nonnull Location location, @Nonnull String customName) {
        ArmorStand armorStand = spawnArmorStand(location);
        armorStand.setCustomName(customName);
        armorStand.setCustomNameVisible(true);
        return armorStand;
    }
    
    /**
     * Spawns an {@link ArmorStand} at the given {@link Location}
     * <br>
     * Set Properties: Invisible, Silent, Marker, No-Gravity, No Base Plate, Don't Remove When Far Away
     *
     * @param location The {@link Location} to spawn the {@link ArmorStand}
     *
     * @return The spawned {@link ArmorStand}
     */
    public static @Nonnull ArmorStand spawnArmorStand(@Nonnull Location location) {
        // The consumer method was moved from World to RegionAccessor in 1.20.2
        // Due to this, we need to use a rubbish workaround to support 1.20.1 and below
        // This causes flicker on these versions which sucks but not sure a better way around this right now.
        if (isBefore1202()) {
            ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
            setupArmorStand(armorStand);
            return armorStand;
        }

        return location.getWorld().spawn(location, ArmorStand.class, ArmorStandUtils::setupArmorStand);
    }

    /**
     * Whether the server runs a Minecraft version below 1.20.2.
     * We parse this ourselves since PaperLib cannot read year-based versions (26.x)
     * and would report them as version 0, i.e. "older than 1.20.2".
     */
    private static boolean isBefore1202() {
        Boolean before = legacySpawn;

        if (before == null) {
            int[] version = MinecraftVersion.parseVersion(Bukkit.getVersion());
            before = version != null && (version[0] < 20 || (version[0] == 20 && version[1] < 2));
            legacySpawn = before;
        }

        return before;
    }

    private static void setupArmorStand(ArmorStand armorStand) {
        armorStand.setVisible(false);
        armorStand.setSilent(true);
        armorStand.setMarker(true);
        armorStand.setGravity(false);
        armorStand.setBasePlate(false);
        armorStand.setRemoveWhenFarAway(false);
    }
}
