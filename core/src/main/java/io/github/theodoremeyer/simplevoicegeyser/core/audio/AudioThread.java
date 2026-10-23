package io.github.theodoremeyer.simplevoicegeyser.core.audio;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Server's Audio Thread
 */
public final class AudioThread {

    private static final int MAX_PENDING_TASKS = 256;
    private static AudioThread instance;

    private final ThreadPoolExecutor executor;

    /**
     * EntryPoint
     */
    public AudioThread() {
        this(MAX_PENDING_TASKS);
    }

    AudioThread(int maxPendingTasks) {
        if (maxPendingTasks < 1) {
            throw new IllegalArgumentException("maxPendingTasks must be positive");
        }

        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxPendingTasks), r -> {
            Thread t = new Thread(r, "SVG-Audio");
            t.setDaemon(true);
            return t;
        }, new ThreadPoolExecutor.AbortPolicy());
        instance = this;
    }

    /**
     * Stop Thread
     */
    public static void shutdown() {
        AudioThread current = instance;
        if (current != null) {
            current.executor.shutdownNow();
        }
    }

    /**
     * Gets the executor to execute on Audio Thread
     * @return executor
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Execute code on the thread
     * @param runnable code to execute
     */
    public static void execute(Runnable runnable) {
        tryExecute(runnable);
    }

    /**
     * Try to enqueue code without blocking when the audio queue is full.
     * @param runnable code to execute
     * @return whether the task was accepted
     */
    public static boolean tryExecute(Runnable runnable) {
        AudioThread current = instance;
        if (current == null || current.executor.isShutdown()) {
            return false;
        }

        try {
            current.executor.execute(runnable);
            return true;
        } catch (RejectedExecutionException ignored) {
            return false;
        }
    }
}
