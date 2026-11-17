package me.mrCookieSlime.Slimefun.api;

import java.util.concurrent.CountDownLatch;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.Capacitor;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

/**
 * Slimefun blocks must not turn into vanilla blocks when they are broken and placed again quickly.
 * <p>
 * The energy network ticks asynchronously and still writes charge to blocks a player just broke,
 * while the player places a new block there on the main thread.
 */
class TestBlockStorageRace {

    private static final int ROUNDS = 20_000;

    private static ServerMock server;
    private static Capacitor capacitor;
    private static World world;
    private static int nextX;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        Slimefun plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "block_storage_race");
        SlimefunItemStack item = new SlimefunItemStack("TEST_RACE_CAPACITOR", HeadTexture.CAPACITOR_25, "&aTest Capacitor");
        capacitor = new Capacitor(itemGroup, 1000, item, RecipeType.NULL, new ItemStack[9]);
        capacitor.register(plugin);

        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private static Location freshLocation() {
        return new Location(world, nextX++, 64, 0);
    }

    @Test
    @DisplayName("Placing a block while an async ticker writes to the same spot keeps the block's id")
    void concurrentWritesKeepTheId() throws InterruptedException {
        int lost = 0;

        for (int round = 0; round < ROUNDS; round++) {
            Location l = freshLocation();
            CountDownLatch start = new CountDownLatch(1);

            Thread player = new Thread(() -> {
                await(start);
                BlockStorage.addBlockInfo(l, "id", capacitor.getId());
            });
            Thread ticker = new Thread(() -> {
                await(start);
                BlockStorage.addBlockInfo(l, "energy-charge", "5");
            });

            player.start();
            ticker.start();
            start.countDown();
            player.join();
            ticker.join();

            if (!capacitor.getId().equals(BlockStorage.getLocationInfo(l).getString("id"))) {
                lost++;
            }
        }

        Assertions.assertEquals(0, lost, "blocks that lost their id (turned vanilla) in " + ROUNDS + " rounds");
    }

    @Test
    @DisplayName("Charging a capacitor that was just broken does not leave data behind")
    void chargeAfterBreakLeavesNoData() {
        Block block = freshLocation().getBlock();
        block.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(block, "id", capacitor.getId());

        // The player breaks it, and the ticker deletes its data
        block.setType(Material.AIR);
        BlockStorage.deleteLocationInfoUnsafely(block.getLocation(), true);

        // The energy network still has it cached and charges it
        capacitor.setCharge(block.getLocation(), 500);
        capacitor.addCharge(block.getLocation(), 100);

        Assertions.assertNull(BlockStorage.getLocationInfo(block.getLocation()).getString("energy-charge"));
        Assertions.assertFalse(BlockStorage.hasBlockInfo(block));
    }

    @Test
    @DisplayName("A capacitor placed where one was just broken starts empty")
    void replacedCapacitorStartsEmpty() {
        Block block = freshLocation().getBlock();
        block.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(block, "id", capacitor.getId());
        capacitor.setCharge(block.getLocation(), 800);

        block.setType(Material.AIR);
        BlockStorage.deleteLocationInfoUnsafely(block.getLocation(), true);
        capacitor.setCharge(block.getLocation(), 800);

        block.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(block, "id", capacitor.getId());

        Assertions.assertEquals(0, capacitor.getCharge(block.getLocation()));
    }

    @Test
    @DisplayName("Charging a placed capacitor still works")
    void chargingStillWorks() {
        Block block = freshLocation().getBlock();
        block.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(block, "id", capacitor.getId());

        capacitor.setCharge(block.getLocation(), 300);
        capacitor.addCharge(block.getLocation(), 200);
        capacitor.removeCharge(block.getLocation(), 100);

        Assertions.assertEquals(400, capacitor.getCharge(block.getLocation()));
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
