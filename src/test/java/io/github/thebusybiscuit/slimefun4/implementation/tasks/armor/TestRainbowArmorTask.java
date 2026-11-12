package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.RainbowArmorPiece;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.ArmorTask;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

class TestRainbowArmorTask {

    private static final Color CUSTOM_COLOR = Color.fromRGB(12, 34, 56);

    private static ServerMock server;
    private static Slimefun plugin;
    private static SlimefunItemStack rainbowChestplate;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        rainbowChestplate = new SlimefunItemStack("MOCK_RAINBOW_CHESTPLATE", Material.LEATHER_CHESTPLATE, "&cRainbow Test Chestplate");
        DyeColor[] colors = { DyeColor.RED, DyeColor.BLUE };
        new RainbowArmorPiece(TestUtilities.getItemGroup(plugin, "rainbow_test"), rainbowChestplate, RecipeType.NULL, new ItemStack[9], colors).register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test that Rainbow Armor changes its color")
    void testRainbowArmorIsRecolored() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);

        player.getInventory().setChestplate(rainbowChestplate.item().clone());
        new ArmorTask(false).run();
        new RainbowArmorTask().run();

        Color color = getColor(player.getInventory().getChestplate());
        Assertions.assertEquals(DyeColor.RED.getColor(), color);
    }

    @Test
    @DisplayName("Test that normal leather armor is not recolored after swapping it with Rainbow Armor (#3931)")
    void testNormalArmorIsNotRecolored() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);

        // The armor cache of the profile now contains the rainbow chestplate
        player.getInventory().setChestplate(rainbowChestplate.item().clone());
        new ArmorTask(false).run();

        // Quickly swap it with a normal leather chestplate before the armor cache is updated
        ItemStack normal = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) normal.getItemMeta();
        meta.setColor(CUSTOM_COLOR);
        normal.setItemMeta(meta);
        player.getInventory().setChestplate(normal);

        new RainbowArmorTask().run();

        Assertions.assertEquals(CUSTOM_COLOR, getColor(player.getInventory().getChestplate()));
    }

    private static Color getColor(ItemStack item) {
        return ((LeatherArmorMeta) item.getItemMeta()).getColor();
    }
}
