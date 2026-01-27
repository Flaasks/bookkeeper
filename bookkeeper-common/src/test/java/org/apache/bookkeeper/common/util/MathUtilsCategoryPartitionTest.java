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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Category Partition Test Suite per MathUtils
 * 
 * Partizioni boundary identificate:
 * 
 * 1. Partizione signSafeMod - Dividendo ai bordi:
 *    - Boundary: dividendo = -1 (negativo limite)
 *    - Boundary: dividendo = 0 (zero)
 *    - Boundary: dividendo = 1 (positivo limite)
 * 
 * 2. Partizione signSafeMod - Divisore al bordo minimo:
 *    - Boundary: divisore = 1 (minimo valore valido)
 * 
 * 3. Partizione findNextPositivePowerOfTwo - Valori ai bordi delle potenze:
 *    - Boundary: valore = 1 (minimo valido)
 *    - Boundary: valore = 2 (potenza di 2 gia' valida)
 *    - Boundary: valore = 3 (primo valore non-potenza)
 */
public class MathUtilsCategoryPartitionTest {

    /**
     * CP-1: Test signSafeMod con dividendo ai bordi: -1, 0, 1
     * Verifica il comportamento della funzione ai valori limite del dividendo
     */
    @Test
    public void testSignSafeModDividendBoundaries_CP1() {
        int result1 = MathUtils.signSafeMod(-1L, 5);
        assertTrue("Risultato per -1 deve essere non negativo", result1 >= 0);
        assertTrue("Risultato per -1 deve essere < divisore", result1 < 5);
        assertEquals("Atteso 4 per (-1 mod 5)", 4, result1);
        
        // dividendo = 0
        int result2 = MathUtils.signSafeMod(0L, 5);
        assertEquals("0 mod 5 dovrebbe essere 0", 0, result2);
        
        // dividendo = 1 
        int result3 = MathUtils.signSafeMod(1L, 5);
        assertEquals("1 mod 5 dovrebbe essere 1", 1, result3);
    }

    /**
     * CP-2: Test signSafeMod con divisore al bordo minimo
     */
    @Test
    public void testSignSafeModDivisorBoundary_CP2() {
        // divisore = 1 (minimo valore valido)
        assertEquals("Qualsiasi valore mod 1 dovrebbe essere 0", 0, MathUtils.signSafeMod(100L, 1));
        assertEquals("Qualsiasi valore mod 1 dovrebbe essere 0", 0, MathUtils.signSafeMod(-100L, 1));
        assertEquals("Zero mod 1 dovrebbe essere 0", 0, MathUtils.signSafeMod(0L, 1));
    }

    /**
     * CP-3: Test findNextPositivePowerOfTwo ai bordi delle potenze: 1, 2, 3
     * Verifica il comportamento ai valori limite delle potenze di 2
     */
    @Test
    public void testFindNextPowerOfTwoBoundaries_CP3() {
        // valore = 1 (minimo valido)
        assertEquals("1 dovrebbe ritornare 1", 1, MathUtils.findNextPositivePowerOfTwo(1));
        
        // valore = 2 (potenza di 2, dovrebbe ritornare se stesso)
        assertEquals("2 dovrebbe ritornare 2", 2, MathUtils.findNextPositivePowerOfTwo(2));
        
        // valore = 3 (primo non-potenza, dovrebbe ritornare 4)
        assertEquals("3 dovrebbe ritornare 4", 4, MathUtils.findNextPositivePowerOfTwo(3));
    }
}
