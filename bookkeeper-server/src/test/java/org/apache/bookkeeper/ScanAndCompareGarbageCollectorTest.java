package org.apache.bookkeeper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.mockito.Mockito.*;

public class ScanAndCompareGarbageCollectorTest {

    private LedgerStorage ledgerStorage;
    private LedgerManager ledgerManager;
    private StatsLogger statsLogger;
    private ScanAndCompareGarbageCollector gc;

    @BeforeEach
    public void setup() {
        ledgerStorage = mock(LedgerStorage.class);
        ledgerManager = mock(LedgerManager.class);
        statsLogger = mock(StatsLogger.class);
        gc = new ScanAndCompareGarbageCollector(ledgerStorage, ledgerManager, statsLogger);
    }

    @Test
    public void testGarbageCollectionRemovesOnlyUnusedLedgers() throws Exception {
        when(ledgerStorage.getActiveLedgers()).thenReturn(Set.of(1L, 2L));

        LedgerManager.LedgerRange range = new LedgerManager.LedgerRange(1L, 3L);
        Enumeration<LedgerManager.LedgerRange> ranges = Collections.enumeration(List.of(range));
        when(ledgerManager.getLedgerRanges()).thenReturn(ranges);

        doNothing().when(ledgerManager).removeLedger(anyLong());

        gc.doGc();

        verify(ledgerManager, times(1)).removeLedger(3L);
        verify(ledgerManager, never()).removeLedger(1L);
        verify(ledgerManager, never()).removeLedger(2L);
    }
}
