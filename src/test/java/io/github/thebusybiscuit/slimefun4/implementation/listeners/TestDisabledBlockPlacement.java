package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/** {@code options.disabled-block-placement}: listed Slimefun blocks can't be placed. */
class TestDisabledBlockPlacement {

    private static ServerMock server;
    private static Slimefun plugin;
    private static SlimefunItem laggy;
    private static SlimefunItem fine;
    private static World world;

    @BeforeAll
    static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        new BlockListener(plugin);

        laggy = TestUtilities.mockSlimefunItem(plugin, "LAGGY_NODE", new ItemStack(Material.BLUE_TERRACOTTA));
        laggy.register(plugin);
        fine = TestUtilities.mockSlimefunItem(plugin, "FINE_MACHINE", new ItemStack(Material.RED_TERRACOTTA));
        fine.register(plugin);

        world = server.addSimpleWorld("place_world");
        Slimefun.getRegistry().getWorlds().put("place_world", new BlockStorage(world));
    }

    @AfterAll
    static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void beforeEach() {
        server.getPluginManager().clearEvents();
        Slimefun.getCfg().setValue(DisabledPlacement.CONFIG_KEY, List.of("laggy_node"));
    }

    @AfterEach
    void afterEach() {
        Slimefun.getCfg().setValue(DisabledPlacement.CONFIG_KEY, List.of());
    }

    private BlockPlaceEvent place(SlimefunItem item, PlayerMock player) {
        ItemStack stack = item.getItem().clone();
        player.getInventory().setItemInMainHand(stack);

        int x = TestUtilities.randomInt();
        int z = TestUtilities.randomInt();
        Block block = new BlockMock(stack.getType(), new Location(world, x, 0, z));
        Block against = new BlockMock(Material.GRASS_BLOCK, new Location(world, x, 1, z));

        BlockPlaceEvent event = new BlockPlaceEvent(block, block.getState(), against, stack, player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("A listed block can't be placed, and the player is told why (the id is matched in any case)")
    void listedBlockIsRefused() {
        PlayerMock player = server.addPlayer();

        BlockPlaceEvent event = place(laggy, player);

        Assertions.assertTrue(event.isCancelled());
        Assertions.assertFalse(BlockStorage.hasBlockInfo(event.getBlock()), "no Slimefun block was created");
        server.getPluginManager().assertEventNotFired(SlimefunBlockPlaceEvent.class);
        Assertions.assertNotNull(player.nextMessage(), "the player gets a message");
    }

    @Test
    @DisplayName("Blocks that aren't listed are placed as usual")
    void otherBlocksStillWork() {
        BlockPlaceEvent event = place(fine, server.addPlayer());

        Assertions.assertFalse(event.isCancelled());
        Assertions.assertEquals("FINE_MACHINE", BlockStorage.checkID(event.getBlock()));
    }

    @Test
    @DisplayName("Operators can't place listed blocks either, unless they have slimefun.place.bypass")
    void bypassPermission() {
        PlayerMock op = server.addPlayer();
        op.setOp(true);
        Assertions.assertTrue(place(laggy, op).isCancelled(), "being op is not enough");

        PlayerMock trusted = server.addPlayer();
        trusted.addAttachment(plugin, DisabledPlacement.BYPASS_PERMISSION, true);
        BlockPlaceEvent event = place(laggy, trusted);
        Assertions.assertFalse(event.isCancelled());
        Assertions.assertEquals("LAGGY_NODE", BlockStorage.checkID(event.getBlock()));
    }

    @Test
    @DisplayName("With an empty list (the default) everything can be placed")
    void emptyListByDefault() {
        Slimefun.getCfg().setValue(DisabledPlacement.CONFIG_KEY, List.of());
        Assertions.assertFalse(place(laggy, server.addPlayer()).isCancelled());
    }

    @Test
    @DisplayName("Block Placers check the same list, with no bypass")
    void blockPlacerCheck() {
        Assertions.assertTrue(DisabledPlacement.isDisabledFor(laggy, null));
        Assertions.assertFalse(DisabledPlacement.isDisabledFor(fine, null));
    }
}
