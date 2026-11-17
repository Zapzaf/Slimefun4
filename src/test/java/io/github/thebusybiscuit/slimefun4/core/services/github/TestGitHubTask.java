package io.github.thebusybiscuit.slimefun4.core.services.github;

import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

class TestGitHubTask {

    private static final String REPO = "Zapzaf/Slimefun4";
    private static final String TEXTURE = "dGVzdC1za2luLXRleHR1cmU=";

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
    @DisplayName("A skin saved by an earlier run is used without asking Mojang (avoids HTTP 429 on every restart)")
    void cachedSkinNeedsNoRequest() {
        // An earlier run looked the skin up and saved it
        GitHubService earlierRun = new GitHubService(REPO);
        Contributor before = earlierRun.addContributor("CachedSkinTester", "https://github.com/CachedSkinTester", "developer", 1);
        before.setTexture(TEXTURE);
        earlierRun.saveCache();

        // After a restart the contributor has no skin in memory yet
        GitHubService restarted = new GitHubService(REPO);
        Contributor contributor = restarted.addContributor("CachedSkinTester", "https://github.com/CachedSkinTester", "developer", 1);
        Assertions.assertFalse(contributor.hasTexture());

        int requests = new GitHubTask(restarted).requestTexture(contributor, new HashMap<>());

        Assertions.assertEquals(0, requests, "no request to Mojang");
        Assertions.assertTrue(contributor.hasTexture());
        Assertions.assertEquals(TEXTURE, contributor.getTexture(restarted));
    }
}
