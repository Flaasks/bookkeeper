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

package org.apache.bookkeeper.common.collections;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/**
 * Test generati tramite LLM per BatchedArrayBlockingQueue
 */
public class BatchedArrayBlockingQueueLLMGeneratedTest {

    /**
     * TEST LLM-1: Categoria CAPACITY (zero) - La classe ACCETTA capacità 0!
     */
    @Test(timeout = 5000) // 5 secondi timeout
    public void testCapacityZeroIsAccepted() {
        BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(0);
        assertNotNull("Queue con capacità 0 viene creata", queue);
        assertEquals("Capacità 0 significa queue sempre piena", 0, queue.remainingCapacity());

        assertFalse("Non può accettare elementi con capacità 0", queue.offer("A"));
        assertNull("Poll su queue con capacità 0 ritorna null", queue.poll());
        assertEquals("Size rimane 0", 0, queue.size());
    }

    /**
     * TEST LLM-1b: Categoria CAPACITY (negativa)
     */
    @Test(expected = NegativeArraySizeException.class, timeout = 5000)
    public void testInvalidCapacityNegative() {
        new BatchedArrayBlockingQueue<>(-5);
    }

    /**
     * TEST LLM-2: Categoria DATA (array vuoti e null) + OPERATION_TYPE (batch)
     */
    @Test(timeout = 5000)
    public void testBatchOperationsWithEmptyAndNullArrays() throws InterruptedException {
        BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(10);
        queue.offer("A");
        queue.offer("B");

        List<String> emptyList = new ArrayList<>();
        int inserted = queue.putAll(emptyList);
        assertEquals("PutAll con lista vuota dovrebbe inserire 0 elementi", 0, inserted);
        assertEquals("Size non dovrebbe cambiare", 2, queue.size());

        String[] emptyBuffer = new String[0];
        int polled = queue.pollAll(emptyBuffer, 0, TimeUnit.SECONDS);
        assertEquals("PollAll con buffer vuoto dovrebbe ritornare 0", 0, polled);
        assertEquals("Size non dovrebbe cambiare", 2, queue.size());
    }

    /**
     * TEST LLM-3: Categoria CONCURRENCY -
     */
    @Test(timeout = 10000) // 10 secondi timeout
    public void testConcurrentProducers() throws InterruptedException {
        final BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(50);
        final int numProducers = 3; 
        final int itemsPerProducer = 10; 
        final CountDownLatch endLatch = new CountDownLatch(numProducers);

        for (int i = 0; i < numProducers; i++) {
            final int producerId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < itemsPerProducer; j++) {
                        queue.offer(producerId * 100 + j, 100, TimeUnit.MILLISECONDS);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        assertTrue("Tutti i producer dovrebbero completare",
                endLatch.await(5, TimeUnit.SECONDS));

        // Verifica solo il size finale
        assertFalse("Queue dovrebbe avere elementi", queue.isEmpty());
        assertTrue("Queue non dovrebbe avere più elementi del previsto",
                queue.size() <= numProducers * itemsPerProducer);
    }

    /**
     * TEST LLM-4: Categoria BOUNDARY - Test wrap-around
     */
    @Test(timeout = 5000)
    public void testCircularBufferWrapAround() {
        BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(5);

        // Riempi completamente
        for (int i = 0; i < 5; i++) {
            assertTrue(queue.offer(i));
        }

        // Consuma 3 elementi
        for (int i = 0; i < 3; i++) {
            assertEquals(Integer.valueOf(i), queue.poll());
        }

        // Aggiungi 3 nuovi elementi
        for (int i = 5; i < 8; i++) {
            assertTrue("Dovrebbe poter aggiungere dopo wrap", queue.offer(i));
        }

        // Verifica ordine corretto
        assertEquals(Integer.valueOf(3), queue.poll());
        assertEquals(Integer.valueOf(4), queue.poll());
        assertEquals(Integer.valueOf(5), queue.poll());
    }

    /**
     * TEST LLM-5: Test drainTo
     */
    @Test(timeout = 5000)
    public void testDrainToCollections() {
        BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(10);
        for (int i = 0; i < 5; i++) {
            queue.offer("Item" + i);
        }

        List<String> list = new ArrayList<>();
        int drained = queue.drainTo(list, 3);
        assertEquals("Dovrebbe drenare 3 elementi", 3, drained);
        assertEquals("Lista dovrebbe avere 3 elementi", 3, list.size());
        assertEquals("Queue dovrebbe avere 2 elementi rimanenti", 2, queue.size());
    }

    /**
     * TEST LLM-6: Test putAll con timeout 
     */
    @Test(timeout = 5000)
    public void testPutAllWithTimeout() throws InterruptedException {
        BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(3);

        // Riempi parzialmente
        queue.offer(1);

        // putAll che entra completamente
        List<Integer> items = Arrays.asList(2, 3);
        int inserted = queue.putAll(items);
        assertEquals("Dovrebbe inserire 2 elementi", 2, inserted);
        assertEquals("Queue dovrebbe essere piena", 3, queue.size());
    }

    /**
     * TEST LLM-7: Test remainingCapacity in vari stati
     */
    @Test(timeout = 5000)
    public void testRemainingCapacity() {
        BatchedArrayBlockingQueue<Integer> queue = new BatchedArrayBlockingQueue<>(5);

        // Queue vuota
        assertEquals("Capacità rimanente con queue vuota", 5, queue.remainingCapacity());

        // Aggiungi elementi
        queue.offer(1);
        queue.offer(2);
        assertEquals("Capacità rimanente con 2 elementi", 3, queue.remainingCapacity());

        // Riempi completamente
        queue.offer(3);
        queue.offer(4);
        queue.offer(5);
        assertEquals("Capacità rimanente con queue piena", 0, queue.remainingCapacity());

        // Dopo poll
        queue.poll();
        assertEquals("Capacità rimanente dopo poll", 1, queue.remainingCapacity());
    }

    /**
     * TEST LLM-8: Test iterator non supportato e clear
     */
    @Test(timeout = 5000)
    public void testUnsupportedAndClearOperations() {
        BatchedArrayBlockingQueue<String> queue = new BatchedArrayBlockingQueue<>(5);
        queue.offer("A");
        queue.offer("B");
        queue.offer("C");

        // Test iterator non supportato
        try {
            queue.iterator();
            fail("Iterator dovrebbe lanciare UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Atteso
        }

        // Test clear
        queue.clear();
        assertEquals("Queue dovrebbe essere vuota dopo clear", 0, queue.size());
        assertTrue("isEmpty dovrebbe ritornare true", queue.isEmpty());
    }

}