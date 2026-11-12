package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import be.seeseemelk.mockbukkit.MockBukkit;

class TestGiveCommandStacks {

    @BeforeAll
    public static void load() {
        MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @ParameterizedTest
    @DisplayName("Test that /sf give splits items into stacks of their max stack size (#4272)")
    @CsvSource({ "GOLD_INGOT, 666, 11, 26", "GOLD_INGOT, 64, 1, 64", "GOLD_INGOT, 1, 1, 1", "ENDER_PEARL, 40, 3, 8", "DIAMOND_SWORD, 3, 3, 1" })
    void testSplitIntoStacks(Material type, int amount, int expectedStacks, int lastStackSize) {
        ItemStack item = new ItemStack(type);
        List<ItemStack> stacks = GiveCommand.splitIntoStacks(item, amount);

        Assertions.assertEquals(expectedStacks, stacks.size());
        Assertions.assertEquals(amount, stacks.stream().mapToInt(ItemStack::getAmount).sum());
        Assertions.assertEquals(lastStackSize, stacks.get(stacks.size() - 1).getAmount());

        for (ItemStack stack : stacks) {
            Assertions.assertTrue(stack.getAmount() <= type.getMaxStackSize());
            Assertions.assertTrue(stack.isSimilar(item));
        }
    }
}
