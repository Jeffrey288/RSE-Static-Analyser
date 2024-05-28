package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE SAFE
// ITEM_PROFIT SAFE
// OVERALL_PROFIT SAFE
public class Random_Test_4 {

  public static void m1() {
    Frog frog;
    int i = 50;
    int j = 100;
    i = -1 * (i * (j + i) - j * (i + j));
    if (i < 70) {
        frog = new Frog(150000);
    } else {
        frog = new Frog(420);  // Random integer
    }
    i = -1 * (i * (j + i) - j * (i + j));
    if (i > 0) {
        frog.sell(i);
    } else {
        frog.sell(-1 * i);
    }
    
  }
}