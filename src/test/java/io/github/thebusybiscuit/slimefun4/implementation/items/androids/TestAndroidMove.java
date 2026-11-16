package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.BlockListener;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Tests that a moving {@link ProgrammableAndroid} cannot be duplicated by placing a block
 * into the spot it just left, before its data has been moved by the ticker.
 */
class TestAndroidMove {

    private static final int FUEL_SLOT = 43;

    private static ServerMock server;
    private static ProgrammableAndroid android;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        Slimefun plugin = MockBukkit.load(Slimefun.class);
        new BlockListener(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "androids_move");
        SlimefunItemStack item = new SlimefunItemStack("TEST_MOVING_ANDROID", HeadTexture.PROGRAMMABLE_ANDROID, "&cTest Moving Android");
        android = new ProgrammableAndroid(itemGroup, 1, item, RecipeType.NULL, new ItemStack[9]);
        android.register(plugin);

        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private @Nonnull Block placeAndroid() {
        // Androids do not move outside the world border, so keep the coordinates small
        Block block = world.getBlockAt(TestUtilities.randomInt(10000), 64, TestUtilities.randomInt(10000));
        block.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(block, "id", android.getId());
        BlockStorage.addBlockInfo(block, "fuel", "50");
        BlockStorage.addBlockInfo(block, "rotation", BlockFace.NORTH.name());
        BlockStorage.addBlockInfo(block, "paused", "false");
        BlockStorage.getInventory(block).replaceExistingItem(FUEL_SLOT, new ItemStack(Material.COAL, 12));
        return block;
    }

    private void placeBlockAsPlayer(@Nonnull Player player, @Nonnull Block block) {
        BlockState replaced = block.getState();
        block.setType(Material.STONE);

        BlockPlaceEvent event = new BlockPlaceEvent(block, replaced, block.getRelative(BlockFace.DOWN), new ItemStack(Material.STONE), player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
    }

    private long countDroppedAndroids(@Nonnull Location l) {
        return world.getEntitiesByClass(Item.class).stream()
            .filter(entity -> entity.getLocation().distanceSquared(l) < 4)
            .filter(entity -> android.isItem(entity.getItemStack()))
            .count();
    }

    @Test
    @DisplayName("Test that a moved android keeps its data and fuel after the ticker runs")
    void testMoveKeepsData() {
        Block from = placeAndroid();
        Block to = from.getRelative(BlockFace.NORTH);

        android.move(from, BlockFace.NORTH, to);
        Slimefun.getTickerTask().run();

        Assertions.assertEquals(android, BlockStorage.check(to));
        Assertions.assertFalse(BlockStorage.hasBlockInfo(from));
        Assertions.assertEquals("50", BlockStorage.getLocationInfo(to.getLocation(), "fuel"));
        Assertions.assertEquals(12, BlockStorage.getInventory(to).getItemInSlot(FUEL_SLOT).getAmount());
    }

    @Test
    @DisplayName("Test that placing a block where an android just was does not duplicate it or wipe its fuel")
    void testPlaceBlockInOldSpotDoesNotDuplicate() {
        Player player = server.addPlayer();
        Block from = placeAndroid();
        Block to = from.getRelative(BlockFace.NORTH);

        android.move(from, BlockFace.NORTH, to);

        // The player places a block into the spot the android just left, before the ticker moved its data
        placeBlockAsPlayer(player, from);

        Assertions.assertEquals(0, countDroppedAndroids(from.getLocation()), "An android item was dropped: duplication exploit");

        Slimefun.getTickerTask().run();

        Assertions.assertEquals(android, BlockStorage.check(to), "The moved android lost its data");
        Assertions.assertFalse(BlockStorage.hasBlockInfo(from));
        Assertions.assertEquals("50", BlockStorage.getLocationInfo(to.getLocation(), "fuel"));

        BlockMenu menu = BlockStorage.getInventory(to);
        Assertions.assertNotNull(menu, "The moved android lost its inventory");
        Assertions.assertEquals(12, menu.getItemInSlot(FUEL_SLOT).getAmount(), "The fuel in the android disappeared");
    }
}
