package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestVersionedAttribute {

    @Test
    @DisplayName("Test that the max health attribute resolves on the current API version")
    void testMaxHealthResolves() {
        Assertions.assertNotNull(VersionedAttribute.MAX_HEALTH);

        // "generic.max_health" before 1.21.3, "max_health" on 1.21.3+ and 26.x
        String key = VersionedAttribute.MAX_HEALTH.getKey().getKey();
        Assertions.assertTrue(key.endsWith("max_health"), () -> "Unexpected attribute key: " + key);
    }
}
