package io.github.thebusybiscuit.slimefun4.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;

class TestMinecraftVersion {

    @Test
    @DisplayName("Test if Minecraft versions match themselves")
    void testMatches() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_16.isMinecraftVersion(16, -1));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_17.isMinecraftVersion(17, -1));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20_5.isMinecraftVersion(20, 5));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_17.isMinecraftVersion(16, -1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_16.isMinecraftVersion(0, -1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20_5.isMinecraftVersion(20, 4));
    }

    @Test
    @DisplayName("Test if Minecraft versions match minor versions")
    void testMatchesMinor() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_16.isMinecraftVersion(16, 1));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_16.isMinecraftVersion(16, 2));

        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20.isMinecraftVersion(20, 4));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20_5.isMinecraftVersion(20, 6));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20.isMinecraftVersion(20, 5));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_16.isMinecraftVersion(17, 1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20_5.isMinecraftVersion(20, 4));
    }

    @Test
    @DisplayName("Test if Minecraft versions are ordered correctly (#atLeast)")
    void testAtLeast() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_18.isAtLeast(MinecraftVersion.MINECRAFT_1_16));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_17.isAtLeast(MinecraftVersion.MINECRAFT_1_16));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_17.isAtLeast(MinecraftVersion.MINECRAFT_1_17));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20.isAtLeast(MinecraftVersion.MINECRAFT_1_20));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20_5.isAtLeast(MinecraftVersion.MINECRAFT_1_20));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_17.isAtLeast(MinecraftVersion.MINECRAFT_1_18));
    }

    @Test
    @DisplayName("Test correct behaviour for MinecraftVersion.UNKNOWN.isAtleast(...)")
    void testAtLeastUnknown() {
        // Unknown should always fall back to false
        Assertions.assertFalse(MinecraftVersion.UNKNOWN.isAtLeast(MinecraftVersion.MINECRAFT_1_16));
        Assertions.assertFalse(MinecraftVersion.UNKNOWN.isAtLeast(MinecraftVersion.MINECRAFT_1_17));

        Assertions.assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.MINECRAFT_1_16.isAtLeast(null));
    }

    @Test
    @DisplayName("Test if Minecraft versions are ordered correctly (#isBefore)")
    void testIsBefore() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_16.isBefore(MinecraftVersion.MINECRAFT_1_17));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_16.isBefore(MinecraftVersion.MINECRAFT_1_18));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_17.isBefore(MinecraftVersion.MINECRAFT_1_17));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_17.isBefore(MinecraftVersion.MINECRAFT_1_16));
    }

    @Test
    @DisplayName("Test if Minecraft versions #isBefore behaves correctly for minor versions")
    void testIsBeforeMinor() {
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_18.isBefore(16, 5));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_18.isBefore(17, 1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_18.isBefore(18, 0));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_18.isBefore(18, 1));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20.isBefore(20, 0));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20.isBefore(20, 2));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20.isBefore(20, 4));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20.isBefore(20, 5));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20_5.isBefore(20, 4));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20_5.isBefore(20, 5));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20_5.isBefore(20, 6));
    }

    @Test
    @DisplayName("Test correct behaviour for MinecraftVersion.UNKNOWN.isBefore(...)")
    void testIsBeforeUnknown() {
        // Unknown should always fall back to true
        Assertions.assertTrue(MinecraftVersion.UNKNOWN.isBefore(MinecraftVersion.MINECRAFT_1_16));
        Assertions.assertTrue(MinecraftVersion.UNKNOWN.isBefore(MinecraftVersion.MINECRAFT_1_17));

        Assertions.assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.MINECRAFT_1_16.isBefore(null));
    }

    @Test
    @DisplayName("Test warning system for lowest supported version checks")
    void testLowestSupportedVersion() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.UNIT_TEST.isAtLeast(MinecraftVersion.MINECRAFT_1_16));
    }

    @ParameterizedTest(name = "\"{0}\" is parsed as [{1}, {2}]")
    @CsvSource(delimiter = '|', value = {
        // Bukkit.getVersion() on legacy versions
        "git-Paper-123 (MC: 1.21.1)                | 21 | 1",
        "git-Paper-50 (MC: 1.21)                   | 21 | 0",
        "4321-Spigot-abc-def (MC: 1.21.11)         | 21 | 11",
        "git-Paper-1 (MC: 1.16.5)                  | 16 | 5",
        // Bukkit.getVersion() on year-based versions
        "26.3-249-abcdef (MC: 26.3)                | 26 | 3",
        "26.3.1-12-abcdef (MC: 26.3.1)             | 26 | 3",
        "26.1-1-abcdef (MC: 26.1)                  | 26 | 1",
        // Bukkit.getBukkitVersion()
        "1.21.1-R0.1-SNAPSHOT                      | 21 | 1",
        "1.20.6-R0.1-SNAPSHOT                      | 20 | 6",
        "26.3.build.249-stable                     | 26 | 3",
        "26.3-R0.1-SNAPSHOT                        | 26 | 3",
    })
    void testParseVersion(String versionString, int expectedMajor, int expectedPatch) {
        int[] version = MinecraftVersion.parseVersion(versionString);

        Assertions.assertNotNull(version);
        Assertions.assertArrayEquals(new int[] { expectedMajor, expectedPatch }, version);
    }

    @ParameterizedTest(name = "\"{0}\" cannot be parsed")
    @NullAndEmptySource
    @ValueSource(strings = { "Unknown", "git-Paper-123", "(MC: x.y)", "0.30-classic" })
    void testParseInvalidVersion(String versionString) {
        Assertions.assertNull(MinecraftVersion.parseVersion(versionString));
    }

    @Test
    @DisplayName("Test that the (MC: ...) part takes precedence over a leading build number")
    void testParseVersionPrefersMinecraftSection() {
        Assertions.assertArrayEquals(new int[] { 26, 3 }, MinecraftVersion.parseVersion("1.2.3-custom (MC: 26.3)"));
    }

    @ParameterizedTest(name = "\"{0}\" resolves to {1}")
    @CsvSource(delimiter = '|', value = {
        "git-Paper-123 (MC: 1.21.1)   | MINECRAFT_1_21",
        "git-Paper-123 (MC: 1.21.11)  | MINECRAFT_1_21",
        "git-Paper-123 (MC: 1.20.4)   | MINECRAFT_1_20",
        "git-Paper-123 (MC: 1.20.6)   | MINECRAFT_1_20_5",
        "26.3-249-abcdef (MC: 26.3)   | MINECRAFT_26_3",
        "26.3.1-12-abcdef (MC: 26.3.1)| MINECRAFT_26_3",
        "26.3.build.249-stable        | MINECRAFT_26_3",
        // Not supported (yet)
        "26.1-1-abcdef (MC: 26.1)     | UNKNOWN",
        "26.2-1-abcdef (MC: 26.2)     | UNKNOWN",
        "27.1-1-abcdef (MC: 27.1)     | UNKNOWN",
        "git-Paper-1 (MC: 1.15.2)     | UNKNOWN",
    })
    void testResolveVersion(String versionString, MinecraftVersion expected) {
        int[] version = MinecraftVersion.parseVersion(versionString);

        Assertions.assertNotNull(version);
        Assertions.assertEquals(expected, MinecraftVersion.of(version[0], version[1]));
    }

    @Test
    @DisplayName("Test that Minecraft 26.3 matches itself and later 26.x drops")
    void testMatches26() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isMinecraftVersion(26, 3));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isMinecraftVersion(26, 4));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_3.isMinecraftVersion(26, 2));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_3.isMinecraftVersion(21, 3));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_21.isMinecraftVersion(26, 3));
    }

    @Test
    @DisplayName("Test that Minecraft 26.3 is ordered after every 1.x version")
    void testOrdering26() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isAtLeast(MinecraftVersion.MINECRAFT_1_21));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isAtLeast(MinecraftVersion.MINECRAFT_1_20_5));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isAtLeast(MinecraftVersion.MINECRAFT_1_17));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isAtLeast(MinecraftVersion.MINECRAFT_26_3));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_21.isAtLeast(MinecraftVersion.MINECRAFT_26_3));

        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_21.isBefore(MinecraftVersion.MINECRAFT_26_3));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_3.isBefore(MinecraftVersion.MINECRAFT_1_21));
    }

    @Test
    @DisplayName("Test that legacy patch checks (#isBefore(int, int)) behave correctly on 26.3")
    void testIsBeforeMinor26() {
        // These are the checks used by the AutoBrewer and FluidPump
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_3.isBefore(20, 2));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_3.isBefore(21, 11));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_3.isBefore(26, 3));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isBefore(26, 4));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_3.isBefore(27, 1));
    }

}
