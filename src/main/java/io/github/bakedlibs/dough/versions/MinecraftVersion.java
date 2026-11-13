package io.github.bakedlibs.dough.versions;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Fork override of dough's {@code MinecraftVersion}.
 * <p>
 * The upstream implementation passes {@link Server#getBukkitVersion()} (minus the {@literal "-..."} suffix)
 * straight to {@link SemanticVersion#parse(String)}. Since the year-based versioning scheme (26.x),
 * Paper and its forks report strings such as {@literal "26.3.build.2658-stable"}, which that parser rejects.
 * This made dough's item name adapter fail to initialize, so every item name resolved to {@literal "unknown"}.
 * <p>
 * This class keeps the exact public API of the upstream class, but reads the leading
 * {@literal <major>.<minor>[.<patch>]} numbers and ignores anything after them.
 * The original class is excluded from the shaded jar in {@code pom.xml}.
 */
public class MinecraftVersion extends SemanticVersion {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public MinecraftVersion(int major, int minor, int patch) {
        super(major, minor, patch);
    }

    public static @Nonnull MinecraftVersion of(@Nonnull Server server) throws UnknownServerVersionException {
        Objects.requireNonNull(server, "Server should not be null!");
        String bukkitVersion = server.getBukkitVersion();

        try {
            String version = bukkitVersion.split("-")[0].trim();
            Matcher matcher = VERSION_PATTERN.matcher(version);

            if (matcher.find()) {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                int patch = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
                return new MinecraftVersion(major, minor, patch);
            }

            SemanticVersion semver = SemanticVersion.parse(version);
            return new MinecraftVersion(semver.getMajorVersion(), semver.getMinorVersion(), semver.getPatchVersion());
        } catch (Exception x) {
            throw new UnknownServerVersionException(bukkitVersion, x);
        }
    }

    public static @Nonnull MinecraftVersion get() throws UnknownServerVersionException {
        return of(Bukkit.getServer());
    }

    public static boolean isMocked(@Nonnull Server server) {
        Class<?> clazz = server.getClass();

        while (clazz != null) {
            if (clazz.getName().endsWith("mockbukkit.ServerMock")) {
                return true;
            }

            clazz = clazz.getSuperclass();
        }

        return false;
    }

    public static boolean isMocked() {
        return isMocked(Bukkit.getServer());
    }

    @Override
    public @Nonnull String getAsString() {
        return "Minecraft " + super.getAsString();
    }

}
