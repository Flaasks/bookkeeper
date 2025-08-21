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


import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.junit.After;

/**
 * Test Control-Flow per BoundedScheduledExecutorService
 */
@RunWith(MockitoJUnitRunner.class)
public class BoundedScheduledExecutorServiceControlFlowTest {

    @Mock
    private ScheduledThreadPoolExecutor mockExecutor;

    @Mock
    private BlockingQueue<Runnable> mockQueue;
    private ScheduledExecutorService executor;

    @Before
    public void setUpExecutor() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Before
    public void setUp() {
        when(mockExecutor.getQueue()).thenReturn(mockQueue);
    }

    @After
    public void tearDownExecutor() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    /**
     * CF-TEST-1: Branch maxTasksInQueue = 0 (no limit)
     * VERSIONE CON LENIENT
     */
    @Test
    public void testCheckQueueWithZeroLimit() {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, 0);

        // Stub lenient per documentare che la size non importa
        lenient().when(mockQueue.size()).thenReturn(1000);

        service.execute(mock(Runnable.class));
        service.submit(mock(Callable.class));

        verify(mockQueue, never()).size();
    }

    /**
     * CF-TEST-2: Branch (queue.size() + numberOfTasks) = maxTasksInQueue esatto
     * VERSIONE SEMPLIFICATA - focus solo sul control flow
     */
    @Test
    public void testCheckQueueAtExactBoundary() {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, 10);

        // Setup: queue.size = 9, numberOfTasks = 1, totale = 10 (esatto)
        when(mockQueue.size()).thenReturn(9);

        Runnable task = mock(Runnable.class);

        // Non dovrebbe lanciare eccezione (9 + 1 = 10, non > 10)
        try {
            service.schedule(task, 1L, TimeUnit.SECONDS);
            // OK - non ha lanciato eccezione
        } catch (RejectedExecutionException e) {
            fail("Non dovrebbe lanciare eccezione al boundary esatto");
        }

        // Verify che checkQueue abbia controllato
        verify(mockQueue, times(1)).size();
    }

    /**
     * CF-TEST-3: Branch numberOfTasks > 1 con invokeAll
     */
    @Test
    public void testCheckQueueWithMultipleTasks() {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, 10);

        when(mockQueue.size()).thenReturn(6);

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

        verify(mockQueue, times(1)).size();
    }

    /**
     * CF-TEST-4: Path con queue size che diventa esattamente maxTasksInQueue
     */
    @Test
    public void testCheckQueueWithZeroTasksAtLimit() throws Exception {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, 10);

        when(mockQueue.size()).thenReturn(10);

        List<Callable<String>> emptyTasks = Collections.emptyList();

        try {
            service.invokeAny(emptyTasks);
        } catch (Exception e) {
            // Ignora eccezioni dall'executor
        }

        verify(mockQueue, times(1)).size();
    }

    /**
     * CF-TEST-5: Verifica che ogni metodo chiami checkQueue con numberOfTasks = 1
     * VERSIONE SEMPLIFICATA
     */
    @Test
    public void testAllMethodsCallCheckQueue() {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, 100); // Limite alto

        when(mockQueue.size()).thenReturn(0); // Queue vuota

        // Conta quante volte viene chiamato checkQueue
        int callsBefore = mockingDetails(mockQueue).getInvocations().size();

        // Chiama tutti i metodi singoli (numberOfTasks = 1)
        try {
            service.execute(mock(Runnable.class));
            service.submit(mock(Runnable.class));
            service.submit(mock(Callable.class));
            service.submit(mock(Runnable.class), "result");
            service.schedule(mock(Runnable.class), 1L, TimeUnit.SECONDS);
            service.schedule(mock(Callable.class), 1L, TimeUnit.SECONDS);
            service.scheduleAtFixedRate(mock(Runnable.class), 0L, 1L, TimeUnit.SECONDS);
            service.scheduleWithFixedDelay(mock(Runnable.class), 0L, 1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignora eccezioni dal decorator
        }

        // Verify che checkQueue sia stato chiamato 8 volte (una per metodo)
        verify(mockQueue, times(8)).size();
    }

    /**
     * CF-TEST-6: Branch con overflow di esattamente 1
     */
    @Test
    public void testCheckQueueOverflowByOne() {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, 5);

        when(mockQueue.size()).thenReturn(5);

        Runnable task = mock(Runnable.class);

        try {
            service.execute(task);
            fail("Dovrebbe lanciare eccezione");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit of 5 items"));
        }

        verify(mockQueue, times(1)).size();
        verify(mockExecutor, never()).execute(any());
    }

    /**
     * CF-TEST-7: Test con invokeAny
     */
    @Test
    public void testInvokeAnyNumberOfTasks() throws Exception {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, 5);

        when(mockQueue.size()).thenReturn(2);

        List<Callable<String>> tasks = Arrays.asList(
                () -> "1", () -> "2", () -> "3", () -> "4"
        );

        try {
            service.invokeAny(tasks);
            fail("Dovrebbe fallire check");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit"));
        }

        verify(mockQueue, times(1)).size();
    }

    /**
     * CF-TEST-8: Test ramo maxTasksInQueue negativo
     * VERSIONE CON LENIENT
     */
    @Test
    public void testNegativeLimitBehavesAsNoLimit() {
        BoundedScheduledExecutorService service =
                new BoundedScheduledExecutorService(mockExecutor, -10);

        // Stub lenient per documentare il caso estremo
        lenient().when(mockQueue.size()).thenReturn(Integer.MAX_VALUE);

        service.execute(mock(Runnable.class));

        List<Callable<String>> manyTasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyTasks.add(() -> "task");
        }

        try {
            service.invokeAll(manyTasks, 1L, TimeUnit.SECONDS);
        } catch (Exception e) {
            assertFalse(e instanceof RejectedExecutionException);
        }

        verify(mockQueue, never()).size();
    }

}