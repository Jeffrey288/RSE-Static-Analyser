package ch.ethz.rse.numerical;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import apron.Abstract1;
import apron.ApronException;
import apron.Coeff;
import apron.Environment;
import apron.Interval;
import apron.Manager;
import apron.MpqScalar;
import apron.Polka;
import apron.Scalar;
import apron.Tcons1;
import apron.Texpr1BinNode;
import apron.Texpr1CstNode;
import apron.Texpr1Intern;
import apron.Texpr1Node;
import apron.Texpr1VarNode;
import ch.ethz.rse.VerificationProperty;
import ch.ethz.rse.pointer.FrogInitializer;
import ch.ethz.rse.pointer.PointsToInitializer;
import ch.ethz.rse.utils.Constants;
import ch.ethz.rse.verify.EnvironmentGenerator;
import gmp.Mpq;
import soot.ArrayType;
import soot.DoubleType;
import soot.Local;
import soot.RefType;
import soot.SootHelper;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.MulExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.SubExpr;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.JAddExpr;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JMulExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JNegExpr;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JSubExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

/**
 * Convenience class running a numerical analysis on a given {@link SootMethod}
 */
public class NumericalAnalysis extends ForwardBranchedFlowAnalysis<NumericalStateWrapper> {

	private static final Logger logger = LoggerFactory.getLogger(NumericalAnalysis.class);

	private final SootMethod method;

	/**
	 * the property we are verifying
	 */
	private final VerificationProperty property;

	/**
	 * the pointer analysis result we are verifying
	 */
	private final PointsToInitializer pointsTo;

	/**
	 * number of times this loop head was encountered during analysis
	 */
	private HashMap<Unit, IntegerWrapper> loopHeads = new HashMap<Unit, IntegerWrapper>();
	/**
	 * Previously seen abstract state for each loop head
	 */
	private HashMap<Unit, NumericalStateWrapper> loopHeadState = new HashMap<Unit, NumericalStateWrapper>();

	/**
	 * Numerical abstract domain to use for analysis: Convex polyhedra
	 */
	public final Manager man = new Polka(true);

	public final Environment env;

	/**
	 * We apply widening after updating the state at a given merge point for the
	 * {@link WIDENING_THRESHOLD}th time
	 */
	private static final int WIDENING_THRESHOLD = 6;

	/**
	 * 
	 * @param method   method to analyze
	 * @param property the property we are verifying
	 */
	public NumericalAnalysis(SootMethod method, VerificationProperty property, PointsToInitializer pointsTo) {
		super(SootHelper.getUnitGraph(method));

		UnitGraph g = SootHelper.getUnitGraph(method);

		this.property = property;

		this.pointsTo = pointsTo;

		this.method = method;

		this.env = new EnvironmentGenerator(method, pointsTo).getEnvironment();

		// initialize counts for loop heads
		for (Loop l : new LoopNestTree(g.getBody())) {
			loopHeads.put(l.getHead(), new IntegerWrapper(0));
		}

		// perform analysis by calling into super-class
		logger.info("Analyzing {} in {}", method.getName(), method.getDeclaringClass().getName());
		doAnalysis(); // calls newInitialFlow, entryInitialFlow, merge, flowThrough, and stops when a
						// fixed point is reached
	}

	/**
	 * Report unhandled instructions, types, cases, etc.
	 * 
	 * @param task description of current task
	 * @param what
	 */
	public static void unhandled(String task, Object what, boolean raiseException) {
		String description = task + ": Can't handle " + what.toString() + " of type " + what.getClass().getName();

		if (raiseException) {
			logger.error("Raising exception " + description);
			throw new UnsupportedOperationException(description);
		} else {
			logger.error(description);

			// print stack trace
			StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stackTrace.length; i++) {
				logger.error(stackTrace[i].toString());
			}
		}
	}

	@Override
	protected void copy(NumericalStateWrapper source, NumericalStateWrapper dest) {
		source.copyInto(dest);
	}

	@Override
	protected NumericalStateWrapper newInitialFlow() {
		// should be bottom (only entry flows are not bottom originally)
		// logger.debug("newInitalFlow was called!");
		return NumericalStateWrapper.bottom(man, env);
	}

	@Override
	protected NumericalStateWrapper entryInitialFlow() {
		// state of entry points into function
		logger.debug("entryInitalFlow was called!");
		NumericalStateWrapper ret = NumericalStateWrapper.top(man, env);

		// TODO: MAYBE FILL THIS OUT
		try {
			Abstract1 abs = ret.get();
			Texpr1Intern intern = new Texpr1Intern(env, new Texpr1CstNode(new MpqScalar(0)));
			abs = abs.assignCopy(man, "FROG_OVERALL_PROFIT", intern, null);
			ret.set(abs);
		} catch (ApronException e) {
			throw new RuntimeException(e);
		}

		return ret;
	}

	@Override
	protected void merge(Unit succNode, NumericalStateWrapper w1, NumericalStateWrapper w2, NumericalStateWrapper w3) {
		// merge the two states from w1 and w2 and store the result into w3
		
		// TODO: FILL THIS OUT
		boolean isLoop = loopHeads.containsKey(succNode);
		int num_iters = 0;
		if (isLoop) {
			num_iters = loopHeads.get(succNode).value++;
			logger.debug("in merge: " + succNode + ", visited " + num_iters + " times");
		} else {
			logger.debug("in merge: " + succNode);
		}
		
		try {
			if (isLoop && num_iters > WIDENING_THRESHOLD) {
				w3.set(this.widenFixed(w1.get(), w2.get())); // widening
			} else {
				// should be join, because we want to overapproximate
				w3.set(w1.get().joinCopy(man, w2.get())); // joining
			}
		} catch (ApronException e) {
			throw new RuntimeException(e);
		}

		logger.debug("w1: " + w1);
		logger.debug("w2: " + w2);
		logger.debug("w3: " + w3);
	}

	@Override
	protected void merge(NumericalStateWrapper src1, NumericalStateWrapper src2, NumericalStateWrapper trg) {
		// this method is never called, we are using the other merge instead
		throw new UnsupportedOperationException();
	}

	@Override
	protected void flowThrough(NumericalStateWrapper inWrapper, Unit op, List<NumericalStateWrapper> fallOutWrappers,
			List<NumericalStateWrapper> branchOutWrappers) {
		logger.debug(inWrapper + " " + op + " => ?");

		// TODO: delete later, is used to test stuff
		// if (unitState.containsKey(op)) {
		// 	NumericalStateWrapper state = unitState.get(op);
		// 	try {
		// 		if (state.get().isIncluded(man, inWrapper.get())) {
		// 			throw new RuntimeException("inWrapper not included in state");
		// 		}
		// 	} catch (ApronException e) {
		// 		throw new RuntimeException(e);
		// 	}
		// }
		// store the state
		// unitState.put(op, inWrapper);

		Stmt s = (Stmt) op;

		// fallOutWrapper is the wrapper for the state after running op,
		// assuming we move to the next statement. Do not overwrite
		// fallOutWrapper, but use its .set method instead
		assert fallOutWrappers.size() <= 1;
		NumericalStateWrapper fallOutWrapper = null;
		if (fallOutWrappers.size() == 1) {
			fallOutWrapper = fallOutWrappers.get(0);
			inWrapper.copyInto(fallOutWrapper);
		}

		// branchOutWrapper is the wrapper for the state after running op,
		// assuming we follow a conditional jump. It is therefore only relevant
		// if op is a conditional jump. In this case, (i) fallOutWrapper
		// contains the state after "falling out" of the statement, i.e., if the
		// condition is false, and (ii) branchOutWrapper contains the state
		// after "branching out" of the statement, i.e., if the condition is
		// true.
		assert branchOutWrappers.size() <= 1;
		NumericalStateWrapper branchOutWrapper = null;
		if (branchOutWrappers.size() == 1) {
			branchOutWrapper = branchOutWrappers.get(0);
			inWrapper.copyInto(branchOutWrapper);
		}

		try {
			if (s instanceof DefinitionStmt) {
				// handle assignment

				DefinitionStmt sd = (DefinitionStmt) s;
				Value left = sd.getLeftOp();
				Value right = sd.getRightOp();

				// We are not handling these cases:
				if (!(left instanceof JimpleLocal)) {
					unhandled("Assignment to non-local variable", left, true);
				} else if (left instanceof JArrayRef) {
					unhandled("Assignment to a non-local array variable", left, true);
				} else if (left.getType() instanceof ArrayType) {
					unhandled("Assignment to Array", left, true);
				} else if (left.getType() instanceof DoubleType) {
					unhandled("Assignment to double", left, true);
				} else if (left instanceof JInstanceFieldRef) {
					unhandled("Assignment to field", left, true);
				}

				if (left.getType() instanceof RefType) {
					// assignments to references are handled by pointer analysis
					// no action necessary
				} else {
					// handle assignment
					handleDef(fallOutWrapper, left, right);
				}

			} else if (s instanceof JIfStmt) {
				// handle if

				// TODO: FILL THIS OUT
				JIfStmt jIfStmt = (JIfStmt) s;
				if (jIfStmt.getCondition() instanceof ConditionExpr) {
					ConditionExpr conditionExpr = (ConditionExpr) jIfStmt.getCondition();
					Value op1 = conditionExpr.getOp1();
					Value op2 = conditionExpr.getOp2();
					Texpr1Node node1 = getNodeFromOp(op1);
					Texpr1Node node2 = getNodeFromOp(op2);
					// Get the abstract domain of op1 and op2, and combine them down here

					Abstract1 trueBranch = branchOutWrapper.get();
					Abstract1 falseBranch = fallOutWrapper.get();

					Texpr1Node node1MinusNode2 = new Texpr1BinNode(Texpr1BinNode.OP_SUB, Texpr1BinNode.RTYPE_INT,
																   Texpr1BinNode.RDIR_ZERO, node1, node2);
					Texpr1Node node2MinusNode1 = new Texpr1BinNode(Texpr1BinNode.OP_SUB, Texpr1BinNode.RTYPE_INT,
																   Texpr1BinNode.RDIR_ZERO, node2, node1);
					Tcons1 equal = new Tcons1(env, Tcons1.EQ, node1MinusNode2); // op1 - op2 == 0
					Tcons1 greater_equal = new Tcons1(env, Tcons1.SUPEQ, node1MinusNode2); // op1 - op2 >= 0
					Tcons1 greater_than = new Tcons1(env, Tcons1.SUP, node1MinusNode2); // op1 - op2 > 0
					Tcons1 less_equal = new Tcons1(env, Tcons1.SUPEQ, node2MinusNode1); // op1 - op2 <= 0, i.e. op2 - op1 >= 0
					Tcons1 less_than = new Tcons1(env, Tcons1.SUP, node2MinusNode1); // op1 - op2 < 0, i.e. op2 - op1 > 0

					if (conditionExpr instanceof JEqExpr) { // ==
						trueBranch = trueBranch.meetCopy(man, equal);

						// etc and (d < 0 or d > 0) == (etc and d<0) or (etc and d>0)
						Abstract1 falseBranchGT = falseBranch.meetCopy(man, greater_than);
						Abstract1 falseBranchLT = falseBranch.meetCopy(man, less_than);
						falseBranch = falseBranchGT.joinCopy(man, falseBranchLT);

					} else if (conditionExpr instanceof JGeExpr) { // >=
						trueBranch = trueBranch.meetCopy(man, greater_equal);
						falseBranch = falseBranch.meetCopy(man, less_than);

					} else if (conditionExpr instanceof JGtExpr) { // >
						trueBranch = trueBranch.meetCopy(man, greater_than);
						falseBranch = falseBranch.meetCopy(man, less_equal);

					} else if (conditionExpr instanceof JLeExpr) { // <=
						trueBranch = trueBranch.meetCopy(man, less_equal);
						falseBranch = falseBranch.meetCopy(man, greater_than);

					} else if (conditionExpr instanceof JLtExpr) { // <
						trueBranch = trueBranch.meetCopy(man, less_than);
						falseBranch = falseBranch.meetCopy(man, greater_equal);

					} else if (conditionExpr instanceof JNeExpr) { // !=, i.e. > or <
						falseBranch = falseBranch.meetCopy(man, equal);

						Abstract1 trueBranchGT = trueBranch.meetCopy(man, greater_than);
						Abstract1 trueBranchLT = trueBranch.meetCopy(man, less_than);
						trueBranch = trueBranchGT.joinCopy(man, trueBranchLT);

					} else {
						// sanity check
						throw new RuntimeException("VIOLATION");
					}

					branchOutWrapper.set(trueBranch);
					fallOutWrapper.set(falseBranch);

				} else {
					unhandled("Unhandled condition type", jIfStmt, true);
				}

				logger.debug(jIfStmt.getCondition().toString());

			} else if (s instanceof JInvokeStmt) {
				// handle invocations
				JInvokeStmt jInvStmt = (JInvokeStmt) s;
				InvokeExpr invokeExpr = jInvStmt.getInvokeExpr();
				if (invokeExpr instanceof JVirtualInvokeExpr) {
					handleInvoke(jInvStmt, fallOutWrapper);
				} else if (invokeExpr instanceof JSpecialInvokeExpr) {
					// initializer for object
					handleInitialize(jInvStmt, fallOutWrapper);
				} else {
					unhandled("Unhandled invoke statement", invokeExpr, true);
				}
			} else if (s instanceof JGotoStmt) {
				// safe to ignore
			} else if (s instanceof JReturnVoidStmt) {
				// safe to ignore
			} else {
				unhandled("Unhandled statement", s, true);
			}

			// log outcome
			if (fallOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[fallout] " + fallOutWrapper);
			}
			if (branchOutWrapper != null) {
				logger.debug(inWrapper.get() + " " + s + " =>[branchout] " + branchOutWrapper);
			}

		} catch (ApronException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleInvoke(JInvokeStmt jInvStmt, NumericalStateWrapper fallOutWrapper) throws ApronException {
		// TODO: MAYBE FILL THIS OUT
		if (this.property == VerificationProperty.OVERALL_PROFIT) {
			// TODO: MAYBE FILL THIS OUT

			// Frog.total_profit += (price - this.production_cost);
			// min(total_profit) = min(price) - max(production_cost)

			Abstract1 abs = fallOutWrapper.get();

			JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) jInvStmt.getInvokeExpr();
			Local base = (Local) invokeExpr.getBase();
			List<FrogInitializer> initializers = pointsTo.pointsTo(base);
			int max_cost = Integer.MIN_VALUE;
			for (FrogInitializer initializer: initializers) {
				max_cost = Math.max(max_cost, initializer.argument);
			}
			Texpr1Node costNode = new Texpr1CstNode(new MpqScalar(max_cost));

			Value arg = invokeExpr.getArg(0);
			Texpr1Node argNode = getNodeFromOp(arg);

			Texpr1Node argSubCost = new Texpr1BinNode(Texpr1BinNode.OP_SUB, Texpr1BinNode.RTYPE_INT,
				Texpr1BinNode.RDIR_ZERO, argNode, costNode);
			Texpr1Node totalNode = new Texpr1VarNode("FROG_OVERALL_PROFIT");
			Texpr1Node totalPlusArgSubCost = new Texpr1BinNode(Texpr1BinNode.OP_ADD, Texpr1BinNode.RTYPE_INT,
				Texpr1BinNode.RDIR_ZERO, totalNode, argSubCost);
			Texpr1Intern intern = new Texpr1Intern(env, totalPlusArgSubCost);
			
			abs = abs.assignCopy(man, "FROG_OVERALL_PROFIT", intern, null);
			fallOutWrapper.set(abs);
		}
	}

	public void handleInitialize(JInvokeStmt jInvStmt, NumericalStateWrapper fallOutWrapper) throws ApronException {
		// TODO: MAYBE FILL THIS OUT
	}

	// returns state of in after assignment
	/**
	 * Definition Statement: here, you only need to handle integer assignments to a
	 * local variable.
	 * That is, x = y, or x = 5 or x = EXPR, where EXPR is one of the three binary
	 * expressions below.
	 * That is, you need to be able to handle: y = x + 5 or y = x * z.
	 */
	private void handleDef(NumericalStateWrapper outWrapper, Value left, Value right) throws ApronException {
		// TODO: FILL THIS OUT
		// assumption: left != right
		if (left == right) {
			logger.debug("left == right!");
			return;
		}

		// Extracting the abstr
		Abstract1 abstr = outWrapper.get();
		logger.debug("Initial map: " + abstr.toString());

		// process left
		String varNameLeft;
		if (left instanceof JimpleLocal) {
			varNameLeft = ((JimpleLocal) left).getName();
		} else {
			throw new RuntimeException("left is not a Local!");
		}

		// process right
		if (right instanceof BinopExpr) {
			BinopExpr binopExpr = (BinopExpr) right;
			Value op1 = binopExpr.getOp1();
			Value op2 = binopExpr.getOp2();

			if (right instanceof JMulExpr && op1 instanceof JimpleLocal && op2 instanceof JimpleLocal) {
				// first approximation: if op1 and op2 are both bounded, or if op takes one
				// value
				String opName1 = ((JimpleLocal) op1).getName();
				String opName2 = ((JimpleLocal) op2).getName();

				// assumption: you may ignore overflows in your implementation

				// https://en.wikipedia.org/wiki/Interval_arithmetic
				// [x₁, x₂] · [y₁, y₂] = [min{x₁y₁, x₁y₂, x₂y₁, x₂y₂}, max{x₁y₁, x₁y₂, x₂y₁, x₂y₂}]

				Interval int1 = abstr.getBound(man, opName1);
				Interval int2 = abstr.getBound(man, opName2);

				// forget nodeLeft (set it to [-infty, infty])
				abstr = abstr.forgetCopy(man, varNameLeft, false);

				Interval int3 = new Interval();
				if (int1.isBottom() || int2.isBottom()) {
					int3.setBottom();
				} else if (int1.isTop() || int2.isTop()) {
					int3.setTop();
				} else if (int1.isScalar() || int2.isScalar()) {
					int3 = null;

				    Scalar scalar;
					String varName;
					if (int1.isScalar()) {
						scalar = int1.inf();
						varName = opName2;
					} else {
						scalar = int2.inf();
						varName = opName1;
					}
					Texpr1Node varNode = new Texpr1VarNode(varName);
					Texpr1Node scalarNode = new Texpr1CstNode(scalar);
					Texpr1Node varTimesScalar = new Texpr1BinNode(Texpr1BinNode.OP_MUL, Texpr1BinNode.RTYPE_INT,
													Texpr1BinNode.RDIR_ZERO, varNode, scalarNode);
					Texpr1Intern intern = new Texpr1Intern(env, varTimesScalar);
					abstr = abstr.assignCopy(man, varNameLeft, intern, null);
					
				} else {
					int3 = MultiplyIntervals(int1, int2);
				}

				if (int3 != null) {
					String[] vars = {varNameLeft};
					Interval[] box = {int3};
					Abstract1 leftAbstract = new Abstract1(man, env, vars, box);
					abstr = abstr.meetCopy(man, leftAbstract);
				}

			} else {

				Texpr1Node node1 = getNodeFromOp(op1);
				Texpr1Node node2 = getNodeFromOp(op2);
				Texpr1Node nodeRight = null;

				if (right instanceof JMulExpr) { // const * local, or const * const (which won't happen)
					nodeRight = new Texpr1BinNode(Texpr1BinNode.OP_MUL, Texpr1BinNode.RTYPE_INT,
							Texpr1BinNode.RDIR_ZERO, node1, node2);
				} else if (right instanceof JSubExpr) {
					nodeRight = new Texpr1BinNode(Texpr1BinNode.OP_SUB, Texpr1BinNode.RTYPE_INT,
							Texpr1BinNode.RDIR_ZERO, node1, node2);
				} else if (right instanceof JAddExpr) {
					nodeRight = new Texpr1BinNode(Texpr1BinNode.OP_ADD, Texpr1BinNode.RTYPE_INT,
							Texpr1BinNode.RDIR_ZERO, node1, node2);
				} else {
					unhandled("Unhandled binary operation", right, true);
				}

				// forget nodeLeft
				abstr = abstr.forgetCopy(man, varNameLeft, false);

				// assign new value
				Texpr1Intern internRight = new Texpr1Intern(env, nodeRight);
				abstr = abstr.assignCopy(man, varNameLeft, internRight, null);

				// logger.debug(node1.toString() + " " + node2.toString() + " => " + nodeRight);
				// logger.debug(internRight.toString());

			}

		} else if (right instanceof ParameterRef) { // e.g. i3 := @parameter0: int => ?
			// TODO: how do you handle parameter?
			// Nothing really needed to do for parameters
		} else if (right instanceof IntConstant || right instanceof JimpleLocal) {

			Texpr1Node nodeRight = getNodeFromOp(right);
			Texpr1Intern internRight = new Texpr1Intern(env, nodeRight);
			abstr = abstr.forgetCopy(man, varNameLeft, false);
			abstr = abstr.assignCopy(man, varNameLeft, internRight, null);

		} else if (right instanceof JNegExpr) { // not necessary
			unhandled("Unhandled unary negate operation", right, true);
		} else {
			unhandled("Unhandled define operation", right, true);
		}

		// set the abstr
		outWrapper.set(abstr);
		logger.debug("Final map: " + abstr.toString());
	}

	// TODO: MAYBE FILL THIS OUT: add convenience methods
	private Texpr1Node getNodeFromOp(Value op) {
		Texpr1Node node;
		if (op instanceof JimpleLocal) {
			node = new Texpr1VarNode(((JimpleLocal) op).getName());
		} else if (op instanceof IntConstant) {
			node = new Texpr1CstNode(new MpqScalar(((IntConstant) op).value));
		} else {
			throw new RuntimeException(op.toString() + " is not a Local or IntConstant!");
		}
		return node;
	}
	
	private Abstract1 widenFixed(Abstract1 oldState, Abstract1 newState) throws ApronException {
        Abstract1 joined = newState.joinCopy(man, oldState);
        Abstract1 widened = oldState.widening(man, joined);
        return widened;
    }

	private Interval MultiplyIntervals(Interval a, Interval b) {
        // [x₁, x₂] · [y₁, y₂] = [min{x₁y₁, x₁y₂, x₂y₁, x₂y₂}, max{x₁y₁, x₁y₂, x₂y₁, x₂y₂}]
        
        // Calculate the four products
        Scalar prod1 = MultiplyScalars(a.inf(), b.inf());
        Scalar prod2 = MultiplyScalars(a.sup(), b.inf());
        Scalar prod3 = MultiplyScalars(a.inf(), b.sup());
        Scalar prod4 = MultiplyScalars(a.sup(), b.sup());
        
        // Determine the minimum and maximum of the products
        Scalar min = prod1;
        if (prod2.cmp(min) < 0) min = prod2;
        if (prod3.cmp(min) < 0) min = prod3;
        if (prod4.cmp(min) < 0) min = prod4;
        
        Scalar max = prod1;
        if (prod2.cmp(max) > 0) max = prod2;
        if (prod3.cmp(max) > 0) max = prod3;
        if (prod4.cmp(max) > 0) max = prod4;
        
        // Return the resulting interval
        return new Interval(min, max);
    }

    private Scalar MultiplyScalars(Scalar a, Scalar b) {
        Scalar temp = new MpqScalar();
        if (a.isInfty() * b.isInfty() != 0) { // i.e. a and b are both infinity
            temp.setInfty(a.isInfty() * b.isInfty());
        } else if (a.isZero() || b.isZero()) { // anything times 0 is 0, no need to think of limits ;D
            temp.set(0);
        } else if (a.isInfty() != 0 || b.isInfty() != 0) {
            temp.setInfty(a.sgn() * b.sgn());
        } else { // are finite
            Mpq a_mpq = new Mpq();
            ((MpqScalar) a).toMpq(a_mpq, 0);
            Mpq b_mpq = new Mpq();
            ((MpqScalar) b).toMpq(b_mpq, 0);
            a_mpq.mul(b_mpq);
            temp = new MpqScalar(a_mpq);
        }
        return temp;
    }

	// for debugging
	private HashMap<Unit, NumericalStateWrapper> unitState = new HashMap<Unit, NumericalStateWrapper>();

}
