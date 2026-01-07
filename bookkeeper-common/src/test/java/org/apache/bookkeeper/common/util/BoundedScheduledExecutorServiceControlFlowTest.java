/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.bookkeeper.common.util;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test Control-Flow per BoundedScheduledExecutorService
 */
public class BoundedScheduledExecutorServiceControlFlowTest {
    private AdjustableBlockingQueue queue;
    private ScheduledThreadPoolExecutor executor;

    @Before
    public void setUp() {
        queue = new AdjustableBlockingQueue();
        executor = new StubScheduledThreadPoolExecutor(queue);
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * Simple stub that exposes a controllable queue while avoiding Mockito on JDK class.
     */
    private static final class StubScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
        private final BlockingQueue<Runnable> exposedQueue;

        StubScheduledThreadPoolExecutor(BlockingQueue<Runnable> exposedQueue) {
            super(1);
            this.exposedQueue = exposedQueue;
        }

        @Override
        public BlockingQueue<Runnable> getQueue() {
            return exposedQueue;
        }
    }

    /**
     * Minimal queue stub with controllable size and call counter.
     */
    private static final class AdjustableBlockingQueue implements BlockingQueue<Runnable> {
        private final AtomicInteger reportedSize = new AtomicInteger();
        private final AtomicInteger sizeCalls = new AtomicInteger();

        void setReportedSize(int size) {
            reportedSize.set(size);
        }

        int getSizeCalls() {
            return sizeCalls.get();
        }

        @Override
        public int size() {
            sizeCalls.incrementAndGet();
            return reportedSize.get();
        }

        // The rest of the methods are not used in these tests.
        @Override public boolean add(Runnable runnable) { throw new UnsupportedOperationException(); }
        @Override public boolean offer(Runnable runnable) { throw new UnsupportedOperationException(); }
        @Override public void put(Runnable runnable) { throw new UnsupportedOperationException(); }
        @Override public boolean offer(Runnable runnable, long l, TimeUnit timeUnit) { throw new UnsupportedOperationException(); }
        @Override public Runnable take() { throw new UnsupportedOperationException(); }
        @Override public Runnable poll(long l, TimeUnit timeUnit) { throw new UnsupportedOperationException(); }
        @Override public int remainingCapacity() { throw new UnsupportedOperationException(); }
        @Override public boolean remove(Object o) { throw new UnsupportedOperationException(); }
        @Override public boolean contains(Object o) { throw new UnsupportedOperationException(); }
        @Override public int drainTo(Collection<? super Runnable> collection) { throw new UnsupportedOperationException(); }
        @Override public int drainTo(Collection<? super Runnable> collection, int i) { throw new UnsupportedOperationException(); }
        @Override public boolean removeAll(Collection<?> collection) { throw new UnsupportedOperationException(); }
        @Override public boolean retainAll(Collection<?> collection) { throw new UnsupportedOperationException(); }
        @Override public boolean containsAll(Collection<?> collection) { throw new UnsupportedOperationException(); }
        @Override public boolean addAll(Collection<? extends Runnable> c) { throw new UnsupportedOperationException(); }
        @Override public Object[] toArray() { throw new UnsupportedOperationException(); }
        @Override public <T> T[] toArray(T[] ts) { throw new UnsupportedOperationException(); }
        @Override public Iterator<Runnable> iterator() { throw new UnsupportedOperationException(); }
        @Override public Runnable remove() { throw new UnsupportedOperationException(); }
        @Override public Runnable poll() { throw new UnsupportedOperationException(); }
        @Override public Runnable element() { throw new UnsupportedOperationException(); }
        @Override public Runnable peek() { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public boolean isEmpty() { return size() == 0; }
    }

    /**
     * CF-TEST-1: Branch maxTasksInQueue = 0 (no limit)
     */
    @Test
    public void testCheckQueueWithZeroLimit() {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, 0);

        queue.setReportedSize(1000);

        service.execute(() -> { });
        service.submit(() -> null);

        assertEquals(0, queue.getSizeCalls());
    }

    /**
     * CF-TEST-2: Branch (queue.size() + numberOfTasks) = maxTasksInQueue 
     */
    @Test
    public void testCheckQueueAtExactBoundary() {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, 10);

        // Setup: queue.size = 9, numberOfTasks = 1, totale = 10 
        queue.setReportedSize(9);

        Runnable task = () -> { };

        try {
            service.schedule(task, 1L, TimeUnit.SECONDS);

        } catch (RejectedExecutionException e) {
            fail("Non dovrebbe lanciare eccezione al boundary esatto");
        }

        // Verifica che checkQueue abbia controllato
        assertEquals(1, queue.getSizeCalls());
    }

    /**
     * CF-TEST-3: Branch numberOfTasks > 1 con invokeAll
     */
    @Test
    public void testCheckQueueWithMultipleTasks() {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, 10);

        queue.setReportedSize(6);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int value = i; // Final per lambda
            tasks.add(() -> value);
        }

        try {
            service.invokeAll(tasks);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            assertEquals("Queue at limit of 10 items", e.getMessage());
        } catch (InterruptedException e) {
            fail("Non dovrebbe essere interrotto");
        }

        assertEquals(1, queue.getSizeCalls());
    }

    /**
     * CF-TEST-4: Path con queue size che diventa esattamente maxTasksInQueue
     */
    @Test
    public void testCheckQueueWithZeroTasksAtLimit() throws Exception {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, 10);

        queue.setReportedSize(10);

        List<Callable<String>> emptyTasks = Collections.emptyList();

        try {
            service.invokeAny(emptyTasks);
        } catch (Exception e) {
            // Ignora eccezioni dall'executor
        }

        assertEquals(1, queue.getSizeCalls());
    }

    /**
     * CF-TEST-5: Verifica che ogni metodo chiami checkQueue con numberOfTasks = 1
     */
    @Test
    public void testAllMethodsCallCheckQueue() {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, 100); // Limite alto

        queue.setReportedSize(0); // Queue vuota

        // Chiama tutti i metodi singoli (numberOfTasks = 1)
        Runnable r = () -> { };
        Callable<String> c = () -> "result";

        try {
            service.execute(r);
            service.submit(r);
            service.submit(c);
            service.submit(r, "result");
            service.schedule(r, 1L, TimeUnit.SECONDS);
            service.schedule(c, 1L, TimeUnit.SECONDS);
            service.scheduleAtFixedRate(r, 0L, 1L, TimeUnit.SECONDS);
            service.scheduleWithFixedDelay(r, 0L, 1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignora eccezioni dal decorator
        }

        // Verify che checkQueue sia stato chiamato 8 volte (una per metodo)
        assertEquals(8, queue.getSizeCalls());
    }

    /**
     * CF-TEST-6: Branch con overflow di esattamente 1
     */
    @Test
    public void testCheckQueueOverflowByOne() {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, 5);

        queue.setReportedSize(5);

        Runnable task = () -> { };

        try {
            service.execute(task);
            fail("Dovrebbe lanciare eccezione");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit of 5 items"));
        }

        assertEquals(1, queue.getSizeCalls());
    }

    /**
     * CF-TEST-7: Test con invokeAny
     */
    @Test
    public void testInvokeAnyNumberOfTasks() throws Exception {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, 5);

        queue.setReportedSize(2);

        List<Callable<String>> tasks = Arrays.asList(
                () -> "1", () -> "2", () -> "3", () -> "4"
        );

        try {
            service.invokeAny(tasks);
            fail("Dovrebbe fallire check");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit"));
        }

        assertEquals(1, queue.getSizeCalls());
    }

    /**
     * CF-TEST-8: Test ramo maxTasksInQueue negativo
     */
    @Test
    public void testNegativeLimitBehavesAsNoLimit() {
        BoundedScheduledExecutorService service =
            new BoundedScheduledExecutorService(executor, -10);

        queue.setReportedSize(Integer.MAX_VALUE);

        service.execute(() -> { });

        List<Callable<String>> manyTasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyTasks.add(() -> "task");
        }

        try {
            service.invokeAll(manyTasks, 1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            assertFalse(e instanceof RejectedExecutionException);
        }

        assertEquals(0, queue.getSizeCalls());
    }

}