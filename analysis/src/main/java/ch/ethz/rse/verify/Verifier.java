package ch.ethz.rse.verify;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.Abstract1;
import apron.ApronException;
import apron.MpqScalar;
import apron.Scalar;
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
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
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

		// - You may assume the class you analyze only has a single method in addition
		// to
		// its constructor (called <init> in Soot). You may assume that the constructor
		// is empty.
		// - You can assume all analyzed methods only have integer parameters (in
		// particular, they cannot have Frog parameters).

		for (SootMethod method : this.c.getMethods()) {

			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				continue;
			}

			// For debug, print the whole program
			logger.debug("-=-=-" + method.getName() + "-=-=-");
			for (Unit unit : method.getActiveBody().getUnits()) { // a statement
				logger.debug(unit.toString());
			}
			logger.debug("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");

			numericalAnalysis.put(method, new NumericalAnalysis(method, property, pointsTo));

		}

	}

	@Override
	public boolean checksNonNegative() {

		// TODO: FILL THIS OUT
		for (SootMethod method : numericalAnalysis.keySet()) {
			NumericalAnalysis analysis = numericalAnalysis.get(method);
			for (Unit unit : method.getActiveBody().getUnits()) {
				Abstract1 abs = analysis.getFlowBefore(unit).get();
				if (unit instanceof JInvokeStmt) {
					InvokeExpr expr = ((JInvokeStmt) unit).getInvokeExpr();
					if (expr instanceof JVirtualInvokeExpr) {
						JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) expr;

						try {
							if (!abs.isBottom(analysis.man)) { // if abs is empty, then this statement cannot be reached
								Value arg = invokeExpr.getArg(0);
								if (arg instanceof JimpleLocal) {
									String argName = ((JimpleLocal) arg).getName();
									Scalar lowerBound = abs.getBound(analysis.man, argName).inf();
									if (lowerBound.cmp(0) == -1) { // is negative
										return false;
									}
								} else if (arg instanceof IntConstant) {
									int argValue = ((IntConstant) arg).value;
									if (argValue < 0) {
										return false;
									}
								} else {
									throw new RuntimeException("arg is not JimpleLocal or IntConstant!");
								}
							}
						} catch (ApronException e) {
							throw new RuntimeException(e);
						}
						
						logger.debug(invokeExpr.toString());
						logger.debug(abs.toString());
						
					}

				}

			}
		}

		return true;
	}

	@Override
	public boolean checkItemProfit() {

		// TODO: FILL THIS OUT
		for (SootMethod method : numericalAnalysis.keySet()) {
			NumericalAnalysis analysis = numericalAnalysis.get(method);
			for (Unit unit : method.getActiveBody().getUnits()) {
				Abstract1 abs = analysis.getFlowBefore(unit).get();
				try {
					if (unit instanceof JInvokeStmt) {
						InvokeExpr expr = ((JInvokeStmt) unit).getInvokeExpr();
						if (expr instanceof JVirtualInvokeExpr) {
							JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) expr;
							if (!abs.isBottom(analysis.man)) { // if abs is empty, then this statement cannot be reached

								// get the object, and its value
								Local base = (Local) invokeExpr.getBase();
								List<FrogInitializer> initializers = pointsTo.pointsTo(base);

								Value arg = invokeExpr.getArg(0);
								if (arg instanceof JimpleLocal) {
									String argName = ((JimpleLocal) arg).getName();
									Scalar lowerBound = abs.getBound(analysis.man, argName).inf();
									for (FrogInitializer initializer : initializers) {
										if (lowerBound.cmp(initializer.argument) == -1) { // is strictly less than
											return false;
										}
									}
								} else if (arg instanceof IntConstant) {
									int argValue = ((IntConstant) arg).value;
									for (FrogInitializer initializer : initializers) {
										if (argValue < initializer.argument) {
											return false;
										}
									}
								} else {
									throw new RuntimeException("arg is not JimpleLocal or IntConstant!");
								}

							}
						}
					}
				} catch (ApronException e) {
					throw new RuntimeException(e);
				}

			}
		}

		return true;
	}

	@Override
	public boolean checkOverallProfit() {
		// TODO: FILL THIS OUT
		for (SootMethod method : numericalAnalysis.keySet()) {
			NumericalAnalysis analysis = numericalAnalysis.get(method);

			for (Unit unit : method.getActiveBody().getUnits()) {
				Abstract1 abs = analysis.getFlowBefore(unit).get();
				try {
					if (unit instanceof JReturnVoidStmt) {
						if (!abs.isBottom(analysis.man)) { // if abs is empty, then this statement cannot be reached
							Scalar lowerBound = abs.getBound(analysis.man, "FROG_OVERALL_PROFIT").inf();
							Scalar lowerBoundInterval = abs.getBound(analysis.man, "FROG_OVERALL_PROFIT_INTERVAL").inf();
							if (lowerBound.cmp(0) == -1 && lowerBoundInterval.cmp(0) == -1) { // is negative
								return false;
							}
							// note that both lowerBound and lowerBoundInterval are OVER_APPROXIMATIONS of the actual lower bound
							// i.e. lowerBound < actual && lowerBoundInterval < actual
						}
					}
				} catch (ApronException e) {
					throw new RuntimeException(e);
				}

			}

		}

		return true;
	}

	// TODO: MAYBE FILL THIS OUT: add convenience methods

}
