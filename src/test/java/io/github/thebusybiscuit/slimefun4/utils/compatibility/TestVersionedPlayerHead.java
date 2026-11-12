package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;

import be.seeseemelk.mockbukkit.MockBukkit;

class TestVersionedPlayerHead {

    private static final String HASH = HeadTexture.CAPACITOR_25.getTexture();
    private static final String URL = "http://textures.minecraft.net/texture/" + HASH;

    @BeforeAll
    public static void load() {
        MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test that the skin url is read from a Base64 texture")
    void testGetTextureUrl() {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + URL + "\"}}}";
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(URL, VersionedPlayerHead.getTextureUrl(base64).toString());
    }

    @Test
    @DisplayName("Test that #toBase64 and #getTextureUrl round-trip")
    void testBase64RoundTrip() {
        Assertions.assertEquals(URL, VersionedPlayerHead.getTextureUrl(VersionedPlayerHead.toBase64(URL)).toString());
    }

    @Test
    @DisplayName("Test that invalid textures are rejected")
    void testInvalidTexture() {
        String base64 = Base64.getEncoder().encodeToString("{\"textures\":{\"SKIN\":{\"url\":\"not a url\"}}}".getBytes(StandardCharsets.UTF_8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> VersionedPlayerHead.getTextureUrl(base64));
    }

    @Test
    @DisplayName("Test that the profile UUID matches the one dough generates")
    void testUuidMatchesDough() {
        // dough uses UUID.nameUUIDFromBytes(texture) - heads must keep that UUID to stay similar after updating
        UUID expected = UUID.nameUUIDFromBytes(HASH.getBytes(StandardCharsets.UTF_8));

        Assertions.assertEquals(expected, VersionedPlayerHead.uuidOf(HASH));
        Assertions.assertEquals(HeadTexture.CAPACITOR_25.getUniqueId(), VersionedPlayerHead.uuidOf(HASH));
    }

    @Test
    @DisplayName("Test that the profile matches the one dough builds (UUID, name and skin)")
    void testCreateProfile() throws Exception {
        UUID uuid = VersionedPlayerHead.uuidOf(HASH);
        PlayerProfile profile = VersionedPlayerHead.createProfile(uuid, new java.net.URI(URL).toURL());

        Assertions.assertEquals(uuid, profile.getUniqueId());
        Assertions.assertEquals("CS-CoreLib", profile.getName());
        Assertions.assertNotNull(profile.getTextures().getSkin());
        Assertions.assertEquals(URL, profile.getTextures().getSkin().toString());
    }

    @Test
    @DisplayName("Test that a custom head ItemStack is a player head owned by the texture profile")
    void testGetItemStack() {
        ItemStack item = VersionedPlayerHead.getItemStack(VersionedPlayerHead.toBase64(URL));

        Assertions.assertEquals(Material.PLAYER_HEAD, item.getType());
        Assertions.assertInstanceOf(SkullMeta.class, item.getItemMeta());

        /*
         * MockBukkit's SkullMeta only keeps the name and UUID of an owner profile
         * (textures are dropped and getOwnerProfile() returns null), so the texture itself
         * is verified on the profile in #testCreateProfile() instead.
         */
    }

    @Test
    @DisplayName("Test that heads with the same texture are similar (so they stack)")
    void testSameTextureIsSimilar() {
        String base64 = VersionedPlayerHead.toBase64(URL);
        ItemStack a = VersionedPlayerHead.getItemStack(base64);
        ItemStack b = VersionedPlayerHead.getItemStack(base64);

        Assertions.assertTrue(a.isSimilar(b));
    }
}
