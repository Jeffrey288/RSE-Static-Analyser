package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE SAFE
// ITEM_PROFIT UNSAFE
// OVERALL_PROFIT UNSAFE
public class Random_Test_2 {

  public static void m1() {
    int i = 1;
    int j = 3;
    for (int k = 10; k * k < 100; k--) {
      i = i + j;
      j = i + j;
    }
    boolean a = i > 1000000;
    boolean b = j - i < 200000;
    boolean c = (2 * j - i) * (2 * i - j) < (3 * j - 2 * i) * (3 * i - 2 * j) ;
    boolean d = a || b || c; 
    Frog frog = new Frog(100);
    frog.sell(d ? (j - i) : (i - j));
  }
}