package org.apache.bookkeeper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.apache.bookkeeper.bookie.LedgerStorage;
import org.apache.bookkeeper.stats.StatsLogger;
import org.apache.bookkeeper.bookie.ScanAndCompareGarbageCollector;

import org.apache.bookkeeper.meta.LedgerManager;
import org.apache.bookkeeper.meta.LedgerManager.LedgerRange;
import org.apache.bookkeeper.meta.LedgerManager.LedgerRangeIterator;
import org.apache.bookkeeper.bookie.GarbageCleaner;
import org.apache.bookkeeper.util.SnapshotMap;

import static org.mockito.Mockito.*;


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
    public void shouldCleanLocalLedgersNotInGlobalList() {
        // ledger attivi localmente: [1, 2]
        NavigableMap<Long, Boolean> localLedgers = new TreeMap<>();
        localLedgers.put(1L, true);
        localLedgers.put(2L, true);

        SnapshotMap<Long, Boolean> snapshotMap = mock(SnapshotMap.class);
        when(snapshotMap.snapshot()).thenReturn(localLedgers);

        LedgerManager ledgerManager = mock(LedgerManager.class);

        // ledger globali (da zookeeper): [1]
        LedgerRange globalRange = new LedgerRange(1L, 2L);  // include solo ledger 1
        LedgerRangeIterator iterator = mock(LedgerRangeIterator.class);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.next()).thenReturn(globalRange);
        when(ledgerManager.getLedgerRanges()).thenReturn(iterator);

        // cleaner che registra i ledger puliti
        GarbageCleaner cleaner = mock(GarbageCleaner.class);

        ScanAndCompareGarbageCollector gc =
            new ScanAndCompareGarbageCollector(ledgerManager, snapshotMap);

        gc.gc(cleaner);

        verify(cleaner, times(1)).clean(2L);  // solo ledger 2 deve essere rimosso
        verify(cleaner, never()).clean(1L);
    }

    

    @Test
    public void shouldCleanAllWhenGlobalLedgersIsEmpty() {
  
           GarbageCleaner cleaner = new GarbageCleaner() {
        @Override
        public void clean(long ledgerId) {
            System.out.println("Cleaned ledger: " + ledgerId);
        }
    };
    
        NavigableMap<Long, Boolean> localLedgers = new TreeMap<>();
        localLedgers.put(10L, true);
        localLedgers.put(20L, true);

        SnapshotMap<Long, Boolean> snapshotMap = mock(SnapshotMap.class);
        when(snapshotMap.snapshot()).thenReturn(localLedgers);

        LedgerManager ledgerManager = mock(LedgerManager.class);
        LedgerRangeIterator iterator = mock(LedgerRangeIterator.class);

        // non ci sono ledger globali
        when(iterator.hasNext()).thenReturn(false);
        when(ledgerManager.getLedgerRanges()).thenReturn(iterator);

        GarbageCleaner cleaner = mock(GarbageCleaner.class);

        ScanAndCompareGarbageCollector gc =
            new ScanAndCompareGarbageCollector(ledgerManager, snapshotMap);

        gc.gc(cleaner);

        verify(cleaner).clean(10L);
        verify(cleaner).clean(20L);
    }

}
