package org.apache.bookkeeper.common.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Control-flow suite tests per RecyclableArrayList
 */
public class RecyclableArrayListControlFlowTest {

	/**
	 * Copre recycle() branch dove handle != null (istanza pooled) e verifica il riutilizzo
	 */
	@Test
	public void testRecyclePooledReusesInstanceAndClears() {
		RecyclableArrayList.Recycler<String> recycler = new RecyclableArrayList.Recycler<>();
		RecyclableArrayList<String> first = recycler.newInstance();
		first.add("a");

		first.recycle(); // dovrebbe pulire e restituire al pool

		RecyclableArrayList<String> second = recycler.newInstance();
		assertSame("Il recycler pooled dovrebbe restituire l'istanza riciclata", first, second);
		assertEquals("L'istanza riciclata deve essere pulita", 0, second.size());
		// controllo che l'istanza riciclata sia ancora utilizzabile
		second.add("b");
		assertEquals("la lista deve rimanere utilizzabile dopo il recycle", "b", second.get(0));
	}

	/**
	 * liste uguali, dimensioni diverse, e tipo errato
	 */
	@Test
	public void testEqualsControlFlowSameContentAndDifferentSizes() {
		RecyclableArrayList<String> list1 = new RecyclableArrayList<>();
		RecyclableArrayList<String> list2 = new RecyclableArrayList<>();
		RecyclableArrayList<String> list3 = new RecyclableArrayList<>();

		list1.add("x");
		list1.add("y");

		list2.add("x");
		list2.add("y");

		list3.add("x");

		assertTrue("Liste con stesso contenuto devono essere uguali", list1.equals(list2));
		assertFalse("Liste con dimensioni diverse non devono essere uguali", list1.equals(list3));
		assertFalse("Equals deve ritornare false per tipi diversi", list1.equals("not a list"));
	}
}
