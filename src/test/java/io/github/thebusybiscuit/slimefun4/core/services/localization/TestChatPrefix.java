package io.github.thebusybiscuit.slimefun4.core.services.localization;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/** The chat prefix reads "SF » message", with one space on each side of the ». */
class TestChatPrefix {

    @Test
    @DisplayName("The default config uses the new prefix")
    void defaultConfig() throws Exception {
        YamlConfiguration defaults = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(Slimefun.class.getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            defaults.load(reader);
        }
        Assertions.assertEquals(Slimefun.CHAT_PREFIX, defaults.getString("options.chat-prefix"));
    }

    @Test
    @DisplayName("Messages read \"SF » message\"")
    void rendering() {
        String shown = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', Slimefun.CHAT_PREFIX) + "You researched Cargo");
        Assertions.assertEquals("SF » You researched Cargo", shown);
    }
}
