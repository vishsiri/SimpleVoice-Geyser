package io.github.theodoremeyer.simplevoicegeyser.core.server.connection.auth;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Bounds concurrent ticket consumption and rejects duplicate in-flight tickets.
 */
final class PendingTicketRegistry {

    private final Set<String> tickets = ConcurrentHashMap.newKeySet();
    private final Semaphore permits;

    PendingTicketRegistry(int maxPending) {
        if (maxPending < 1) {
            throw new IllegalArgumentException("maxPending must be positive");
        }
        this.permits = new Semaphore(maxPending);
    }

    boolean tryStart(String ticket) {
        if (!tickets.add(ticket)) {
            return false;
        }
        if (permits.tryAcquire()) {
            return true;
        }
        tickets.remove(ticket);
        return false;
    }

    void finish(String ticket) {
        if (tickets.remove(ticket)) {
            permits.release();
        }
    }
}
