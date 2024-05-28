package ch.ethz.rse.integration.tests;

import ch.ethz.rse.Frog;

// expected results:
// NON_NEGATIVE UNSAFE (SAFE)
// ITEM_PROFIT UNSAFE
// OVERALL_PROFIT UNSAFE
public class Random_Test_3 {

  public static void m1() {
    int i = 1;
    int j = 3;
    for (int k = 1; k < 5; k++) {
      i = i + j;
      j = i + j;
    }

    Frog frog;
    if (i < 70) {
      frog = new Frog(15);
    } else if (i < 71) {
        frog = new Frog(42);  // Random integer
    } else if (i < 72) {
        frog = new Frog(67);  // Random integer
    } else if (i < 73) {
        frog = new Frog(23);  // Random integer
    } else if (i < 74) {
        frog = new Frog(89);  // Random integer
    } else if (i < 75) {
        frog = new Frog(34);  // Random integer
    } else if (i < 76) {
        frog = new Frog(76);  // Random integer
    } else if (i < 77) {
        frog = new Frog(58);  // Random integer
    } else if (i < 78) {
        frog = new Frog(91);  // Random integer
    } else if (i < 79) {
        frog = new Frog(45);  // Random integer
    } else if (i < 80) {
        frog = new Frog(63);  // Random integer
    } else if (i < 81) {
        frog = new Frog(37);  // Random integer
    } else if (i < 82) {
        frog = new Frog(29);  // Random integer
    } else if (i < 83) {
        frog = new Frog(84);  // Random integer
    } else if (i < 84) {
        frog = new Frog(19);  // Random integer
    } else {
        frog = new Frog(50);  // Random integer
    }

    frog.sell(j - i);
    
  }
}