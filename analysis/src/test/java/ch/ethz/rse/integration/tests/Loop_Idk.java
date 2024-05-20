package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE SAFE
// ITEM_PROFIT SAFE
// OVERALL_PROFIT SAFE

public class Loop_Idk {

	public void m() {
        Frog[] frogs = new Frog[20];
		for (int i = 1; i < 10; i++) {
			frogs[i] = new Frog(-20);
			frogs[i].sell(i);
			frogs[i].sell(i+1);
			frogs[i].sell(i*2-1);
			frogs[i].sell(i*i);
		}		
	}
}