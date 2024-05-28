package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE SAFE
// ITEM_PROFIT SAFE
// OVERALL_PROFIT UNSAFE (SAFE)
public class Loop_Simplified {

	public void m() {
		for (int i = 1; i < 10; i++) {
			Frog frog = new Frog(-20);
			frog.sell(i*i); 
		}
	}
}
