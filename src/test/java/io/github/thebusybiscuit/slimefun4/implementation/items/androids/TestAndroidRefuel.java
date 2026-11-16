package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.block.state.DispenserMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * Tests that a {@link ProgrammableAndroid} can pull fuel from an
 * {@link AndroidInterface} (Fuel) using the {@link Instruction#INTERFACE_FUEL} instruction.
 */
class TestAndroidRefuel {

    private static final int FUEL_SLOT = 43;

    private static ServerMock server;
    private static ProgrammableAndroid android;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        Slimefun plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "androids");
        SlimefunItemStack item = new SlimefunItemStack("TEST_PROGRAMMABLE_ANDROID", Material.PLAYER_HEAD, "&cTest Android");
        android = new ProgrammableAndroid(itemGroup, 1, item, RecipeType.NULL, new ItemStack[9]);
        android.register(plugin);

        new AndroidInterface(itemGroup, SlimefunItems.ANDROID_INTERFACE_FUEL, RecipeType.NULL, new ItemStack[9]).register(plugin);

        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private @Nonnull Block placeAndroid(int x, int z) {
        Block block = world.getBlockAt(x, 64, z);
        block.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(block, "id", android.getId());
        BlockStorage.addBlockInfo(block, "fuel", "0");
        BlockStorage.addBlockInfo(block, "rotation", BlockFace.NORTH.name());
        BlockStorage.addBlockInfo(block, "paused", "true");
        return block;
    }

    private @Nonnull BlockMock placeFuelInterface(@Nonnull Block android, ItemStack... items) {
        BlockMock block = (BlockMock) android.getRelative(BlockFace.NORTH);
        block.setType(Material.DISPENSER);

        DispenserMock state = (DispenserMock) block.getState();
        state.getInventory().setContents(items);
        state.update(true, false);

        BlockStorage.addBlockInfo(block, "id", SlimefunItems.ANDROID_INTERFACE_FUEL.getItemId());
        return block;
    }

    private @Nonnull ItemStack[] interfaceContents(@Nonnull Block block) {
        return ((Dispenser) block.getState()).getInventory().getContents();
    }

    @Test
    @DisplayName("Test that the android has a menu with a fuel slot")
    void testAndroidHasMenu() {
        Block block = placeAndroid(TestUtilities.randomInt(), TestUtilities.randomInt());
        Assertions.assertNotNull(BlockStorage.getInventory(block));
    }

    @Test
    @DisplayName("Test that fuel moves from the Fuel Interface into an empty fuel slot")
    void testRefuelEmptySlot() {
        Block block = placeAndroid(TestUtilities.randomInt(), TestUtilities.randomInt());
        BlockMock fuelInterface = placeFuelInterface(block, new ItemStack(Material.COAL, 16));
        BlockMenu menu = BlockStorage.getInventory(block);

        android.refuel(menu, fuelInterface);

        ItemStack fuel = menu.getItemInSlot(FUEL_SLOT);
        Assertions.assertNotNull(fuel, "Fuel slot is still empty");
        Assertions.assertEquals(Material.COAL, fuel.getType());
        Assertions.assertEquals(16, fuel.getAmount());

        for (ItemStack item : interfaceContents(fuelInterface)) {
            Assertions.assertTrue(item == null || item.getType().isAir(), "Fuel was not removed from the interface: " + item);
        }
    }

    @Test
    @DisplayName("Test that fuel stacks onto existing fuel in the fuel slot")
    void testRefuelStacks() {
        Block block = placeAndroid(TestUtilities.randomInt(), TestUtilities.randomInt());
        BlockMock fuelInterface = placeFuelInterface(block, new ItemStack(Material.COAL, 10), new ItemStack(Material.COAL, 10));
        BlockMenu menu = BlockStorage.getInventory(block);
        menu.replaceExistingItem(FUEL_SLOT, new ItemStack(Material.COAL, 5));

        android.refuel(menu, fuelInterface);

        Assertions.assertEquals(25, menu.getItemInSlot(FUEL_SLOT).getAmount());

        int left = 0;
        for (ItemStack item : interfaceContents(fuelInterface)) {
            if (item != null && !item.getType().isAir()) {
                left += item.getAmount();
            }
        }

        Assertions.assertEquals(0, left, "Fuel was duplicated or not removed from the interface");
    }

    @Test
    @DisplayName("Test that a cargo output node can insert fuel into the android")
    void testCargoCanInsertFuel() {
        Block block = placeAndroid(TestUtilities.randomInt(), TestUtilities.randomInt());
        BlockMenu menu = BlockStorage.getInventory(block);

        // This is exactly what CargoUtils#insert asks before putting an item into a Slimefun block
        int[] slots = menu.getPreset().getSlotsAccessedByItemTransport(menu, ItemTransportFlow.INSERT, new ItemStack(Material.COAL));
        Assertions.assertArrayEquals(new int[] { FUEL_SLOT }, slots);
    }

    @Test
    @DisplayName("Test that cargo cannot insert non-fuel items or withdraw from the android")
    void testCargoRejectsNonFuel() {
        Block block = placeAndroid(TestUtilities.randomInt(), TestUtilities.randomInt());
        BlockMenu menu = BlockStorage.getInventory(block);

        Assertions.assertEquals(0, menu.getPreset().getSlotsAccessedByItemTransport(menu, ItemTransportFlow.INSERT, new ItemStack(Material.DIRT)).length);
        Assertions.assertEquals(0, menu.getPreset().getSlotsAccessedByItemTransport(menu, ItemTransportFlow.WITHDRAW, null).length);
    }

    @Test
    @DisplayName("Test that a running android pulls fuel through its script")
    void testRefuelThroughScript() {
        Block block = placeAndroid(TestUtilities.randomInt(), TestUtilities.randomInt());
        BlockMock fuelInterface = placeFuelInterface(block, new ItemStack(Material.COAL, 8));
        BlockMenu menu = BlockStorage.getInventory(block);

        // One coal so the android can start running at all
        menu.replaceExistingItem(FUEL_SLOT, new ItemStack(Material.COAL, 1));
        BlockStorage.addBlockInfo(block, "script", "START-INTERFACE_FUEL-REPEAT");
        BlockStorage.addBlockInfo(block, "index", "0");
        BlockStorage.addBlockInfo(block, "paused", "false");

        for (int i = 0; i < 4; i++) {
            android.tick(block, BlockStorage.getLocationInfo(block.getLocation()));
        }

        Assertions.assertEquals(8, menu.getItemInSlot(FUEL_SLOT).getAmount());
    }
}
