package io.github.theodoremeyer.simplevoicegeyser.core.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginValidatorTest {

    @Test
    void acceptsOnlyTheExactConfiguredHttpsOrigin() {
        String allowed = "https://voicechat.minedream.city";

        assertTrue(OriginValidator.allows(allowed, allowed));
        assertFalse(OriginValidator.allows(null, allowed));
        assertFalse(OriginValidator.allows("https://evil.example", allowed));
        assertFalse(OriginValidator.allows("https://voicechat.minedream.city/", allowed));
        assertFalse(OriginValidator.allows(allowed, "http://voicechat.minedream.city"));
    }
}
