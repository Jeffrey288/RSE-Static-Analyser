package ch.ethz.rse.pointer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import apron.Texpr1Node;
import ch.ethz.rse.utils.Constants;
import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.JastAddJ.Expr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.spark.pag.Node;

/**
 * Convenience class which helps determine the {@link FrogInitializer}s
 * potentially used to create objects pointed to by a given variable
 */
public class PointsToInitializer {

	private static final Logger logger = LoggerFactory.getLogger(PointsToInitializer.class);

	/**
	 * Internally used points-to analysis
	 */
	private final PointsToAnalysisWrapper pointsTo;

	/**
	 * class for which we are running points-to
	 */
	private final SootClass c;

	/**
	 * Maps abstract object indices to initializers
	 */
	private final Map<Node, FrogInitializer> initializers = new HashMap<Node, FrogInitializer>();

	/**
	 * All {@link FrogInitializer}s, keyed by method
	 */
	private final Multimap<SootMethod, FrogInitializer> perMethod = HashMultimap.create();

	// CONSTRUCTOR
	public PointsToInitializer(SootClass c) {
		this.c = c;
		logger.debug("Running points-to analysis on " + c.getName());
		this.pointsTo = new PointsToAnalysisWrapper(c);
		logger.debug("Analyzing initializers in " + c.getName());
		this.analyzeAllInitializers();
	} 	
		
	private void analyzeAllInitializers() {
		for (SootMethod method : this.c.getMethods()) {

			if (method.getName().contains("<init>")) {
				// skip constructor of the class
				// e.g. <Foo: void <init>()>
				continue;
			}

			// populate data structures perMethod and initializers
			// TODO: FILL THIS OUT

			/**
			 * Usually the code is in the form of two separate statmenets:
			 * $r0 = new ch.ethz.rse.Frog
			 * specialinvoke $r0.<ch.ethz.rse.Frog: void <init>(int)>(3)
			 */
			int num_initializers = 0;
			for (Unit unit: method.getActiveBody().getUnits()) { // a statement
				logger.debug(unit.toString());
				if (unit instanceof JInvokeStmt) { // assumpiton: .sell or constructor
					// logger.debug("We have a JInvokeStmt!");
					JInvokeStmt stmt = (JInvokeStmt) unit;
					InvokeExpr invokeExpr = stmt.getInvokeExpr();
					if (invokeExpr instanceof JSpecialInvokeExpr) {
						// logger.debug("We have a JSpecialInvokeExpr!");
						JSpecialInvokeExpr expr = (JSpecialInvokeExpr) invokeExpr;
						if (isRelevantInit(expr)) {
							// assumption: the constructor Frog takes as arguments (production_cost) only integer constants (never local variables)
							// 		you can put a constant expression in the constructor in the master solution, but still
							//		it works: new Frog((int) (1+3/4.0+2)*1); is converted to specialinvoke $r0.<ch.ethz.rse.Frog: void <init>(int)>(3)
							// logger.debug(expr.toString());
							// logger.debug(expr.getArg(0).toString());
							// logger.debug("" + ((IntConstant) expr.getArg(0)).value);
							FrogInitializer initializer = new FrogInitializer(stmt, num_initializers++, ((IntConstant) expr.getArg(0)).value);
							perMethod.put(method, initializer);
							
							// Node: Represents every node in the pointer assignment graph.
							// https://plg.uwaterloo.ca/~olhotak/pubs/cc03.pdf#page=4
							// logger.debug(expr.getBase().toString());
							// logger.debug(getAllocationNodes(expr).toString());
							for (Node node: getAllocationNodes(expr)) {
								initializers.put(node, initializer);
							}
						}
					}
				}
			}

		}
	}

	// TODO: MAYBE FILL THIS OUT: add convenience methods

	public Collection<FrogInitializer> getInitializers(SootMethod method) {
		return this.perMethod.get(method);
	}

	// should probably use this function
	public List<FrogInitializer> pointsTo(Local base) {
		Collection<Node> nodes = this.pointsTo.getNodes(base);
		List<FrogInitializer> initializers = new LinkedList<FrogInitializer>();
		for (Node node : nodes) {
			FrogInitializer initializer = this.initializers.get(node);
			if (initializer != null) {
				// ignore nodes that were not initialized
				initializers.add(initializer);
			}
		}
		return initializers;
	}

	/**
	 * Returns all allocation nodes that could correspond to the given invokeExpression, which must be a call to Frog init function
	 * Note that more than one node can be returned.
	 * TODO: Create a test case with more than one node per invokeExpr
	 */
	public Collection<Node> getAllocationNodes(JSpecialInvokeExpr invokeExpr){
		if(!isRelevantInit(invokeExpr)){
			throw new RuntimeException("Call to getAllocationNodes with " + invokeExpr.toString() + "which is not an init call for the Frog class");
		}
		Local base = (Local) invokeExpr.getBase();
		Collection<Node> allocationNodes = this.pointsTo.getNodes(base);
		return allocationNodes;
	}

	public boolean isRelevantInit(JSpecialInvokeExpr invokeExpr){
		Local base = (Local) invokeExpr.getBase();
		boolean isRelevant = base.getType().toString().equals(Constants.FrogClassName);
		boolean isInit = invokeExpr.getMethod().getName().equals("<init>");
		return isRelevant && isInit;
	}
}
