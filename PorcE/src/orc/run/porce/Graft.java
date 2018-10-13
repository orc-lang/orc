//
// Graft.java -- Truffle node Graft
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import java.util.concurrent.atomic.AtomicBoolean;

import orc.ast.porc.Variable;
import orc.compiler.porce.PorcToPorcE;
import orc.error.runtime.HaltException;
import orc.run.porce.call.Call;
import orc.run.porce.runtime.Future;
import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.PorcERuntime$;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

@Introspectable
public abstract class Graft extends Expression {
    protected static final boolean TRUE = true;

    protected final PorcEExecution execution;

    protected final ValueProfile targetRoot = ValueProfile.createIdentityProfile();

    @Child
    protected Expression p;
    @Child
    protected Expression c;
    @Child
    protected Expression t;
    @Child
    protected Expression v;

    protected Graft(PorcEExecution execution, Expression p, Expression c, Expression t, Expression v) {
	this.execution = execution;
	this.p = p;
	this.c = c;
	this.t = t;
	this.v = v;
    }

    private static final boolean allowSpawnInlining = PorcERuntime$.MODULE$.allowSpawnInlining();

    protected boolean shouldInlineSpawn(VirtualFrame frame) {
	try {
	    final PorcERuntime r = execution.runtime();
	    PorcEClosure computation = v.executePorcEClosure(frame);
	    return (allowSpawnInlining
		    && computation.getTimePerCall(targetRoot) < SpecializationConfiguration.InlineAverageTimeLimit)
		    || !r.isWorkQueueUnderful(r.minQueueSize() + 2);
	} catch (UnexpectedResultException e) {
	    throw InternalPorcEError.typeError(this, e);
	}
    }

    protected final Variable compSlotID = new Variable("comp");
    protected final Variable futSlotID = new Variable("fut");
    protected final Variable newCSlotID = new Variable("newC");

    protected FrameSlot createFrameSlot(final VirtualFrame frame, Object id) {
	FrameDescriptor descriptor = frame.getFrameDescriptor();
	return descriptor.findOrAddFrameSlot(id, FrameSlotKind.Object);
    }

    private final BranchProfile haltCatchProfile = BranchProfile.create();
    private final BranchProfile killCatchProfile = BranchProfile.create();

    // FIXME: None of this will work in distributed Orc because it creates new rootnodes/calltargets at runtime.
    //   To make this work with distrib there will need to be a way to transmit a descriptor of the target
    //   so it can be built when it is recieved, OR these closures need to exist at PorcE start time.
    //   The latter is probably easier and better. The targets can be created during porc to porce conversion
    //   and then just used when needed.

    protected class ProfilingCallNode extends Expression {
	@Child
	protected Expression target;

	protected final ValueProfile targetProfile = ValueProfile.createIdentityProfile();

	@Child
	protected Expression call;

        @Override
        public void setTail(boolean v) {
            super.setTail(v);
            call.setTail(v);
        }

	protected ProfilingCallNode(Expression target, Expression... args) {
	    this.target = target;
	    call = Call.CPS.create((Expression) target.copy(), args, execution, false);
	}

	private boolean shouldTimeRoot(PorcERootNode root) {
	    return root != null && root.shouldTimeCall();
	}

	@Override
	public void executePorcEUnit(final VirtualFrame frame) {
	    PorcERootNode root = targetProfile.profile((PorcERootNode) ((PorcEClosure) target.execute(frame)).body.getRootNode());
	    long startTime = 0;
	    if (shouldTimeRoot(root)) {
            startTime = System.nanoTime();
        }
	    try {
		call.execute(frame);
	    } finally {
		if (shouldTimeRoot(root) && startTime > 0) {
		    root.addSpawnedCall(System.nanoTime() - startTime);
		}
	    }
	}
    }

    public FullFutureNodes createFullFutureNodes(FrameSlot futSlot, FrameSlot compSlot) {
	return new FullFutureNodes(futSlot, compSlot);
    }

    protected class FullFutureNodes extends NodeBase {
	@Child
	protected NewContinuation compClosureNode;
	@Child
	protected NewToken newToken;
	@Child
	protected Spawn spawn;
	@Child
	protected HaltToken haltToken;
	@Child
	protected Expression callP;

	protected FullFutureNodes(FrameSlot futSlot, FrameSlot compSlot) {
	    newToken = NewToken.create((Expression) c.copy(), execution);
	    spawn = Spawn.create((Expression) c.copy(), (Expression) t.copy(), false, Read.Local.create(compSlot),
		    execution);
	    haltToken = HaltToken.create((Expression) c.copy(), execution);
	    compClosureNode = NewContinuation.create(
		    new Expression[] { (Expression) c.copy(), (Expression) v.copy(), Read.Local.create(futSlot) },
		    createComp(), false);
	    callP = Call.CPS.create((Expression) p.copy(), new Expression[] { Read.Local.create(futSlot) }, execution,
		    isTail);
	}

        @Override
        public void setTail(boolean v) {
            super.setTail(v);
            callP.setTail(v);
        }

	protected RootNode createComp() {
	    FrameDescriptor descript = new FrameDescriptor();
	    FrameSlot newCSlot = descript.findOrAddFrameSlot(newCSlotID, FrameSlotKind.Object);

	    Expression cr = NewContinuation.create(new Expression[] { Read.Closure.create(0), Read.Closure.create(2) }, createCR(), false);
	    Expression newC = NewCounter.Simple.create(execution, Read.Closure.create(0), cr);

	    Expression newP = NewContinuation.create(new Expression[] { Read.Local.create(newCSlot), Read.Closure.create(2) }, createNewP(), false);

	    Expression body = Sequence.create(new Expression[] {
		    Write.Local.create(newCSlot, newC),
		    new ProfilingCallNode(Read.Closure.create(1), newP, Read.Local.create(newCSlot))
	    });
            body.setTail(true);
	    PorcERootNode r = PorcERootNode.create(getPorcERootNode().getLanguage(PorcELanguage.class), descript, body, 0, 3,
	            getPorcERootNode().getMethodKey(), execution);
            r.setVariables(PorcToPorcE.variableSeq(),
                    PorcToPorcE.variableSeq(new Variable("C"), new Variable("target"), new Variable("fut")));
	    if (porcNode().isDefined()) {
		r.setPorcAST(porcNode().get());
	    }
	    execution.registerRootNode(r);
	    return r;
	}

	protected RootNode createCR() {
	    Expression body = Sequence.create(new Expression[] {
		    BindStop.create(Read.Closure.create(1)),
		    HaltToken.create(Read.Closure.create(0), execution) });
	    body.setTail(true);
	    PorcERootNode r = PorcERootNode.create(getPorcERootNode().getLanguage(PorcELanguage.class), null, body, 0, 2,
	            getPorcERootNode().getMethodKey(), execution);
	    r.setVariables(PorcToPorcE.variableSeq(),
	            PorcToPorcE.variableSeq(new Variable("C"), new Variable("fut")));
	    if (porcNode().isDefined()) {
		r.setPorcAST(porcNode().get());
	    }
	    execution.registerRootNode(r);
	    return r;
	}

	protected RootNode createNewP() {
	    Expression body = Sequence.create(new Expression[] {
		    Bind.create(Read.Closure.create(1), Read.Argument.create(0), execution),
		    HaltToken.create(Read.Closure.create(0), execution) });
            body.setTail(true);
	    PorcERootNode r = PorcERootNode.create(getPorcERootNode().getLanguage(PorcELanguage.class), null, body, 1, 2,
	            getPorcERootNode().getMethodKey(), execution);
            r.setVariables(PorcToPorcE.variableSeq(new Variable("v")),
                    PorcToPorcE.variableSeq(new Variable("C"), new Variable("fut")));
	    if (porcNode().isDefined()) {
		r.setPorcAST(porcNode().get());
	    }
	    execution.registerRootNode(r);
	    return r;
	}
    }

    @Specialization(guards = { "!shouldInlineSpawn(frame)", "TRUE" })
    public PorcEUnit fullFuture(final VirtualFrame frame,
        @Cached("createFrameSlot(frame, compSlotID)") FrameSlot compSlot,
        @Cached("createFrameSlot(frame, futSlotID)") FrameSlot futSlot,
        @Cached("createFullFutureNodes(futSlot, compSlot)") FullFutureNodes nodes) {
        ensureTail(nodes);
        frame.setObject(futSlot, new Future(false));
        frame.setObject(compSlot, nodes.compClosureNode.execute(frame));
        nodes.newToken.executePorcEUnit(frame);
        try {
            nodes.spawn.executePorcEUnit(frame);
        } catch (HaltException e) {
            haltCatchProfile.enter();
            nodes.haltToken.executePorcEUnit(frame);
        } catch (KilledException e) {
            killCatchProfile.enter();
            nodes.haltToken.executePorcEUnit(frame);
        }
        nodes.callP.executePorcEUnit(frame);
        return PorcEUnit.SINGLETON;
    }


    public NoFutureNodes createNoFutureNodes(FrameSlot futSlot, FrameSlot compSlot) {
	return new NoFutureNodes(futSlot, compSlot);
    }

    protected orc.run.porce.runtime.Future HALTED_FUTURE = new orc.run.porce.runtime.Future(true);
    {
	HALTED_FUTURE.fastLocalStop();
    }

    protected class NoFutureNodes extends Expression {
	@Child
	Expression body;

	protected NoFutureNodes(FrameSlot futSlot, FrameSlot newCSlot) {
	    body = createComp(futSlot, newCSlot);
	}

	@Override
        public void setTail(boolean v) {
	    super.setTail(v);
            body.setTail(v);
        }

	@Override
	public void executePorcEUnit(final VirtualFrame frame) {
	    body.executePorcEUnit(frame);
	}

	protected Expression createComp(FrameSlot futSlot, FrameSlot newCSlot) {
	    Expression cr = NewContinuation.create(
		    new Expression[] { (Expression) c.copy(), Read.Local.create(futSlot), (Expression) p.copy() },
		    createCR(), false);
	    Expression newC = NewCounter.Simple.create(execution, (Expression) c.copy(), cr);

	    Expression newP = NewContinuation.create(
		    new Expression[] { Read.Local.create(newCSlot), Read.Local.create(futSlot), (Expression) p.copy() },
		    createNewP(), false);

	    Expression callV = new ProfilingCallNode((Expression) v.copy(), newP, Read.Local.create(newCSlot));
	    Expression haltToken = HaltToken.create(Read.Local.create(newCSlot), execution);

	    return Sequence.create(new Expression[] {
		    NewToken.create((Expression) c.copy(), execution),
		    Write.Local.create(newCSlot, newC),
		    TryOnException.create(callV, haltToken)
	    });
	}

	protected RootNode createCR() {
	    Expression crBody = Sequence.create(new Expression[] {
		    new Expression() {
			@Child
			protected Expression readFlag = Read.Closure.create(1);
			@Child
			protected Expression callP = new ProfilingCallNode(Read.Closure.create(2), Read.Constant.create(HALTED_FUTURE));

			protected final BranchProfile isFirst = BranchProfile.create();

			@Override
			public void executePorcEUnit(final VirtualFrame frame) {
			    AtomicBoolean flag = (AtomicBoolean) readFlag.execute(frame);
			    if (flag.compareAndSet(false, true)) {
				isFirst.enter();
				callP.executePorcEUnit(frame);
			    }
			}
		    },
		    HaltToken.create(Read.Closure.create(0), execution) });
	    crBody.setTail(true);
	    PorcERootNode r = PorcERootNode.create(getPorcERootNode().getLanguage(PorcELanguage.class), null, crBody, 0, 3,
	            getPorcERootNode().getMethodKey(), execution);
            r.setVariables(PorcToPorcE.variableSeq(),
                    PorcToPorcE.variableSeq(new Variable("C"), new Variable("flag"), new Variable("target")));
	    if (porcNode().isDefined()) {
		r.setPorcAST(porcNode().get());
	    }
	    execution.registerRootNode(r);
	    return r;
	}

	protected RootNode createNewP() {
	    Expression pBody = Sequence.create(new Expression[] {
		    new Expression() {
			@Child
			protected Expression readFlag = Read.Closure.create(1);
			@Child
			protected Expression callP = new ProfilingCallNode(Read.Closure.create(2), Read.Argument.create(0));

			protected final BranchProfile isFirst = BranchProfile.create();

			@Override
			public void executePorcEUnit(final VirtualFrame frame) {
			    AtomicBoolean flag = (AtomicBoolean) readFlag.execute(frame);
			    if (flag.compareAndSet(false, true)) {
				isFirst.enter();
				callP.executePorcEUnit(frame);
			    }
			}
		    },
		    HaltToken.create(Read.Closure.create(0), execution) });
            pBody.setTail(true);
	    PorcERootNode r = PorcERootNode.create(getPorcERootNode().getLanguage(PorcELanguage.class), null, pBody, 1, 3,
	            getPorcERootNode().getMethodKey(), execution);
            r.setVariables(PorcToPorcE.variableSeq(new Variable("v")),
                    PorcToPorcE.variableSeq(new Variable("C"), new Variable("flag"), new Variable("target")));
	    if (porcNode().isDefined()) {
		r.setPorcAST(porcNode().get());
	    }
	    execution.registerRootNode(r);
	    return r;
	}
    }

    @Specialization(guards = { "shouldInlineSpawn(frame)" }, replaces = { "fullFuture" })
    public PorcEUnit noFuture(final VirtualFrame frame,
	    @Cached("createFrameSlot(frame, futSlotID)") FrameSlot futSlot,
	    @Cached("createFrameSlot(frame, newCSlotID)") FrameSlot newCSlot,
	    @Cached("createNoFutureNodes(futSlot, newCSlot)") NoFutureNodes nodes) {
        ensureTail(nodes);
        // Abuse the fut slot for the already-done flag. This is set on publication and on halting.
        frame.setObject(futSlot, new AtomicBoolean(false));
        nodes.executePorcEUnit(frame);
        return PorcEUnit.SINGLETON;
    }

    // This duplication of "fullFuture" allows this node to specialize to only inline and then switch back to both later by adding this specialization.
    @Specialization()
    public PorcEUnit fullAfterNoFuture(final VirtualFrame frame,
        @Cached("createFrameSlot(frame, compSlotID)") FrameSlot compSlot,
        @Cached("createFrameSlot(frame, futSlotID)") FrameSlot futSlot,
        @Cached("createFullFutureNodes(futSlot, compSlot)") FullFutureNodes nodes) {
        return fullFuture(frame, compSlot, futSlot, nodes);
    }

    protected PorcERootNode getPorcERootNode() {
        return (PorcERootNode) Graft.this.getCachedRootNode();
    }


    public static Graft create(PorcEExecution execution, Expression p, Expression c, Expression t, Expression v) {
        return GraftNodeGen.create(execution, p, c, t, v);
    }
}
