package ch.ethz.rse.verify;

import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.numerical.NumericalAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;

import java.util.HashMap;
import java.util.Map;


public abstract class AVerifier {

	private static final Logger logger = LoggerFactory.getLogger(AVerifier.class);
	
	/**
	 * result of running numerical analysis, per method
	 */
	protected final Map<SootMethod, NumericalAnalysis> numericalAnalysis = new HashMap<SootMethod, NumericalAnalysis>();

	/**
	 * 
	 * @param property
	 * @return true if <code>property</code> is SAFE, false if it may be UNSAFE
	 */
	public boolean check(VerificationProperty property) {
		long startTime = System.nanoTime();

		this.runNumericalAnalysis(property);

		boolean ret;
		switch (property) {
		case NON_NEGATIVE:
			ret = this.checksNonNegative();
			break;
		case ITEM_PROFIT:
			ret = this.checkItemProfit();
			break;
		case OVERALL_PROFIT:
			ret = this.checkOverallProfit();
			break;
		default:
			throw new UnsupportedOperationException(property.toString());
		}

		long endTime = System.nanoTime();
		long durationMilliseconds = (endTime - startTime) / 1000000;
		logger.debug("Runtime: Checked property {} in {}ms", property, durationMilliseconds);

		return ret;
	}

	/**
	 * 
	 * @return true if NON_NEGATIVE is SAFE, false if it may be UNSAFE
	 */
	protected abstract boolean checksNonNegative();

	/**
	 * 
	 * @return true if ITEM_PROFIT is SAFE, false if it may be UNSAFE
	 */
	protected abstract boolean checkItemProfit();

	/**
	 * 
	 * @return true if OVERALL_PROFIT is SAFE, false if it may be UNSAFE
	 */
	protected abstract boolean checkOverallProfit();

	/**
	 * Populate {@link #numericalAnalysis}
	 * 
	 * @param property the property about to be verified
	 */
	protected abstract void runNumericalAnalysis(VerificationProperty property);
}
