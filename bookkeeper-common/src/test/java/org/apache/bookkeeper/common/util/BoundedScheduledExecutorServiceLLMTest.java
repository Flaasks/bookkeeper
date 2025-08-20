package org.apache.bookkeeper.common.util;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test generati tramite LLM per BoundedScheduledExecutorService
 * Focus su: edge cases, bug detection, comportamenti concorrenti
 */
@RunWith(MockitoJUnitRunner.class)
public class BoundedScheduledExecutorServiceLLMTest {

    @Mock
    private ScheduledThreadPoolExecutor mockExecutor;

    @Mock
    private BlockingQueue<Runnable> mockQueue;

    private BoundedScheduledExecutorService boundedService;

    @Before
    public void setUp() {
        when(mockExecutor.getQueue()).thenReturn(mockQueue);
    }

    /**
     * LLM-TEST-1: Bug Detection - scheduleWithFixedDelay chiama metodo sbagliato

    @Test
    public void testScheduleWithFixedDelayCallsWrongMethod() {
        // Setup
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 10);
        when(mockQueue.size()).thenReturn(0);

        Runnable task = mock(Runnable.class);
        ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
        when(mockExecutor.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(mockFuture);

        // Execute
        boundedService.scheduleWithFixedDelay(task, 1L, 2L, TimeUnit.SECONDS);

        // Verify BUG: chiama scheduleAtFixedRate invece di scheduleWithFixedDelay!
        verify(mockExecutor).scheduleAtFixedRate(task, 1L, 2L, TimeUnit.SECONDS);
        verify(mockExecutor, never()).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
    }
     */
    
    /**
     * LLM-TEST-2: Edge case - limite esattamente 1
     */
    @Test
    public void testSingleTaskLimit() {
        // Setup con limite 1
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 1);
        when(mockQueue.size()).thenReturn(0);

        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);

        // Prima esecuzione OK
        boundedService.execute(task1);
        verify(mockExecutor).execute(task1);

        // Seconda esecuzione fallisce
        when(mockQueue.size()).thenReturn(1);

        try {
            boundedService.execute(task2);
            fail("Dovrebbe fallire con queue piena");
        } catch (RejectedExecutionException e) {
            assertTrue(e.getMessage().contains("Queue at limit of 1 items"));
        }
    }

    /**
     * LLM-TEST-3: InvokeAny con collection vuota - VERSIONE LENIENT
     */
    @Test
    public void testInvokeAnyWithEmptyCollection() throws Exception {
        // Setup
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 10);
        List<Callable<String>> emptyTasks = Collections.emptyList();

        // Stub lenient per documentazione (potrebbe non essere usato)
        lenient().when(mockExecutor.invokeAny(emptyTasks))
                .thenThrow(new IllegalArgumentException());

        // Execute
        try {
            boundedService.invokeAny(emptyTasks);
        } catch (Exception e) {
            // OK
        }

        // Verify
        verify(mockQueue, times(1)).size();
    }

    /**
     * LLM-TEST-4: Race condition simulation nel check della queue
     */
    @Test
    public void testQueueSizeChangeDuringCheck() {
        // Setup
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 10);

        // Simula queue che si riempie dopo il check
        AtomicInteger callCount = new AtomicInteger(0);
        when(mockQueue.size()).thenAnswer(invocation -> {
            int count = callCount.incrementAndGet();
            return count == 1 ? 9 : 10; // Prima 9, poi 10
        });

        // Mock executor che fallisce se queue piena
        doThrow(new RejectedExecutionException("Queue full"))
                .when(mockExecutor).execute(any());

        Runnable task = mock(Runnable.class);

        // Execute
        try {
            boundedService.execute(task);
            fail("Dovrebbe propagare RejectedExecutionException dall'executor");
        } catch (RejectedExecutionException e) {
            // L'eccezione viene dall'executor, non dal nostro check
            assertEquals("Queue full", e.getMessage());
        }
    }

    /**
     * LLM-TEST-5: Submit con result null - CORRETTO
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSubmitRunnableWithNullResult() {
        // Setup
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 10);
        when(mockQueue.size()).thenReturn(5);

        Runnable task = mock(Runnable.class);

        // Execute
        boundedService.submit(task, null);

        // Verify solo che checkQueue sia stato chiamato
        verify(mockQueue).size();

        // Non possiamo verificare submit direttamente perché viene chiamato sul decorator
        // ma possiamo verificare che execute sia stato chiamato (dal decorator)
        verify(mockExecutor).execute(any(Runnable.class));
    }

    /**
     * LLM-TEST-6: InvokeAll verifica che controlli la dimensione corretta - CORRETTO
     */
    @Test
    public void testInvokeAllChecksCorrectSize() throws InterruptedException {
        // Setup con queue quasi piena (8/10)
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 10);
        when(mockQueue.size()).thenReturn(8);

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
        verify(mockQueue, times(1)).size();

        // CORREZIONE: Non usare verifyNoMoreInteractions perché il costruttore chiama getQueue()
        // Verifica invece che non ci siano state altre chiamate oltre a getQueue()
        verify(mockExecutor, times(1)).getQueue(); // dal costruttore
        verifyNoMoreInteractions(mockExecutor);
    }

    /**
     * LLM-TEST-7: Verifica che tutti i metodi schedule controllino la queue
     */
    @Test
    public void testAllScheduleMethodsCheckQueue() {
        // Setup con queue al limite
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 5);
        when(mockQueue.size()).thenReturn(5); // Al limite

        Runnable runnable = mock(Runnable.class);
        Callable<String> callable = mock(Callable.class);

        // Test schedule(Runnable, delay, unit)
        try {
            boundedService.schedule(runnable, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Test schedule(Callable, delay, unit)
        try {
            boundedService.schedule(callable, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Test scheduleAtFixedRate
        try {
            boundedService.scheduleAtFixedRate(runnable, 0L, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Test scheduleWithFixedDelay
        try {
            boundedService.scheduleWithFixedDelay(runnable, 0L, 1L, TimeUnit.SECONDS);
            fail("Dovrebbe lanciare RejectedExecutionException");
        } catch (RejectedExecutionException e) {
            // Atteso
        }

        // Verify che checkQueue sia stato chiamato 4 volte
        verify(mockQueue, times(4)).size();

        // Verify che nessun task sia stato sottomesso
        verify(mockExecutor, never()).schedule(any(Runnable.class), anyLong(), any());
        verify(mockExecutor, never()).schedule(any(Callable.class), anyLong(), any());
        verify(mockExecutor, never()).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
    }

    /**
     * LLM-TEST-8: Comportamento con maxTasksInQueue negativo - VERSIONE LENIENT
     */
    @Test
    public void testNegativeMaxTasksInQueue() {
        // Setup con limite negativo
        boundedService = new BoundedScheduledExecutorService(mockExecutor, -5);

        // Stub lenient per documentazione (non verrà usato)
        lenient().when(mockQueue.size()).thenReturn(100);

        Runnable task = mock(Runnable.class);

        // Execute
        boundedService.execute(task);

        // Verify
        verify(mockQueue, never()).size();
        verify(mockExecutor).execute(any(Runnable.class));
    }
}