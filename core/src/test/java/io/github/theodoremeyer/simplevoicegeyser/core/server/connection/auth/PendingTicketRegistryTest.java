package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingTicketRegistryTest {

    @Test
    void rejectsDuplicateAndExcessPendingTicketsUntilReleased() {
        PendingTicketRegistry registry = new PendingTicketRegistry(2);

        assertTrue(registry.tryStart("ticket-a"));
        assertFalse(registry.tryStart("ticket-a"));
        assertTrue(registry.tryStart("ticket-b"));
        assertFalse(registry.tryStart("ticket-c"));

        registry.finish("ticket-a");
        assertTrue(registry.tryStart("ticket-c"));

        registry.finish("ticket-a");
        assertFalse(registry.tryStart("ticket-d"));
    }
}
