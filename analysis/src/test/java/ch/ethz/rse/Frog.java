package ch.ethz.rse;

/**
 * We are verifying calls into this class
 * 
 */
public final class Frog {
	
	// cheat
	public static boolean non_negative = true;
	public static boolean item_profit = true;

	// total profit made ever
	public static int total_profit = 0;
  
	// Production cost of the item
	private final int production_cost;
  
	public Frog(int production_cost) {
	  this.production_cost = production_cost;
	}
  
	public void sell(int price) {
	   // check NON_NEGATIVE
	//   assert 0 <= price;
	  if (!(0 <= price)) { non_negative = false; }
  
	  // check ITEM_PROFIT
	//   assert this.production_cost <= price;
	  if (!(this.production_cost <= price)) { item_profit = false; }
	  Frog.total_profit += (price - this.production_cost);
  
	  // check OVERALL_PROFIT
	  // (check only upon program termination)
	  // assert Frog.total_profit >= 0;
	}
  }