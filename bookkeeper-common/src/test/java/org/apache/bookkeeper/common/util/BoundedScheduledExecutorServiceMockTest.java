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
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test di alta qualità per BoundedScheduledExecutorService con Mockito per verificare le
 * interazioni sugli executor, usando una coda regolabile per controllare il flow.
 */
public class BoundedScheduledExecutorServiceMockTest {
    static {
        // Enable ByteBuddy experimental mode to allow Mockito inline on Java 21.
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

    /**
     * Executor stub that counts invocations; scheduling is a no-op.
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

        int getExecuteCalls() {
            return executeCalls.get();
        }

        int getScheduleCalls() {
            return scheduleCalls.get();
        }

        int getScheduleAtFixedRateCalls() {
            return scheduleAtFixedRateCalls.get();
        }

        int getScheduleWithFixedDelayCalls() {
            return scheduleWithFixedDelayCalls.get();
        }

        int getInvokeAllCalls() {
            return invokeAllCalls.get();
        }

        int getInvokeAnyCalls() {
            return invokeAnyCalls.get();
        }

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
            throw new UnsupportedOperationException("invokeAny not executed in stub");
        }
    }

    /** Simple ScheduledFuture stub used by the executor stub. */
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

    /**
     * Minimal adjustable queue with controllable size and call counter.
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

        // Remaining BlockingQueue methods are unused in these tests.
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
     * TEST 1: Categoria QUEUE_SIZE (sotto limite) + TASK_TYPE (Runnable)
     */
    @Test
    public void testExecuteRunnableWithQueueBelowLimit() {
        queue.setReportedSize(5);
        Runnable task = mock(Runnable.class);
        boundedService.execute(task);
        assertEquals(1, queue.getSizeCalls());
        assertEquals(1, executor.getExecuteCalls());
    }

    /**
     * TEST 2: Categoria QUEUE_SIZE (al limite) + TASK_TYPE (Callable)
     */
    @Test
    public void testSubmitCallableAtExactLimit() {
        // Setup: la coda ha 9 elementi. L'aggiunta di un task la porta a 10 (il limite).
        queue.setReportedSize(9);

        Callable<String> task = mock(Callable.class);

        // Esegue e verifica che non ci siano eccezioni
        try {
            boundedService.submit(task);
        } catch (RejectedExecutionException e) {
            fail("Il task non dovrebbe essere rifiutato al limite esatto.");
        }

        assertEquals(1, queue.getSizeCalls());
        assertTrue(executor.getExecuteCalls() >= 1 || executor.getScheduleCalls() >= 1);
    }

    /**
     * TEST 3: Categoria QUEUE_SIZE (sopra limite) + TASK_TYPE (Runnable)
     */
    @Test
    public void testExecuteRunnableOverLimit() {
        queue.setReportedSize(10);
        Runnable task = mock(Runnable.class);
        try {
            boundedService.execute(task);
            fail("Dovrebbe lanciare una RejectedExecutionException.");
        } catch (RejectedExecutionException e) {
            assertEquals("Queue at limit of 10 items", e.getMessage());
        }
        assertEquals(0, executor.getExecuteCalls());
    }

    /**
     * TEST 4: Categoria LIMIT_CONFIG (senza limite)
     */
    @Test
    public void testNoLimitAllowsLargeQueue() {
        BoundedScheduledExecutorService noLimitService = new BoundedScheduledExecutorService(executor, 0);
        Runnable task = mock(Runnable.class);
        noLimitService.execute(task);
        assertEquals(0, queue.getSizeCalls());
        assertEquals(1, executor.getExecuteCalls());
    }

    /**
     * TEST 5: Categoria BATCH_SIZE (multipli task) - solo caso di fallimento.
     */
    @Test
    public void testInvokeAllFailsWithFullQueue() throws InterruptedException {
        queue.setReportedSize(8);
        List<Callable<Integer>> tasks = Arrays.asList(
            mock(Callable.class),
            mock(Callable.class),
            mock(Callable.class)
        );
        try {
            boundedService.invokeAll(tasks);
            fail("Dovrebbe lanciare una RejectedExecutionException.");
        } catch (RejectedExecutionException e) {
            assertEquals("Queue at limit of 10 items", e.getMessage());
        }
        assertEquals(0, executor.getInvokeAllCalls());
    }

    /**
     * TEST 6: Test aggiuntivo per il caso invokeAny con collezione non vuota,
     * verificando solo il caso di fallimento per evitare il loop.
     */
    @Test
    public void testInvokeAnyFailsWithFullQueue() throws Exception {
        queue.setReportedSize(8);
        List<Callable<Integer>> tasks = Arrays.asList(
            mock(Callable.class),
            mock(Callable.class),
            mock(Callable.class)
        );
        try {
            boundedService.invokeAny(tasks);
            fail("Dovrebbe lanciare una RejectedExecutionException.");
        } catch (RejectedExecutionException e) {
            assertEquals("Queue at limit of 10 items", e.getMessage());
        }
        assertEquals(0, executor.getInvokeAnyCalls());
    }

    /**
     * TEST 7: Test invokeAny con collezione vuota per verificare il comportamento
     * della classe quando il numero di task è zero.
     */
    @Test
    public void testInvokeAnyWithEmptyCollection() throws Exception {
        queue.setReportedSize(0);
        Collection<Callable<String>> emptyTasks = Collections.emptyList();

        try {
            boundedService.invokeAny(emptyTasks);
            fail("Dovrebbe lanciare un'eccezione con una collezione vuota.");
        } catch (IllegalArgumentException e) {
            // L'eccezione è attesa.
        }

        assertEquals(1, queue.getSizeCalls());
        assertEquals(0, executor.getInvokeAnyCalls());
    }

    /**
     * TEST 8: Test aggiuntivo che verifica il comportamento di invokeAny
     * con una collezione vuota quando maxTasksInQueue è 0.
     */
    @Test
    public void testInvokeAnyWithEmptyCollectionAndZeroLimit() throws Exception {
        BoundedScheduledExecutorService noLimitService = new BoundedScheduledExecutorService(executor, 0);
        Collection<Callable<String>> emptyTasks = Collections.emptyList();

        try {
            noLimitService.invokeAny(emptyTasks);
            fail("Dovrebbe lanciare un'eccezione con una collezione vuota.");
        } catch (IllegalArgumentException e) {
            // L'eccezione è attesa.
        }

        assertEquals(0, queue.getSizeCalls());
        assertEquals(0, executor.getInvokeAnyCalls());
    }
}