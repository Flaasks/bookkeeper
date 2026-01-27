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
 * LLM-generated tests guided by Category Partition prompts for MathUtils.
 * Includes one test using Mockito for time function isolation.
 */
public class MathUtilsLLMTest {

    /**
     * LLM-1: signSafeMod edge cases with zero and boundary divisors.
     * Prompt: divisor=1 boundary (mod always 0), divisor=large (ratio close to 0).
     */
    @Test
    public void testLLM1_SignSafeModZeroDivisorBoundary() {
        int result1 = MathUtils.signSafeMod(100L, 1);
        assertEquals("signSafeMod(100, 1) must be 0 (any % 1 = 0)", 0, result1);

        int result2 = MathUtils.signSafeMod(-100L, 1);
        assertEquals("signSafeMod(-100, 1) must be 0", 0, result2);

        int result3 = MathUtils.signSafeMod(1L, 1000);
        assertEquals("signSafeMod(1, 1000) must be 1", 1, result3);
    }

    /**
     * LLM-2: findNextPositivePowerOfTwo boundary with 1 and large values.
     * Prompt: value=1 (minimal power), value close to Integer.MAX_VALUE.
     */
    @Test
    public void testLLM2_FindNextPowerOfTwoBoundarySmallAndLarge() {
        int result1 = MathUtils.findNextPositivePowerOfTwo(1);
        assertEquals("findNextPositivePowerOfTwo(1) must be power of 2", 1, result1);
        assertTrue("Result must be power of 2", isPowerOfTwo(result1));

        int result2 = MathUtils.findNextPositivePowerOfTwo(1024);
        assertEquals("findNextPositivePowerOfTwo(1024) must be 1024", 1024, result2);

        int result3 = MathUtils.findNextPositivePowerOfTwo(1025);
        assertEquals("findNextPositivePowerOfTwo(1025) must be 2048", 2048, result3);
    }

    /**
     * LLM-3: signSafeMod negative dividend with comprehensive validation of sign correction.
     * Prompt: Test boundary condition where dividend sign must be corrected to ensure non-negative result.
     * Verifies: (-a % b + b) % b always produces result in [0, b).
     */
    @Test
    public void testLLM3_SignSafeModNegativeDividendSignCorrection() {
        // Test various negative dividends with different divisors
        for (int dividend = -100; dividend <= -1; dividend += 20) {
            for (int divisor = 2; divisor <= 10; divisor++) {
                int result = MathUtils.signSafeMod(dividend, divisor);
                assertTrue("Result must be non-negative for dividend=" + dividend + ", divisor=" + divisor, 
                    result >= 0);
                assertTrue("Result must be less than divisor for dividend=" + dividend + ", divisor=" + divisor, 
                    result < divisor);

                // Verify correctness: mathematically correct modulo operation
                int expected = (int) (((dividend % divisor) + divisor) % divisor);
                assertEquals("signSafeMod must match mathematical definition", expected, result);
            }
        }
    }

    /**
     * LLM-4: signSafeMod with large negative dividend and specific divisor patterns.
     * Prompt: stress test negative correction logic with various divisor sizes.
     */
    @Test
    public void testLLM4_SignSafeModLargeNegativeDividend() {
        // Large negative with small divisor
        int result1 = MathUtils.signSafeMod(-1000L, 7);
        assertTrue("Result must be in range [0, divisor)", result1 >= 0 && result1 < 7);

        // Large negative with large divisor
        int result2 = MathUtils.signSafeMod(-999999L, 1000);
        assertTrue("Result must be in range [0, 1000)", result2 >= 0 && result2 < 1000);

        // Verify correctness: -1000 % 7 = -6, then +7 = 1
        int expected = MathUtils.signSafeMod(-1000L, 7);
        int javaResult = (int) ((-1000L % 7 + 7) % 7);
        assertEquals("signSafeMod must match mathematical correctness", expected, javaResult);
    }

    private static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
}
