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
import static org.mockito.Mockito.*;

/**
 * Test generati tramite LLM per BoundedScheduledExecutorService
 * Focus su: edge cases, bug detection, comportamenti concorrenti
 */
public class BoundedScheduledExecutorServiceLLMTest {
    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    private AdjustableBlockingQueue queue;
    private StubScheduledThreadPoolExecutor executor;
    private BoundedScheduledExecutorService boundedService;

    @Before
    public void setUp() {
        queue = new AdjustableBlockingQueue();
        executor = new StubScheduledThreadPoolExecutor(queue);
        boundedService = new BoundedScheduledExecutorService(executor, 10);
    }

    @After
    public void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }


    /**
     * LLM-TEST-1: Comportamento di base - singolo task
     */
    @Test
    public void testBasicExecute() {
        // Setup
        queue.setReportedSize(0);

        Runnable task = mock(Runnable.class);

        // Execute
        boundedService.execute(task);

        // Verify
        assertEquals(1, queue.getSizeCalls());
        assertEquals(1, executor.getExecuteCalls());
    }
    
    /**
     * LLM-TEST-2: Edge case - limite esattamente 1
     */
    @Test
    public void testSingleTaskLimit() {
        // Setup con limite 1
        queue = new AdjustableBlockingQueue();
        executor = new StubScheduledThreadPoolExecutor(queue);
        boundedService = new BoundedScheduledExecutorService(executor, 1);
        queue.setReportedSize(0);

        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);

        // Prima esecuzione OK
        boundedService.execute(task1);
        assertEquals(1, executor.getExecuteCalls());

        // Seconda esecuzione fallisce
        queue.setReportedSize(1);

        try {
            boundedService.execute(task2);
            fail("Dovrebbe fallire con queue piena");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit of 1 items"));
        }
    }

    /**
     * LLM-TEST-3: InvokeAny con collection vuota
     */
    @Test
    public void testInvokeAnyWithEmptyCollection() throws Exception {
        // Setup
        queue.setReportedSize(0);
        List<Callable<String>> emptyTasks = Collections.emptyList();

        // Execute
        try {
            boundedService.invokeAny(emptyTasks);
        } catch (IllegalArgumentException e) {
            // Atteso per collection vuota
        }

        // Verify
        assertEquals(1, queue.getSizeCalls());
    }

    /**
     * LLM-TEST-4: Verifica che i controlli non permettano overflow
     */
    @Test
    public void testQueueOverflowPrevention() {
        // Setup con queue prossima al limite (9/10)
        queue.setReportedSize(9);

        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);

        // Primo task OK (9+1=10)
        boundedService.execute(task1);
        assertEquals(1, executor.getExecuteCalls());

        // La queue è ora al limite (per la seconda execute)
        queue.setReportedSize(10);

        // Secondo task fallisce (queue ora è al limite)
        try {
            boundedService.execute(task2);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit"));
        }

        // Verify che il secondo task non sia stato eseguito
        assertEquals(1, executor.getExecuteCalls());
    }

    /**
     * LLM-TEST-5: Submit con result null
     */
    @Test
    public void testSubmitRunnableWithNullResult() {
        // Setup
        queue.setReportedSize(5);

        Runnable task = mock(Runnable.class);

        // Execute
        boundedService.submit(task, null);

        // Verify che checkQueue sia stato chiamato
        assertEquals(1, queue.getSizeCalls());

        // Verify che execute sia stato chiamato
        assertEquals(1, executor.getExecuteCalls());
    }

    /**
     * LLM-TEST-6: InvokeAll verifica che controlli la dimensione corretta
     */
    @Test
    public void testInvokeAllChecksCorrectSize() throws InterruptedException {
        // Setup con queue quasi piena (8/10)
        queue.setReportedSize(8);

        // Lista con 3 task - dovrebbe fallire (8+3 > 10)
        List<Callable<String>> tasks = Arrays.asList(
                () -> "task1",
                () -> "task2",
                () -> "task3"
        );

        // Execute
        try {
            boundedService.invokeAll(tasks);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit"));
        }

        // Verify che abbia controllato la size
        assertEquals(1, queue.getSizeCalls());

        // Verify che nessun invokeAll sia stato eseguito
        assertEquals(0, executor.getInvokeAllCalls());
    }

    /**
     * LLM-TEST-7: Verifica che tutti i metodi schedule controllino la queue
     */
    @Test
    public void testAllScheduleMethodsCheckQueue() {
        // Setup con queue al limite (5/5)
        queue.setReportedSize(5);
        BoundedScheduledExecutorService service = new BoundedScheduledExecutorService(executor, 5);

        Runnable runnable = mock(Runnable.class);
        Callable<String> callable = mock(Callable.class);

        // Test schedule(Runnable, delay, unit)
        try {
            service.schedule(runnable, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Test schedule(Callable, delay, unit)
        try {
            service.schedule(callable, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Test scheduleAtFixedRate
        try {
            service.scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Test scheduleWithFixedDelay
        try {
            service.scheduleWithFixedDelay(runnable, 0L, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Verify che checkQueue sia stato chiamato 4 volte
        assertEquals(4, queue.getSizeCalls());

        // Verify che nessun task sia stato sottomesso
        assertEquals(0, executor.getScheduleCalls());
        assertEquals(0, executor.getScheduleAtFixedRateCalls());
        assertEquals(0, executor.getScheduleWithFixedDelayCalls());
    }

    /**
     * LLM-TEST-8: Comportamento con maxTasksInQueue negativo (no limit)
     */
    @Test
    public void testNegativeMaxTasksInQueue() {
        // Setup con limite negativo
        BoundedScheduledExecutorService service = new BoundedScheduledExecutorService(executor, -5);
        queue.setReportedSize(100);

        Runnable task = mock(Runnable.class);

        // Execute
        service.execute(task);

        // Verify - non dovrebbe controllare la size (limite negativo ignora controllo)
        assertEquals(0, queue.getSizeCalls());
        assertEquals(1, executor.getExecuteCalls());
    }

    /**
     * Stub executor and adjustable queue implementations
     */
    private static final class StubScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
        private final BlockingQueue<Runnable> exposedQueue;
        private final AtomicInteger executeCalls = new AtomicInteger();
        private final AtomicInteger scheduleCalls = new AtomicInteger();
        private final AtomicInteger scheduleAtFixedRateCalls = new AtomicInteger();
        private final AtomicInteger scheduleWithFixedDelayCalls = new AtomicInteger();
        private final AtomicInteger invokeAllCalls = new AtomicInteger();
        private final AtomicInteger invokeAnyCalls = new AtomicInteger();

        StubScheduledThreadPoolExecutor(BlockingQueue<Runnable> exposedQueue) {
            super(1);
            this.exposedQueue = exposedQueue;
        }

        int getExecuteCalls() { return executeCalls.get(); }
        int getScheduleCalls() { return scheduleCalls.get(); }
        int getScheduleAtFixedRateCalls() { return scheduleAtFixedRateCalls.get(); }
        int getScheduleWithFixedDelayCalls() { return scheduleWithFixedDelayCalls.get(); }
        int getInvokeAllCalls() { return invokeAllCalls.get(); }
        int getInvokeAnyCalls() { return invokeAnyCalls.get(); }

        @Override
        public BlockingQueue<Runnable> getQueue() {
            return exposedQueue;
        }

        @Override
        public void execute(Runnable command) {
            executeCalls.incrementAndGet();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            scheduleCalls.incrementAndGet();
            return new NoOpScheduledFuture<>(null);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            scheduleCalls.incrementAndGet();
            return new NoOpScheduledFuture<>(null);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            scheduleAtFixedRateCalls.incrementAndGet();
            return new NoOpScheduledFuture<>(null);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            scheduleWithFixedDelayCalls.incrementAndGet();
            return new NoOpScheduledFuture<>(null);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
            invokeAllCalls.incrementAndGet();
            return Collections.emptyList();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
            invokeAnyCalls.incrementAndGet();
            throw new UnsupportedOperationException();
        }
    }

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

    private static final class NoOpScheduledFuture<V> implements ScheduledFuture<V> {
        private final V value;
        private final AtomicInteger cancelled = new AtomicInteger();

        NoOpScheduledFuture(V value) {
            this.value = value;
        }

        @Override public long getDelay(TimeUnit unit) { return 0; }
        @Override public int compareTo(Delayed o) { return 0; }
        @Override public boolean cancel(boolean mayInterruptIfRunning) { cancelled.incrementAndGet(); return true; }
        @Override public boolean isCancelled() { return cancelled.get() > 0; }
        @Override public boolean isDone() { return true; }
        @Override public V get() { return value; }
        @Override public V get(long timeout, TimeUnit unit) { return value; }
    }
}