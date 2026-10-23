package io.github.theodoremeyer.simplevoicegeyser.core.audio;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioThreadTest {

    @Test
    void rejectsWorkWhenTheBoundedQueueIsFull() throws Exception {
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        new AudioThread(1);

        try {
            assertTrue(AudioThread.tryExecute(() -> {
                running.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertTrue(running.await(2, TimeUnit.SECONDS));
            assertTrue(AudioThread.tryExecute(() -> { }));
            assertFalse(AudioThread.tryExecute(() -> { }));
        } finally {
            release.countDown();
            AudioThread.shutdown();
        }
    }
}
