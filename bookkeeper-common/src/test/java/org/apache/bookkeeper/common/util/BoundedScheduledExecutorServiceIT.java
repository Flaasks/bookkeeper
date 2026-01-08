/*
 * Integration tests for BoundedScheduledExecutorService using real ScheduledThreadPoolExecutor.
 */
package org.apache.bookkeeper.common.util;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class BoundedScheduledExecutorServiceIT {

    private ScheduledThreadPoolExecutor executor;
    private BoundedScheduledExecutorService bounded;

    @After
    public void tearDown() {
        if (bounded != null) {
            bounded.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Verifica che più task schedulati con ritardo vengano eseguiti correttamente entro il limite.
     */
    @Test
    public void testScheduledTasksWithinLimitExecute() throws Exception {
        executor = new ScheduledThreadPoolExecutor(1);
        bounded = new BoundedScheduledExecutorService(executor, 5);

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger counter = new AtomicInteger();

        bounded.schedule(() -> {
            counter.incrementAndGet();
            latch.countDown();
        }, 50, TimeUnit.MILLISECONDS);

        bounded.schedule(() -> {
            counter.incrementAndGet();
            latch.countDown();
        }, 50, TimeUnit.MILLISECONDS);

        assertTrue("I task dovrebbero completare entro il timeout", latch.await(1, TimeUnit.SECONDS));
        assertEquals("Entrambi i task devono essere eseguiti", 2, counter.get());
    }

    /**
     * Verifica che venga lanciata una RejectedExecutionException quando la coda è già al limite.
     */
    @Test(expected = RejectedExecutionException.class)
    public void testScheduleRejectsWhenQueueAtLimit() {
        executor = new ScheduledThreadPoolExecutor(1);
        bounded = new BoundedScheduledExecutorService(executor, 1);

        // Primo task schedulato occupa l'unico slot di coda (ritardo per restare in coda)
        bounded.schedule(() -> { /* no-op */ }, 1, TimeUnit.SECONDS);

        // Secondo task oltre il limite deve essere rifiutato
        bounded.schedule(() -> { /* no-op */ }, 1, TimeUnit.SECONDS);
    }

    /**
     * Verifica che invokeAll esegua tutti i task quando il batch è entro il limite configurato.
     */
    @Test
    public void testInvokeAllRunsWithinLimit() throws Exception {
        executor = new ScheduledThreadPoolExecutor(2);
        bounded = new BoundedScheduledExecutorService(executor, 4);

        List<Callable<Integer>> tasks = Arrays.asList(
                () -> {
                    TimeUnit.MILLISECONDS.sleep(25);
                    return 1;
                },
                () -> 2
        );

        List<Future<Integer>> results = bounded.invokeAll(tasks);

        assertEquals("Numero di risultati atteso", 2, results.size());
        assertEquals(Integer.valueOf(1), results.get(0).get(500, TimeUnit.MILLISECONDS));
        assertEquals(Integer.valueOf(2), results.get(1).get(500, TimeUnit.MILLISECONDS));
    }
}
