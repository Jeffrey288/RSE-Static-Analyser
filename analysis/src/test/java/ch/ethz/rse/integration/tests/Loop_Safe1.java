package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE SAFE
// ITEM_PROFIT SAFE
// OVERALL_PROFIT SAFE
public class Loop_Safe1 {

	public void m() {
		for (int i = 1; i < 10; i++) {
			Frog frog = new Frog(-20);
			frog.sell(i);
			frog.sell(i+1);
			frog.sell(i*2-1);
			frog.sell(i*i);
		}		
	}
}
