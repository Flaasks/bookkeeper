/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.bookkeeper.common.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Control flow suite test per MathUtils
 */
public class MathUtilsControlFlowTest {

    /**
     * CF-1: Control flow test per nowInNano() 
     * 
     * Scopo: Invocare nowInNano() per aggiungerlo alla copertura dei metodi
     * 
     * Test strategy:
     * - Chiamare nowInNano() piu volte per verificare la monotonicità
     * - Verificare che i valori restituiti siano positivi (per System.nanoTime())
     * - Assicurare la progressione temporale tra le chiamate
     */
    @Test
    public void testNowInNanoControlFlow_1() {
        // prendo primo nanotime checkpoint
        long t1 = MathUtils.nowInNano();
        assertTrue("nowInNano() deve ritornare un valore positivo", t1 > 0);
        
        // prendo secondo nanotime checkpoint
        long t2 = MathUtils.nowInNano();
        assertTrue("Second nowInNano() deve essere >= first call", t2 >= t1);
        assertNotEquals("Chiamate multiple devono mostrare progressione temporale", t1, t2);
        
        // Verifico valori positivi e tempo trascorso non negativo
        assertTrue("t1 deve essere positivo", t1 > 0);
        assertTrue("t2 deve essere positivo", t2 > 0);
        assertTrue("Il tempo trascorso deve essere non negativo", (t2 - t1) >= 0);
    }

    /**
     * CF-2: Control flow test per elapsedMicroSec()
     * 
     * Test strategy:
     * - Usare nowInNano() per catturare il tempo di inizio in nanosecondi
     * - Attendere un intervallo misurabile tramite Thread.sleep(10 ms)
     * - Verificare che elapsedMicroSec() restituisca la conversione corretta (1000 nanos = 1 micro)
     * - Validare che il percorso TimeUnit.NANOSECONDS.toMicros() sia esercitato
     */
    @Test
    public void testElapsedMicroSecControlFlow_2() throws InterruptedException {
        // Catturare il tempo di inizio in nanosecondi
        long startNano = MathUtils.nowInNano();
        
        // Attendere 10 millisecondi per creare un intervallo di tempo misurabile
        Thread.sleep(10);
        
        // Calcolare il tempo trascorso in microsecondi
        long elapsedMicros = MathUtils.elapsedMicroSec(startNano);
        
        // Verificare che il risultato della conversione sia positivo ( TimeUnit.NANOSECONDS.toMicros())
        assertTrue("I microsecondi trascorsi devono essere positivi", elapsedMicros > 0);
        
        // Verificare che la conversione sia nell'intervallo previsto di circa 10ms
        // Tolleranza: [8000, 12000] micros (considerando la variazione di scheduling del sistema)
        assertTrue("I microsecondi trascorsi dovrebbero essere approssimativamente 10000 (10ms)",
                   elapsedMicros >= 8000 && elapsedMicros <= 12000);
        
        // Verificare che il risultato in microsecondi sia inferiore a quello in nanosecondi di circa 1000 volte
        // Questo valida che la conversione TimeUnit funzioni correttamente
        long elapsedNanos = MathUtils.elapsedNanos(startNano);
        assertTrue("I nanosecondi dovrebbero essere circa 1000 volte maggiori dei microsecondi",
                   elapsedNanos > elapsedMicros * 500);
    }

}
