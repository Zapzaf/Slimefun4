package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * This {@link Listener} applies the speed of an {@link EnhancedFurnace}.
 * Whenever such a furnace starts smelting an item, it shortens the total cook time of that item,
 * so the server itself cooks it faster on every tick.
 * <p>
 * This replaces the old approach of pushing the furnace's progress forward from a
 * {@link me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker}, which could fail to apply on newer servers
 * and left the furnaces running at vanilla speed.
 * <p>
 * It is kept apart from {@link EnhancedFurnaceListener} because {@link FurnaceStartSmeltEvent}
 * only exists on Minecraft 1.17+. Registering it on older servers would fail.
 *
 * @see EnhancedFurnace#isStartSmeltEventSupported()
 */
public class EnhancedFurnaceCookTimeListener implements Listener {

    public EnhancedFurnaceCookTimeListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onStartSmelt(FurnaceStartSmeltEvent e) {
        if (e.getBlock().getType() != Material.FURNACE) {
            // We don't care about Smokers, Blast Furnaces and all that fancy stuff
            return;
        }

        SlimefunItem sfItem = BlockStorage.check(e.getBlock());

        if (sfItem instanceof EnhancedFurnace enhancedFurnace && !enhancedFurnace.isDisabledIn(e.getBlock().getWorld())) {
            e.setTotalCookTime(enhancedFurnace.getCookTime(e.getTotalCookTime()));
        }
    }

}
