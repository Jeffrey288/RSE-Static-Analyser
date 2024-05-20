package ch.ethz.rse.verify;

import java.util.Collection;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.MpqScalar;
import apron.Texpr1CstNode;
import apron.Texpr1Node;
import apron.Texpr1VarNode;
import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.numerical.NumericalAnalysis;
import ch.ethz.rse.numerical.NumericalStateWrapper;
import ch.ethz.rse.pointer.FrogInitializer;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.utils.Constants;
import polyglot.ast.Call;
import soot.Local;
import soot.SootClass;
import soot.SootHelper;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.UnitGraph;

/**
 * Main class handling verification
 * 
 */
public class Verifier extends AVerifier {

	private static final Logger logger = LoggerFactory.getLogger(Verifier.class);

	/**
	 * class to be verified
	 */
	private final SootClass c;

	/**
	 * points to analysis for verified class
	 */
	private final PointsToInitializer pointsTo;

	/**
	 * 
	 * @param c class to verify
	 */
	public Verifier(SootClass c) {
		logger.debug("Analyzing {}", c.getName());

		this.c = c;

		// pointer analysis
		// 1. executes pointer analysis,
		this.pointsTo = new PointsToInitializer(this.c);
	}

	// 2. runs numerical analysis
	protected void runNumericalAnalysis(VerificationProperty property) {
		// TODO: FILL THIS OUT

		// - You may assume the class you analyze only has a single method in addition to
		//   its constructor (called <init> in Soot). You may assume that the constructor
		//   is empty. 
		// - You can assume all analyzed methods only have integer parameters (in
		//   particular, they cannot have Frog parameters).

		for (SootMethod method : this.c.getMethods()) {

			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}
			
			NumericalAnalysis numericalAnalysis = new NumericalAnalysis(method, property, pointsTo);

		}

	}

	@Override
	public boolean checksNonNegative() {
		// TODO: FILL THIS OUT
		return true;
	}

	@Override
	public boolean checkItemProfit() {
		// TODO: FILL THIS OUT
		return true;
	}

	@Override
	public boolean checkOverallProfit() {
		// TODO: FILL THIS OUT
		return true;
	}

	// TODO: MAYBE FILL THIS OUT: add convenience methods

}
