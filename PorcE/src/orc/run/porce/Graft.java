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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

@Introspectable
public abstract class Graft extends Expression {
    protected static final boolean TRUE = true;
  
    protected final PorcEExecution execution;
    
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
		    && computation.getTimePerCall() < SpecializationConfiguration.InlineAverageTimeLimit)
		    || !r.isWorkQueueUnderful(r.minQueueSize() + 2);
	} catch (UnexpectedResultException e) {
	    throw InternalPorcEError.typeError(this, e);
	}
    }

    protected final Object compSlotID = new Object();
    protected final Object futSlotID = new Object();

    protected FrameSlot createFrameSlot(final VirtualFrame frame, Object id) {
	FrameDescriptor descriptor = frame.getFrameDescriptor();
	return descriptor.findOrAddFrameSlot(id, FrameSlotKind.Object);
    }

    private final BranchProfile haltCatchProfile = BranchProfile.create();
    private final BranchProfile killCatchProfile = BranchProfile.create();

    public FullFutureNodes createFullFutureNodes(FrameSlot futSlot, FrameSlot compSlot) {
	return new FullFutureNodes(futSlot, compSlot);
    }

    protected class FullFutureNodes extends Node {
        // FIXME: This will not work in distributed Orc because it creates new rootnodes/calltargets at runtime.
        //   To make this work with distrib there will need to be a way to transmit a descriptor of the target
        //   so it can be built when it is recieved, OR these closures need to exist at PorcE start time.
        //   The latter is probably easier and better. The targets can be created during porc to porce conversion
        //   and then just used when needed.
      
	@Child
	NewContinuation compClosureNode;
	@Child
	NewToken newToken;
	@Child
	Spawn spawn;
	@Child
	HaltToken haltToken;
	@Child
	Expression callP;

	protected FullFutureNodes(FrameSlot futSlot, FrameSlot compSlot) {
	    newToken = NewToken.create((Expression) c.copy());
	    spawn = Spawn.create((Expression) c.copy(), (Expression) t.copy(), false, Read.Local.create(compSlot),
		    execution);
	    haltToken = HaltToken.create((Expression) c.copy(), execution);
	    compClosureNode = NewContinuation.create(
		    new Expression[] { (Expression) c.copy(), (Expression) v.copy(), Read.Local.create(futSlot) },
		    createComp(), false);
	    callP = Call.CPS.create((Expression) p.copy(), new Expression[] { Read.Local.create(futSlot) }, execution,
		    isTail);
	}

	class ProfilingPCallNode extends Expression {
	    @Child
	    Expression readTarget = Read.Closure.create(1);

	    protected final ValueProfile targetProfile = ValueProfile.createIdentityProfile();

	    @Child
	    Expression call;

	    protected ProfilingPCallNode(Expression newP, Expression newC) {
		call = Call.CPS.create(Read.Closure.create(1), new Expression[] { newP, newC }, execution, false);
	    }

	    private boolean shouldTimeRoot(PorcERootNode root) {
		return root != null && root.shouldTimeCall();
	    }

	    @Override
	    public void executePorcEUnit(final VirtualFrame frame) {
		PorcERootNode root = targetProfile.profile((PorcERootNode) ((PorcEClosure) readTarget.execute(frame)).body.getRootNode());
		long startTime = 0;
		if (shouldTimeRoot(root))
		    startTime = System.nanoTime();
		try {
		    call.execute(frame);
		} finally {
		    if (shouldTimeRoot(root) && startTime > 0) {
			root.addSpawnedCall(System.nanoTime() - startTime);
		    }
		}
	    }
	}

	protected RootNode createComp() {
	    Expression cr = NewContinuation.create(new Expression[] { Read.Closure.create(0), Read.Closure.create(2) }, createCR(), false);
	    Expression newP = NewContinuation.create(new Expression[] { Read.Closure.create(0), Read.Closure.create(2) }, createNewP(), false);
	    Expression newC = NewCounter.Simple.create(execution, Read.Closure.create(0), cr);

	    Expression body = new ProfilingPCallNode(newP, newC);
	    PorcERootNode r = PorcERootNode.create(Graft.this.getRootNode().getLanguage(PorcELanguage.class), null, body, 0, 3, execution);
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
	    PorcERootNode r = PorcERootNode.create(Graft.this.getRootNode().getLanguage(PorcELanguage.class), null, body, 0, 2, execution);
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
	    PorcERootNode r = PorcERootNode.create(Graft.this.getRootNode().getLanguage(PorcELanguage.class), null,
		    body, 1, 2, execution);
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

    protected Expression createCallV() {
	return Call.CPS.create((Expression) v.copy(), new Expression[] { (Expression) p.copy(), (Expression) c.copy() }, execution, isTail);
    }
    
    @Specialization(guards = { "shouldInlineSpawn(frame)" }, replaces = { "fullFuture" })
    public PorcEUnit noFuture(final VirtualFrame frame, 
          @Cached("createCallV()") Expression callV) {
        callV.execute(frame);
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

    public static Graft create(PorcEExecution execution, Expression p, Expression c, Expression t, Expression v) {
        return GraftNodeGen.create(execution, p, c, t, v);
    }
}
