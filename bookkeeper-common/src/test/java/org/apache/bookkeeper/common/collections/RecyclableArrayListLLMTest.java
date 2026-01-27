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
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * LLM-generated tests guided by Category Partition prompts.
 * These complement the manual boundary tests to broaden coverage on
 * state transitions, type/null handling, iterator stability, and subList ranges.
 */
public class RecyclableArrayListLLMTest {

    private RecyclableArrayList<String> list;

    @Before
    public void setUp() {
        list = new RecyclableArrayList<>();
    }

    /**
     * LLM-1: remove on empty list (post-recycle) with invalid index boundary.
     * Prompt focus: state transitions + boundary violations.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testLLM1_RemoveFromEmptyListAfterRecycle() {
        list.add("element");
        list.recycle();
        assertEquals("Lista deve essere vuota dopo recycle", 0, list.size());

        // Boundary violation: remove from empty list
        list.remove(0);
    }

    /**
     * LLM-2: contains() across type/null and presence/absence partitions.
     * Prompt focus: type boundaries, null handling, presence vs absence.
     */
    @Test
    public void testLLM2_ContainsBoundaryValuesNullAndPresenceAbsence() {
        list.add("first");
        assertFalse("Lista non deve contenere 'nonexistent'", list.contains("nonexistent"));
        assertTrue("Lista deve contenere 'first'", list.contains("first"));

        list.add(null);
        assertTrue("Lista deve contenere null dopo aggiunta", list.contains(null));

        list.add("second");
        assertTrue(list.contains("second"));
        assertTrue(list.contains(null));
        assertFalse(list.contains("missing"));
    }

    /**
     * LLM-3: iterator stability across state transitions and capacity expansion.
     * Prompt focus: empty → single → multiple, post-modification consistency.
     */
    @Test
    public void testLLM3_IteratorBehaviorAcrossStateTransitions() {
        int count = 0;
        for (String ignored : list) {
            count++;
        }
        assertEquals("Iterator su lista vuota deve avere 0 elementi", 0, count);

        list.add("single");
        count = 0;
        for (String ignored : list) {
            count++;
        }
        assertEquals("Iterator su lista con 1 elemento deve contare 1", 1, count);

        for (int i = 0; i < 10; i++) {
            list.add("elem" + i);
        }
        count = 0;
        for (String ignored : list) {
            count++;
        }
        assertEquals("Iterator su lista con 11 elementi deve contare 11", 11, count);

        list.remove(5);
        count = 0;
        for (String ignored : list) {
            count++;
        }
        assertEquals("Iterator dopo remove deve contare 10", 10, count);
    }

    /**
     * LLM-4: subList boundary behavior and slice partitions.
     * Prompt focus: index boundaries, empty slice, full vs partial slices.
     */
    @Test
    public void testLLM4_SubListBoundaryConditionsAndSlicePartitions() {
        for (int i = 0; i < 5; i++) {
            list.add("element" + i);
        }
        assertEquals("Setup: lista deve avere 5 elementi", 5, list.size());

        List<String> fullSlice = list.subList(0, 5);
        assertEquals("Full slice deve contenere 5 elementi", 5, fullSlice.size());
        assertEquals("Full slice primo elemento deve essere element0", "element0", fullSlice.get(0));

        List<String> emptySlice = list.subList(2, 2);
        assertEquals("Empty slice deve avere size 0", 0, emptySlice.size());

        List<String> partialSlice = list.subList(1, 4);
        assertEquals("Partial slice [1,4) deve contenere 3 elementi", 3, partialSlice.size());
        assertEquals("Elemento 0 di partial slice deve essere element1", "element1", partialSlice.get(0));
        assertEquals("Elemento 2 di partial slice deve essere element3", "element3", partialSlice.get(2));

        assertEquals("Lista originale deve mantenere 5 elementi dopo subList", 5, list.size());
        assertEquals("Lista originale[0] deve rimanere element0", "element0", list.get(0));
    }
}
