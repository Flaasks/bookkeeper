package org.apache.bookkeeper.common.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Test di alta qualità per BoundedScheduledExecutorService.
 * I test sono stati migliorati per coprire i casi limite e prevenire blocchi,
 * mirando a un'analisi "pulita" su strumenti come SonarCloud.
 */
@RunWith(MockitoJUnitRunner.class)
public class BoundedScheduledExecutorServiceMockTest {

    @Mock
    private ScheduledThreadPoolExecutor mockExecutor;

    @Mock
    private BlockingQueue<Runnable> mockQueue;

    private BoundedScheduledExecutorService boundedService;

    @Before
    public void setUp() {
        when(mockExecutor.getQueue()).thenReturn(mockQueue);
        boundedService = new BoundedScheduledExecutorService(mockExecutor, 10);
    }

    /**
     * TEST 1: Categoria QUEUE_SIZE (sotto limite) + TASK_TYPE (Runnable)
     */
    @Test
    public void testExecuteRunnableWithQueueBelowLimit() {
        when(mockQueue.size()).thenReturn(5);
        Runnable task = mock(Runnable.class);
        boundedService.execute(task);
        verify(mockQueue, times(1)).size();
        verify(mockExecutor, times(1)).execute(task);
    }

    /**
     * TEST 2: Categoria QUEUE_SIZE (al limite) + TASK_TYPE (Callable)
     */
    @Test
    public void testSubmitCallableAtExactLimit() {
        // Setup: la coda ha 9 elementi. L'aggiunta di un task la porta a 10 (il limite).
        when(mockQueue.size()).thenReturn(9);

        Callable<String> task = mock(Callable.class);
        doNothing().when(mockExecutor).execute(any(Runnable.class));

        // Esegue e verifica che non ci siano eccezioni
        try {
            boundedService.submit(task);
        } catch (RejectedExecutionException e) {
            fail("Il task non dovrebbe essere rifiutato al limite esatto.");
        }

        verify(mockQueue, times(1)).size();
        verify(mockExecutor, times(1)).execute(any(Runnable.class));
    }

    /**
     * TEST 3: Categoria QUEUE_SIZE (sopra limite) + TASK_TYPE (Runnable)
     */
    @Test
    public void testExecuteRunnableOverLimit() {
        when(mockQueue.size()).thenReturn(10);
        Runnable task = mock(Runnable.class);
        try {
            boundedService.execute(task);
            fail("Dovrebbe lanciare una RejectedExecutionException.");
        } catch (RejectedExecutionException e) {
            assertEquals("Queue at limit of 10 items", e.getMessage());
        }
        verify(mockExecutor, never()).execute(any(Runnable.class));
    }

    /**
     * TEST 4: Categoria LIMIT_CONFIG (senza limite)
     */
    @Test
    public void testNoLimitAllowsLargeQueue() {
        BoundedScheduledExecutorService noLimitService = new BoundedScheduledExecutorService(mockExecutor, 0);
        Runnable task = mock(Runnable.class);
        noLimitService.execute(task);
        verify(mockQueue, never()).size();
        verify(mockExecutor, times(1)).execute(task);
    }

    /**
     * TEST 5: Categoria BATCH_SIZE (multipli task) - solo caso di fallimento.
     */
    @Test
    public void testInvokeAllFailsWithFullQueue() throws InterruptedException {
        when(mockQueue.size()).thenReturn(8);
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
        verify(mockExecutor, never()).invokeAll(anyCollection());
    }

    /**
     * TEST 6: Test aggiuntivo per il caso invokeAny con collezione non vuota,
     * verificando solo il caso di fallimento per evitare il loop.
     */
    @Test
    public void testInvokeAnyFailsWithFullQueue() throws Exception {
        when(mockQueue.size()).thenReturn(8);
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
        verify(mockExecutor, never()).invokeAny(anyCollection());
    }

    /**
     * TEST 7: Test invokeAny con collezione vuota per verificare il comportamento
     * della classe quando il numero di task è zero.
     */
    @Test
    public void testInvokeAnyWithEmptyCollection() throws Exception {
        when(mockQueue.size()).thenReturn(0);
        Collection<Callable<String>> emptyTasks = Collections.emptyList();

        try {
            boundedService.invokeAny(emptyTasks);
            fail("Dovrebbe lanciare un'eccezione con una collezione vuota.");
        } catch (IllegalArgumentException e) {
            // L'eccezione è attesa.
        }

        verify(mockQueue, times(1)).size();
        verify(mockExecutor, never()).invokeAny(anyCollection());
    }

    /**
     * TEST 8: Test aggiuntivo che verifica il comportamento di invokeAny
     * con una collezione vuota quando maxTasksInQueue è 0.
     */
    @Test
    public void testInvokeAnyWithEmptyCollectionAndZeroLimit() throws Exception {
        BoundedScheduledExecutorService noLimitService = new BoundedScheduledExecutorService(mockExecutor, 0);
        Collection<Callable<String>> emptyTasks = Collections.emptyList();

        try {
            noLimitService.invokeAny(emptyTasks);
            fail("Dovrebbe lanciare un'eccezione con una collezione vuota.");
        } catch (IllegalArgumentException e) {
            // L'eccezione è attesa.
        }

        verify(mockQueue, never()).size();
        verify(mockExecutor, never()).invokeAny(anyCollection());
    }
}