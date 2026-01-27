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

package org.apache.bookkeeper.common.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Category Partition Test Suite per RecyclableArrayList
 */
public class RecyclableArrayListCategoryPartitionTest {

    private RecyclableArrayList<String> list;

    @Before
    public void setUp() {
        list = new RecyclableArrayList<>();
    }

    /**
     * Test T1: Add Single Non-Null Element to Empty List
     * 
     * Category: Stato=Empty, Elem=NonNull
     * Boundary: Transizione 0 -> 1 elemento
     * 
     * Input:
     *   - Azione: Creare RecyclableArrayList vuota, chiamare add("elemento1")
     *   - Stato iniziale: lista vuota (size = 0)
     *   - Elemento: "elemento1" (String, non-null)
     * 
     * Expected Output:
     *   - Ritorno: true
     *   - Stato: size = 1
     *   - Contenuto: list.get(0) == "elemento1"
     *   - Invariante: lista mantiene ordinamento e contenuto
     */
    @Test
    public void testT1_AddSingleNonNullElementToEmptyList() {

        assertEquals("Lista deve essere inizialmente vuota", 0, list.size());
        
        boolean result = list.add("elemento1");

        assertTrue("add() deve ritornare true", result);
        assertEquals("Size deve essere 1 dopo add", 1, list.size());
        assertEquals("Elemento deve essere accessibile", "elemento1", list.get(0));
    }

    /**
     * Test T2: Add Null Element to Single-Element List
     * 
     * Category: Stato=Single, Elem=Null
     * Boundary: Tipo di elemento (null vs non-null)
     * 
     * Input:
     *   - Azione: Aggiungere null a lista con 1 elemento
     *   - Stato iniziale: lista con "elemento1"
     *   - Elemento: null
     * 
     * Expected Output:
     *   - Ritorno: true (null è valido in ArrayList)
     *   - Stato: size = 2
     *   - Contenuto: list.get(1) == null
     *   - Invariante: lista permette elementi null
     */
    @Test
    public void testT2_AddNullElementToSingleElementList() {

        list.add("elemento1");
        assertEquals("lista deve avere size 1", 1, list.size());

        boolean result = list.add(null);

        assertTrue("add(null) deve ritornare true", result);
        assertEquals("Size deve essere 2 dopo add null", 2, list.size());
        assertNull("Elemento a indice 1 deve essere null", list.get(1));
    }

    /**
     * Test T3: Get First Element from Single-Element List
     * 
     * Category: Indice=0, Stato=Single, Boundary minimo
     * Boundary: Indice 0 
     * 
     * Input:
     *   - Azione: Chiamare get(0) su lista con 1 elemento
     *   - Stato iniziale: lista con "elemento"
     *   - Indice: 0 (boundary inferiore)
     * 
     * Expected Output:
     *   - Ritorno: "elemento"
     *   - Eccezione: nessuna
     *   - Invariante: lista non modificata
     */
    @Test
    public void testT3_GetFirstElementFromSingleElementList() {
        
        list.add("elemento");
        assertEquals("lista deve avere size 1", 1, list.size());

        String element = list.get(0);

        assertEquals("get(0) deve ritornare l'elemento", "elemento", element);
        assertEquals("Size deve rimanere 1 dopo get", 1, list.size());
    }

    /**
     * Test T4: Remove Last Only Element from Single-Element List
     * 
     * Category: Indice=Last, Stato=Single
     * Boundary: Transizione: Single -> Empty
     * 
     * Input:
     *   - Azione: Chiamare remove(0) su lista con 1 elemento
     *   - Stato iniziale: lista con "elemento"
     *   - Indice: 0 (ultimo elemento)
     * 
     * Expected Output:
     *   - Ritorno: "elemento"
     *   - Stato: size = 0 (ritorno ad empty)
     *   - Invariante: lista torna a empty
     */
    @Test
    public void testT4_RemoveLastOnlyElementFromSingleElementList() {

        list.add("elemento");
        assertEquals("lista deve avere size 1", 1, list.size());

        String removed = list.remove(0);

        assertEquals("remove(0) deve ritornare l'elemento", "elemento", removed);
        assertEquals("Size deve essere 0 dopo rimozione", 0, list.size());
    }

    /**
     * Test T5: Equals with Null Parameter
     * 
     * Category: Parametro=Null
     * Boundary: Confronto con null 
     * 
     * Input:
     *   - Azione: Chiamare equals(null) su lista non-vuota
     *   - Lista 1: lista con "elemento"
     *   - Parametro: null
     * 
     * Expected Output:
     *   - Ritorno: false
     *   - Eccezione: nessuna (NOT NullPointerException)
     *   - Invariante: lista non modificata
     */
    @Test
    public void testT5_EqualsWithNullParameter() {
        list.add("elemento");
        assertEquals("lista deve avere size 1", 1, list.size());

        assertFalse("equals(null) deve ritornare false, non lanciare eccezione", list.equals(null));
        assertEquals("Size deve rimanere 1 dopo equals", 1, list.size());
    }

    /**
     * Test T6: Get Element with Out Of Bounds Index
     * 
     * Category: Indice=OutOfBounds, Violazione boundary
     * Boundary: Indice oltre i confini validi (>= size)
     * 
     * Input:
     *   - Azione: Chiamare get(10) su lista con 1 elemento
     *   - Stato: lista con 1 elemento
     *   - Indice: 10 (fuori confini)
     * 
     * Expected Output:
     *   - Eccezione: IndexOutOfBoundsException
     *   - Invariante: lista non modificata
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testT6_GetElementWithOutOfBoundsIndex() {

        list.add("elemento");
        assertEquals("lista deve avere size 1", 1, list.size());

        list.get(10);
    }

    /**
     * Test T7: Recycle Non-Pooled Instance
     * 
     * Category: Handle=Null, Stato=NonEmpty
     * Boundary: Istanza non gestita da recycler
     * 
     * Input:
     *   - Azione: Creare lista via costruttore default, chiamare recycle()
     *   - Istanza: new RecyclableArrayList<>() (non-pooled)
     *   - Handle: null (non gestita da recycler)
     *   - Stato: lista con elementi (es. "elemento")
     * 
     * Expected Output:
     *   - Eccezione: nessuna
     *   - Effetto: lista viene cleared (size = 0)
     *   - Invariante: lista è valida e vuota dopo recycle
     */
    @Test
    public void testT7_RecycleNonPooledInstance() {
        list.add("elemento");
        assertEquals("lista deve avere size 1", 1, list.size());

        list.recycle();

        assertEquals("Size deve essere 0 dopo recycle", 0, list.size());

        boolean canAdd = list.add("nuovo elemento");
        assertTrue("Lista deve essere ancora utilizzabile dopo recycle", canAdd);
        assertEquals("Size deve essere 1 dopo add", 1, list.size());
    }

    /**
     * Test T8: Add Multiple Elements Causing Capacity Expansion
     * 
     * Category: Capacity=ExpandNeeded
     * Boundary: Superamento della capacità iniziale (default 8)
     * 
     * Input:
     *   - Azione: Aggiungere elementi finché non viene richiesta espansione
     *   - Stato iniziale: lista vuota
     *   - Elementi: aggiungere > 8 elementi (default initial capacity è 8)
     * 
     * Expected Output:
     *   - Stato: size = 10
     *   - Accesso: tutti gli elementi accessibili via get()
     *   - Contenuto: ordinamento mantenuto
     *   - Performance: espansione automatica (senza exception)
     */
    @Test
    public void testT8_AddMultipleElementsCausingCapacityExpansion() {
        assertEquals("Lista deve essere inizialmente vuota", 0, list.size());

        for (int i = 0; i < 10; i++) {
            boolean result = list.add("elemento" + i);
            assertTrue("Aggiunta elemento " + i + " deve riuscire", result);
        }
        assertEquals("Size deve essere 10 dopo 10 aggiunte", 10, list.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("Elemento " + i + " deve essere corretto", "elemento" + i, list.get(i));
        }

        assertEquals("Primo elemento deve essere elemento0", "elemento0", list.get(0));
        assertEquals("Ultimo elemento deve essere elemento9", "elemento9", list.get(9));
    }

}
