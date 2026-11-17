package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.google.gson.JsonObject;

import io.github.bakedlibs.dough.skins.PlayerHead;
import io.github.bakedlibs.dough.skins.PlayerSkin;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.JsonUtils;

/**
 * Creates and applies custom player head textures.
 * <p>
 * dough's skin classes subclass {@literal com.mojang.authlib.GameProfile}, which became
 * {@literal final} in Minecraft 26.x. Loading them there fails with an
 * {@link IncompatibleClassChangeError}. On 26.3+ this class therefore uses the Bukkit
 * {@link PlayerProfile} API instead, and delegates to dough on older versions so
 * that their behaviour is unchanged.
 * <p>
 * The profiles built here are identical to the ones dough builds (same {@link UUID},
 * name and skin URL), so heads created before and after the update stay similar.
 */
public final class VersionedPlayerHead {

    private static final String TEXTURE_URL_PREFIX = "http://textures.minecraft.net/texture/";

    /**
     * The profile name dough has always used for custom heads.
     */
    private static final String PROFILE_NAME = "CS-CoreLib";

    private VersionedPlayerHead() {}

    /**
     * This returns a player head {@link ItemStack} with the given texture.
     *
     * @param base64
     *            The Base64-encoded texture json
     *
     * @return A custom head {@link ItemStack}
     */
    public static @Nonnull ItemStack getItemStack(@Nonnull String base64) {
        if (!useBukkitProfiles()) {
            return PlayerHead.getItemStack(PlayerSkin.fromBase64(base64));
        }

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwnerProfile(createProfile(uuidOf(base64), getTextureUrl(base64)));
            item.setItemMeta(skullMeta);
        }

        return item;
    }

    /**
     * This applies the given texture to a player head {@link Block}.
     *
     * @param block
     *            The player head {@link Block}
     * @param base64
     *            The Base64-encoded texture json
     * @param sendUpdate
     *            Whether to send a block update (only used on versions before 26.3)
     */
    @ParametersAreNonnullByDefault
    public static void setSkin(Block block, String base64, boolean sendUpdate) {
        if (!useBukkitProfiles()) {
            PlayerHead.setSkin(block, PlayerSkin.fromBase64(base64), sendUpdate);
            return;
        }

        applyProfile(block, createProfile(uuidOf(base64), getTextureUrl(base64)));
    }

    /**
     * This applies the texture with the given hash to a player head {@link Block}.
     *
     * @param block
     *            The player head {@link Block}
     * @param uuid
     *            The {@link UUID} of the profile
     * @param hash
     *            The texture hash (as used on textures.minecraft.net)
     * @param sendUpdate
     *            Whether to send a block update (only used on versions before 26.3)
     */
    @ParametersAreNonnullByDefault
    public static void setSkinFromHash(Block block, UUID uuid, String hash, boolean sendUpdate) {
        if (!useBukkitProfiles()) {
            PlayerHead.setSkin(block, PlayerSkin.fromHashCode(uuid, hash), sendUpdate);
            return;
        }

        applyProfile(block, createProfile(uuid, toUrl(TEXTURE_URL_PREFIX + hash)));
    }

    /**
     * This applies the texture with the given hash to a player head {@link Block},
     * using a {@link UUID} derived from the hash.
     *
     * @param block
     *            The player head {@link Block}
     * @param hash
     *            The texture hash (as used on textures.minecraft.net)
     * @param sendUpdate
     *            Whether to send a block update (only used on versions before 26.3)
     */
    @ParametersAreNonnullByDefault
    public static void setSkinFromHash(Block block, String hash, boolean sendUpdate) {
        setSkinFromHash(block, uuidOf(hash), hash, sendUpdate);
    }

    /**
     * This looks up the skin of the given player and returns it as a Base64-encoded texture json.
     *
     * @param plugin
     *            The {@link Plugin} performing the lookup
     * @param uuid
     *            The {@link UUID} of the player
     *
     * @return A {@link CompletableFuture} with the texture, or {@literal null} if the player has no skin.
     *         It completes exceptionally if the profile could not be fetched (e.g. rate-limiting).
     */
    @ParametersAreNonnullByDefault
    public static @Nonnull CompletableFuture<String> fetchTexture(Plugin plugin, UUID uuid) {
        if (!useBukkitProfiles()) {
            return PlayerSkin.fromPlayerUUID(plugin, uuid).thenApply(skin -> skin.getProfile().getBase64Texture());
        }

        return Bukkit.createPlayerProfile(uuid).update().thenApply(VersionedPlayerHead::textureOf);
    }

    /**
     * This returns the skin of an updated {@link PlayerProfile} as a Base64-encoded texture json.
     * <p>
     * When Mojang rate-limits us (HTTP 429) or is unavailable, the server only logs a warning and
     * returns the profile without any textures. Every real profile has them (even players with a
     * default skin), so a missing one is treated as a failed lookup. This lets callers back off
     * instead of firing more requests.
     * <p>
     * Only the Bukkit {@link PlayerProfile} API is used here: on Paper 26.3 the Bukkit profile also
     * implements Paper's legacy {@literal com.destroystokyo.paper.profile.PlayerProfile}, but every
     * method of that interface throws {@link UnsupportedOperationException} ("Do not cast to ...").
     *
     * @param profile
     *            The updated {@link PlayerProfile}
     *
     * @return The texture, or {@literal null} if the player has no custom skin
     */
    static @Nullable String textureOf(@Nonnull PlayerProfile profile) {
        PlayerTextures textures = profile.getTextures();

        if (textures.isEmpty()) {
            throw new IllegalStateException("Mojang did not return a profile for " + profile.getUniqueId() + ", likely rate-limited (HTTP 429)");
        }

        URL skin = textures.getSkin();
        return skin == null ? null : toBase64(skin.toString());
    }

    /**
     * This extracts the skin URL from a Base64-encoded texture json.
     *
     * @param base64
     *            The Base64-encoded texture json
     *
     * @return The skin {@link URL}
     */
    static @Nonnull URL getTextureUrl(@Nonnull String base64) {
        String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        JsonObject textures = JsonUtils.parseString(json).getAsJsonObject().getAsJsonObject("textures");
        return toUrl(textures.getAsJsonObject("SKIN").get("url").getAsString());
    }

    /**
     * This returns the {@link UUID} dough uses for a texture, so heads remain similar across versions.
     *
     * @param texture
     *            The texture (Base64 json or hash)
     *
     * @return The {@link UUID} for this texture
     */
    static @Nonnull UUID uuidOf(@Nonnull String texture) {
        return UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
    }

    static @Nonnull String toBase64(@Nonnull String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @ParametersAreNonnullByDefault
    static @Nonnull PlayerProfile createProfile(UUID uuid, URL skin) {
        PlayerProfile profile = Bukkit.createPlayerProfile(uuid, PROFILE_NAME);
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(skin);
        profile.setTextures(textures);
        return profile;
    }

    @ParametersAreNonnullByDefault
    private static void applyProfile(Block block, PlayerProfile profile) {
        BlockState state = block.getState();

        if (state instanceof Skull skull) {
            skull.setOwnerProfile(profile);
            skull.update(true, false);
        }
    }

    private static @Nonnull URL toUrl(@Nonnull String url) {
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid skin url: " + url, e);
        }
    }

    private static boolean useBukkitProfiles() {
        @Nullable
        Slimefun instance = Slimefun.instance();
        return instance == null || Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_26_3);
    }
}
