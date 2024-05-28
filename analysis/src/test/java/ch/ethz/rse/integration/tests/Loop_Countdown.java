package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE SAFE
// ITEM_PROFIT SAFE
// OVERALL_PROFIT SAFE
public class Loop_Countdown {

	public void m1() {
        Frog frog = new Frog(10);
        for (int i = 20; i > 18; i--) {
            frog.sell(i);
        }
	}
}
