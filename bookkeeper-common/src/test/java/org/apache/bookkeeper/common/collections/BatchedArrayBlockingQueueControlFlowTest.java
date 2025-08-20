package org.apache.bookkeeper.common.collections;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test guidati da COVERAGE CONTROL-FLOW per BatchedArrayBlockingQueue
 * Obiettivo: coprire tutti i branch, loop e percorsi di esecuzione

 * Analisi dei branch principali da coprire:
 * - Wrap-around di consumerIdx/producerIdx
 * - Condizioni wait/notify (queue piena/vuota)
 * - Branch in putAll/pollAll per gestione array
 * - Timeout con valore 0, positivo, e scaduto
 */
public class BatchedArrayBlockingQueueControlFlowTest {

    /**
     * CF-TEST-1: Copre branch wrap-around di consumerIdx
     * Target: if (++consumerIdx == capacity) { consumerIdx = 0; }
     */
    @Test
    public void testConsumerIndexWrapAround() {
        BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(3);

        // Setup: riempi la queue
        queue.offer("A");
        queue.offer("B");
        queue.offer("C");

        // Consuma fino al wrap
        assertEquals("A", queue.poll()); // consumerIdx: 0->1
        assertEquals("B", queue.poll()); // consumerIdx: 1->2
        assertEquals("C", queue.poll()); // consumerIdx: 2->0 (WRAP!)

        // Verifica che dopo il wrap funzioni ancora
        queue.offer("D");
        assertEquals("D", queue.poll()); // consumerIdx: 0->1
    }

    /**
     * CF-TEST-2: Copre branch putAll con wrap-around del buffer
     * Target: copertura del secondo span in internalPutAll
     */
    @Test
    public void testPutAllWithWrapAround() throws InterruptedException {
        BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(5);

        // Setup: posiziona producerIdx vicino alla fine
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        queue.offer(4); // producerIdx = 4

        // Consuma alcuni per fare spazio
        queue.poll();
        queue.poll(); // 2 spazi liberi

        // putAll che deve fare wrap-around
        Integer[] items = {5, 6};
        queue.putAll(items, 0, 2);

        // Verifica ordine corretto
        assertEquals(Integer.valueOf(3), queue.poll());
        assertEquals(Integer.valueOf(4), queue.poll());
        assertEquals(Integer.valueOf(5), queue.poll());
        assertEquals(Integer.valueOf(6), queue.poll());
    }

    /**
     * CF-TEST-3: Copre branch timeout = 0 in offer
     * Target: if (remainingTimeNanos <= 0L) return false;
     */
    @Test
    public void testOfferWithZeroTimeout() throws InterruptedException {
        BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(1);
        queue.offer("FULL");

        // Offer con timeout 0 su queue piena
        long start = System.currentTimeMillis();
        boolean result = queue.offer("FAIL", 0, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;

        assertFalse("Dovrebbe fallire immediatamente", result);
        assertTrue("Non dovrebbe aspettare", elapsed < 50);
    }

    /**
     * CF-TEST-4: Copre branch signal in enqueueOne
     * Target: if (size++ == 0) { notEmpty.signalAll(); }
     */
    @Test
    public void testSignalOnFirstElement() throws InterruptedException {
        final BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(5);
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final CountDownLatch elementReceived = new CountDownLatch(1);

        // Thread consumer che aspetta
        Thread consumer = new Thread(() -> {
            try {
                threadStarted.countDown();
                String item = queue.take(); // Bloccherà su queue vuota
                assertNotNull(item);
                elementReceived.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        // Assicura che il consumer sia in attesa
        threadStarted.await();
        Thread.sleep(50);

        // Aggiungi elemento - dovrebbe svegliare il consumer
        queue.offer("WAKE_UP"); // Triggera notEmpty.signalAll()

        assertTrue("Consumer dovrebbe ricevere elemento",
                elementReceived.await(1, TimeUnit.SECONDS));
        consumer.join();
    }

    /**
     * CF-TEST-5: Copre branch pollAll con wrap-around
     * Target: secondSpan > 0 in internalTakeAll
     */
    @Test
    public void testPollAllWithWrapAround() throws InterruptedException {
        BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(5);

        // Setup circolare: aggiungi e rimuovi per posizionare consumerIdx
        for (int i = 0; i < 5; i++) {
            queue.offer(i);
        }
        queue.poll();
        queue.poll(); // consumerIdx = 2

        queue.offer(5);
        queue.offer(6); // Elementi alle posizioni 0,1

        // pollAll che deve fare wrap
        Integer[] buffer = new Integer[5];
        int taken = queue.pollAll(buffer, 0, TimeUnit.SECONDS);

        assertEquals(5, taken);
        // Verifica ordine: dovrebbe essere 2,3,4 (dal vecchio) poi 5,6 (wrap)
        assertArrayEquals(new Integer[]{2, 3, 4, 5, 6}, buffer);
    }

    /**
     * CF-TEST-6: Copre branch clear con elementi da rimuovere
     * Target: while (size > 0) { dequeueOne(); }
     */
    @Test
    public void testClearNonEmptyQueue() {
        BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(10);

        // Aggiungi vari elementi
        for (int i = 0; i < 7; i++) {
            queue.offer("Item" + i);
        }

        assertEquals(7, queue.size());

        // Clear dovrebbe iterare e rimuovere tutti
        queue.clear();

        assertEquals(0, 0);
        assertTrue(queue.isEmpty());
        assertNull(queue.poll());
    }

    /**
     * CF-TEST-7: Copre branch putAll quando deve aspettare spazio
     * Target: while (size == capacity) { notFull.await(); }
     */
    @Test
    public void testPutAllBlockingBehavior() throws InterruptedException {
        final BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(2);
        queue.offer(1);
        queue.offer(2); // Queue piena

        final List<Integer> toPut = Arrays.asList(3, 4, 5);
        final AtomicInteger inserted = new AtomicInteger(0);
        final CountDownLatch done = new CountDownLatch(1);

        // Thread che prova putAll su queue piena
        Thread producer = new Thread(() -> {
            try {
                int count = queue.putAll(toPut); // Dovrebbe bloccare
                inserted.set(count);
                done.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();

        // Verifica che sia bloccato
        Thread.sleep(50);
        assertEquals(0, inserted.get());

        // Libera spazio
        queue.poll(); // Ora c'è 1 spazio

        // putAll dovrebbe procedere parzialmente
        assertTrue(done.await(1, TimeUnit.SECONDS));
        assertEquals(1, inserted.get()); // Solo 1 elemento inserito
        producer.join();
    }

    /**
     * CF-TEST-8: Copre remainingCapacity con queue in vari stati
     * Target: return capacity - size;
     */
    @Test
    public void testRemainingCapacityAllStates() {
        BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(4);

        // Stato: vuota
        assertEquals(4, queue.remainingCapacity());

        // Stato: parzialmente piena
        queue.offer(1);
        assertEquals(3, queue.remainingCapacity());

        queue.offer(2);
        assertEquals(2, queue.remainingCapacity());

        // Stato: piena
        queue.offer(3);
        queue.offer(4);
        assertEquals(0, queue.remainingCapacity());

        // Dopo poll
        queue.poll();
        assertEquals(1, queue.remainingCapacity());

        // Dopo clear
        queue.clear();
        assertEquals(4, queue.remainingCapacity());
    }
}