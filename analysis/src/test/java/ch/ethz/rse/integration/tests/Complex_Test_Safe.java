package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE SAFE
// ITEM_PROFIT SAFE
// OVERALL_PROFIT SAFE

public class Complex_Test_Safe {

  public static void m1(int i, int j) {
    Frog frog_with_hat = new Frog((int) (1+3/4.0+2)*1);
    frog_with_hat.sell(5);
    frog_with_hat.sell(6);

    Frog frog_with_pants = new Frog(2);
    frog_with_pants.sell(2);

    Frog frog_with_big_tongue = new Frog(20);

    for (int k = 0; k < 100; k++) {
      i *= -(i + j);
    }
    if (i < 1) {
      frog_with_hat = frog_with_pants;
    } else {
      frog_with_hat = frog_with_big_tongue;
    }
    frog_with_hat.sell(5);
  }
}