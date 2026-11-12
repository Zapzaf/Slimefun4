package me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import be.seeseemelk.mockbukkit.MockBukkit;

class TestAContainer {

    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test that the consumed amount of a partially used stack is detected (#4268)")
    void testPartiallyConsumedStack() {
        ItemStack[] before = { new ItemStack(Material.GRAVEL, 10), null };
        ItemStack[] after = { new ItemStack(Material.GRAVEL, 9), null };

        List<ItemStack> consumed = AContainer.getConsumedItems(before, after);

        Assertions.assertEquals(1, consumed.size());
        Assertions.assertEquals(Material.GRAVEL, consumed.get(0).getType());
        Assertions.assertEquals(1, consumed.get(0).getAmount());
    }

    @Test
    @DisplayName("Test that fully consumed items keep their exact data (#4268)")
    void testFullyConsumedItems() {
        ItemStack damagedPickaxe = CustomItemStack.create(Material.DIAMOND_PICKAXE, "&bMy Pickaxe");
        ItemStack ductTape = new ItemStack(Material.PAPER, 3);

        ItemStack[] before = { damagedPickaxe, ductTape };
        ItemStack[] after = { null, new ItemStack(Material.PAPER, 2) };

        List<ItemStack> consumed = AContainer.getConsumedItems(before, after);

        Assertions.assertEquals(2, consumed.size());
        Assertions.assertTrue(consumed.get(0).isSimilar(damagedPickaxe));
        Assertions.assertEquals(1, consumed.get(0).getAmount());
        Assertions.assertEquals(Material.PAPER, consumed.get(1).getType());
        Assertions.assertEquals(1, consumed.get(1).getAmount());
    }

    @Test
    @DisplayName("Test that untouched slots are not counted as consumed")
    void testNothingConsumed() {
        ItemStack[] before = { new ItemStack(Material.COBBLESTONE, 5), new ItemStack(Material.AIR) };
        ItemStack[] after = { new ItemStack(Material.COBBLESTONE, 5), null };

        Assertions.assertTrue(AContainer.getConsumedItems(before, after).isEmpty());
    }

    @Test
    @DisplayName("Test that a slot replaced by a different item counts as fully consumed")
    void testReplacedItem() {
        ItemStack[] before = { new ItemStack(Material.BUCKET, 2) };
        ItemStack[] after = { new ItemStack(Material.STONE, 2) };

        List<ItemStack> consumed = AContainer.getConsumedItems(before, after);

        Assertions.assertEquals(1, consumed.size());
        Assertions.assertEquals(Material.BUCKET, consumed.get(0).getType());
        Assertions.assertEquals(2, consumed.get(0).getAmount());
    }

    @Test
    @DisplayName("Test that recipes producing disabled Slimefun items are detected (#4333)")
    void testDisabledOutput() {
        SlimefunItem disabled = TestUtilities.mockSlimefunItem(plugin, "ACONTAINER_DISABLED_OUTPUT", CustomItemStack.create(Material.COOKED_BEEF, "&cDisabled Jerky"));
        Slimefun.getItemCfg().setValue("ACONTAINER_DISABLED_OUTPUT.enabled", false);
        disabled.register(plugin);

        SlimefunItem enabled = TestUtilities.mockSlimefunItem(plugin, "ACONTAINER_ENABLED_OUTPUT", CustomItemStack.create(Material.COOKED_PORKCHOP, "&aEnabled Jerky"));
        enabled.register(plugin);

        ItemStack[] input = { new ItemStack(Material.BEEF) };

        Assertions.assertTrue(disabled.isDisabled());
        Assertions.assertTrue(AContainer.hasDisabledOutput(new MachineRecipe(5, input, new ItemStack[] { disabled.getItem().clone() })));
        Assertions.assertFalse(AContainer.hasDisabledOutput(new MachineRecipe(5, input, new ItemStack[] { enabled.getItem().clone() })));
        Assertions.assertFalse(AContainer.hasDisabledOutput(new MachineRecipe(5, input, new ItemStack[] { new ItemStack(Material.COOKED_BEEF) })));
    }
}
