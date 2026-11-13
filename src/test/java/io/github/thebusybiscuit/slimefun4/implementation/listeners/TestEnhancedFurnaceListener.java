package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.block.state.AbstractFurnaceMock;
import be.seeseemelk.mockbukkit.block.state.BlockStateMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Tests the speed, fuel efficiency and fortune of an {@link EnhancedFurnace},
 * using the same values as the Carbonado Edged Furnace (speed 20, efficiency 10, fortune 3).
 */
class TestEnhancedFurnaceListener {

    private static final int SPEED = 20;
    private static final int EFFICIENCY = 10;
    private static final int FORTUNE = 3;

    private static ServerMock server;
    private static EnhancedFurnace carbonadoFurnace;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        Slimefun plugin = MockBukkit.load(Slimefun.class);
        new EnhancedFurnaceListener(plugin);

        // Enhanced Furnaces use a BlockTicker, so they are disabled unless tickers are enabled
        Slimefun.getCfg().setValue("URID.enable-tickers", true);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "enhanced_furnaces");
        SlimefunItemStack item = new SlimefunItemStack("TEST_CARBONADO_EDGED_FURNACE", Material.FURNACE, "&7Test Carbonado Edged Furnace");
        carbonadoFurnace = new EnhancedFurnace(itemGroup, SPEED, EFFICIENCY, FORTUNE, item, new ItemStack[9]);
        carbonadoFurnace.register(plugin);

        server.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "test_raw_iron"), new ItemStack(Material.IRON_INGOT), Material.RAW_IRON, 0.7F, 200));
        server.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "test_sand"), new ItemStack(Material.GLASS), Material.SAND, 0.1F, 200));
        Slimefun.getMinecraftRecipeService().refresh();

        world = TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    /**
     * MockBukkit has no {@link BlockStateMock} for a plain {@link Material#FURNACE},
     * so we provide a minimal one.
     */
    private static class FurnaceMock extends AbstractFurnaceMock {

        FurnaceMock(@Nonnull Block block) {
            super(block);
        }

        FurnaceMock(@Nonnull FurnaceMock state) {
            super(state);
        }

        @Override
        public @Nonnull BlockStateMock getSnapshot() {
            return new FurnaceMock(this);
        }
    }

    private @Nonnull BlockMock placeFurnace(@Nonnull ItemStack smelting) {
        BlockMock block = (BlockMock) world.getBlockAt(TestUtilities.randomInt(), 64, TestUtilities.randomInt());
        block.setType(Material.FURNACE);

        FurnaceMock state = new FurnaceMock(block);
        state.getInventory().setSmelting(smelting);
        state.setCookTimeTotal(200);
        block.setState(state);

        BlockStorage.addBlockInfo(block, "id", carbonadoFurnace.getId());
        return block;
    }

    @Test
    @DisplayName("Test that a placed furnace is recognized and enabled")
    void testFurnaceIsRecognized() {
        BlockMock block = placeFurnace(new ItemStack(Material.RAW_IRON));

        Assertions.assertEquals(carbonadoFurnace, BlockStorage.check(block));
        Assertions.assertFalse(carbonadoFurnace.isDisabled(), "Item state: " + carbonadoFurnace.getState());
        Assertions.assertFalse(carbonadoFurnace.isDisabledIn(world));
    }

    @Test
    @DisplayName("Test that the Carbonado Edged Furnace stores its configured values")
    void testConfiguredValues() {
        Assertions.assertEquals(SPEED - 1, carbonadoFurnace.getProcessingSpeed());
        Assertions.assertEquals(EFFICIENCY - 1, carbonadoFurnace.getFuelEfficiency());
    }

    @Test
    @DisplayName("Test that the speed modifier advances the cook time on every tick")
    void testSpeedModifier() {
        BlockMock block = placeFurnace(new ItemStack(Material.RAW_IRON));
        Furnace before = (Furnace) block.getState();
        before.setCookTime((short) 1);
        before.update(true, false);

        carbonadoFurnace.getItemHandler().tick(block, carbonadoFurnace, null);

        // Speed 20 adds 19 * 10 = 190 ticks of progress on top of the vanilla tick
        Furnace after = (Furnace) block.getState();
        Assertions.assertEquals(191, after.getCookTime());
    }

    @Test
    @DisplayName("Test that the speed modifier never completes a smelt by itself")
    void testSpeedModifierIsCapped() {
        BlockMock block = placeFurnace(new ItemStack(Material.RAW_IRON));
        Furnace before = (Furnace) block.getState();
        before.setCookTime((short) 150);
        before.update(true, false);

        carbonadoFurnace.getItemHandler().tick(block, carbonadoFurnace, null);

        Furnace after = (Furnace) block.getState();
        Assertions.assertEquals(199, after.getCookTime());
    }

    @Test
    @DisplayName("Test that an idle furnace is not sped up")
    void testIdleFurnaceIsNotSpedUp() {
        BlockMock block = placeFurnace(new ItemStack(Material.RAW_IRON));

        carbonadoFurnace.getItemHandler().tick(block, carbonadoFurnace, null);

        Furnace after = (Furnace) block.getState();
        Assertions.assertEquals(0, after.getCookTime());
    }

    @Test
    @DisplayName("Test that fuel burns longer in the Carbonado Edged Furnace")
    void testFuelEfficiency() {
        BlockMock block = placeFurnace(new ItemStack(Material.RAW_IRON));
        FurnaceBurnEvent event = new FurnaceBurnEvent(block, new ItemStack(Material.COAL), 1600);

        server.getPluginManager().callEvent(event);

        Assertions.assertEquals(1600 * (EFFICIENCY - 1), event.getBurnTime());
    }

    @Test
    @DisplayName("Test that fortune multiplies the output of ores and raw metals")
    void testFortuneOnRawMetals() {
        BlockMock block = placeFurnace(new ItemStack(Material.RAW_IRON));
        Set<Integer> amounts = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            FurnaceSmeltEvent event = new FurnaceSmeltEvent(block, new ItemStack(Material.RAW_IRON), new ItemStack(Material.IRON_INGOT));
            server.getPluginManager().callEvent(event);

            ItemStack result = event.getResult();
            Assertions.assertEquals(Material.IRON_INGOT, result.getType());
            Assertions.assertTrue(result.getAmount() >= 1 && result.getAmount() <= FORTUNE + 1, "Unexpected amount: " + result.getAmount());
            amounts.add(result.getAmount());
        }

        // Fortune 3 gives 1 to 4 items, so 200 smelts should produce more than a single item at least once
        Assertions.assertTrue(amounts.stream().anyMatch(amount -> amount > 1), "Fortune never gave a bonus item: " + amounts);
    }

    @Test
    @DisplayName("Test that fortune does not apply to items that are not ores or raw metals")
    void testNoFortuneOnOtherItems() {
        BlockMock block = placeFurnace(new ItemStack(Material.SAND));

        for (int i = 0; i < 50; i++) {
            FurnaceSmeltEvent event = new FurnaceSmeltEvent(block, new ItemStack(Material.SAND), new ItemStack(Material.GLASS));
            server.getPluginManager().callEvent(event);

            Assertions.assertEquals(1, event.getResult().getAmount());
        }
    }

    @Test
    @DisplayName("Test that fortune never overfills the result slot")
    void testFortuneRespectsStackSize() {
        BlockMock block = placeFurnace(new ItemStack(Material.RAW_IRON));
        Furnace state = (Furnace) block.getState();
        state.getInventory().setResult(new ItemStack(Material.IRON_INGOT, 63));
        state.update(true, false);

        for (int i = 0; i < 50; i++) {
            FurnaceSmeltEvent event = new FurnaceSmeltEvent(block, new ItemStack(Material.RAW_IRON), new ItemStack(Material.IRON_INGOT));
            server.getPluginManager().callEvent(event);

            Assertions.assertEquals(1, event.getResult().getAmount());
        }
    }
}
