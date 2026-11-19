package io.github.thebusybiscuit.slimefun4.api.items;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/** {@code options.disabled-items}: listed Slimefun items are disabled. */
class TestDisabledItems {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    static void unload() {
        MockBukkit.unmock();
    }

    @AfterEach
    void afterEach() {
        Slimefun.getCfg().setValue(DisabledItems.CONFIG_KEY, List.of());
    }

    @Test
    @DisplayName("A listed item is disabled when registered (the id is matched in any case) and can't be used")
    void listedItemIsDisabled() {
        Slimefun.getCfg().setValue(DisabledItems.CONFIG_KEY, List.of("off_item"));
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "OFF_ITEM", new ItemStack(Material.BOOK));
        item.register(plugin);

        Assertions.assertEquals(ItemState.DISABLED, item.getState());
        Assertions.assertTrue(item.isDisabled());

        PlayerMock player = server.addPlayer();
        Assertions.assertFalse(item.canUse(player, true));
        Assertions.assertNotNull(player.nextMessage(), "the player is told the item is disabled");
    }

    @Test
    @DisplayName("Items that aren't listed are enabled as usual")
    void otherItemsStayEnabled() {
        Slimefun.getCfg().setValue(DisabledItems.CONFIG_KEY, List.of("OFF_ITEM"));
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "ON_ITEM", new ItemStack(Material.BOOK));
        item.register(plugin);

        Assertions.assertEquals(ItemState.ENABLED, item.getState());
    }

    @Test
    @DisplayName("By default only the Tome of Knowledge Sharing is disabled")
    void defaultList() throws IOException {
        YamlConfiguration defaults = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(Slimefun.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            defaults.load(reader);
        } catch (InvalidConfigurationException e) {
            throw new IOException(e);
        }

        Assertions.assertEquals(List.of("KNOWLEDGE_TOME"), defaults.getStringList(DisabledItems.CONFIG_KEY));
    }
}
