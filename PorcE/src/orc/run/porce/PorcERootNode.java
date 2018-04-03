//
// PorcERootNode.java -- Truffle root node PorcERootNode
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import scala.Option;

import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.HaltException;
import orc.run.porce.call.CatchSelfTailCall;
import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.SourceSectionFromPorc;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.RootCallTarget;

public class PorcERootNode extends RootNode implements HasPorcNode, HasId {
    
    // TODO: PERFORMANCE: All these counters should probably just be volatile and let the accesses be racy (like the JVM does for call counters).
    // The challenge of using racy counters is to make sure that the values in them are never invalid to the point of breaking the semantics.
    private final AtomicLong totalSpawnedTime = new AtomicLong(0);
    private final AtomicLong totalSpawnedCalls = new AtomicLong(0);

    /*
     *  The solution for a racy version is to use a volatile long and split it into two "fields" one for each value (as an int).
     *  Writes to volatile longs are atomic and the voltatile read/writes shouldn't be much more expensive than normal read/write
     *  since they will not happen often enough to be combined.
    private volatile long both = 0;
    private final int getTotalSpawnedTime(long both) {
	return (int) (both & 0xffffffff);
    }
    private final int getTotalSpawnedCalls(long both) {
	return (int) (both >> 32);
    }
    */
    
    public final long getTotalSpawnedTime() {
	return totalSpawnedTime.get();
    }
    public final long getTotalSpawnedCalls() {
	return totalSpawnedCalls.get();
    }
    
    @CompilationFinal
    private boolean internal = false;
    
    @CompilationFinal
    private long timePerCall = -1;
   
    final public void addSpawnedCall(long time) {
        totalSpawnedTime.getAndAdd(time);
        totalSpawnedCalls.getAndIncrement();        
    }
    
    final public boolean shouldTimeCall() {
    	return timePerCall < 0 || CompilerDirectives.inInterpreter();
    }
    
    final public long getTimePerCall() {
	//CompilerAsserts.compilationConstant(this);
    	if (shouldTimeCall()) {
        	long n = totalSpawnedCalls.get();
            	long t = totalSpawnedTime.get();
    		
    		if (n >= SpecializationConfiguration.MinCallsForTimePerCall) {
        		CompilerDirectives.transferToInterpreterAndInvalidate();
        		timePerCall = t / n;
    		} else if (n < 2) {
                	return Long.MAX_VALUE;
    		} else {
                	return t / n;
    		}
    	}
		return timePerCall;
    }

    /* ROOTNODE-STATISTICS
    private final AtomicLong totalSpawns = new AtomicLong(0);
    private final AtomicLong totalBindSingle = new AtomicLong(0);
    private final AtomicLong totalBindJoin = new AtomicLong(0);
    private final AtomicLong totalHalt = new AtomicLong(0);
    private final AtomicLong totalPublication = new AtomicLong(0);

    final public void incrementSpawn() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalSpawns.getAndIncrement();
    	}
    } 

    final public void incrementHalt() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalHalt.getAndIncrement();
    	}
    }

    final public void incrementPublication() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalPublication.getAndIncrement();
    	}
    }

    final public void incrementBindSingle() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalBindSingle.getAndIncrement();
    	}
    }

    final public void incrementBindJoin() {
    	if (CompilerDirectives.inInterpreter()) {
    		totalBindJoin.getAndIncrement();
    	}
    }

	public scala.Tuple7<Long, Long, Long, Long, Long, Long, Long> getCollectedCallInformation() {
		return new scala.Tuple7<>(
				totalSpawns.get(), totalBindSingle.get(), totalBindJoin.get(), totalHalt.get(), totalPublication.get(),
				totalSpawnedTime.get(), totalSpawnedCalls.get()        
				);
    }
    */

    private Option<PorcAST> porcNode = Option.apply(null);

	public void setPorcAST(final PorcAST ast) {
		CompilerAsserts.neverPartOfCompilation();
		porcNode = Option.apply(ast);
		section = SourceSectionFromPorc.apply(ast);
		internal = !(ast instanceof orc.ast.porc.Method);
	}
    
    @Override
    public boolean isInternal() {
      return internal;
    }

    @Override
    public Option<PorcAST> porcNode() {
        return porcNode;
    }
    
    @CompilationFinal
    private SourceSection section = null;

    @Override
    public SourceSection getSourceSection() {
        return section;
    }
    
    @Override
    public String getName() {
        String name = "<no AST>";
        scala.Option<PorcAST> optAst = porcNode();
        if (optAst.isDefined()) {
            final PorcAST ast = optAst.get();
            name = "<N/A>";
            if (ast instanceof orc.ast.hasOptionalVariableName) {
                scala.Option<String> optName = ((orc.ast.hasOptionalVariableName) ast).optionalVariableName();
                if (optName.isDefined()) {
                    name = optName.get();
                } else {
                    name = "<unset>";
                }
            }
        }
        return name;
    }

    @Override
    public int getId() {
        return ((Integer) ((ASTWithIndex) porcNode().get()).optionalIndex().get()).intValue();
    }

    protected @Child Expression body;
    private final int nArguments;
    private final int nCaptured;
    
    @CompilationFinal
    private RootCallTarget trampolineCallTarget;

    public RootCallTarget getTrampolineCallTarget() {
	if (trampolineCallTarget == null) {
	    CompilerDirectives.transferToInterpreterAndInvalidate();
	    atomic(() -> {
		if (trampolineCallTarget == null) {
		    RootCallTarget v = Truffle.getRuntime().createCallTarget(new InvokeWithTrampolineRootNode(getLanguage(PorcELanguage.class), this, execution));
		    // TODO: Use the new Java 9 fence when we start requiring Java 9
		    // for PorcE.
		    NodeBase.UNSAFE.fullFence();
		    trampolineCallTarget = v;
		}
	    });
	}
	return trampolineCallTarget;
    }

    private final PorcEExecution execution;

    public PorcERootNode(final PorcELanguage language, final FrameDescriptor descriptor, final Expression body, final int nArguments, final int nCaptured, PorcEExecution execution) {
        super(language, descriptor);
        this.body = body;
        this.nArguments = nArguments;
        this.nCaptured = nCaptured;
	this.execution = execution;
    }
    
    public Expression getBody() {
    	if (body instanceof CatchSelfTailCall) {
    		return ((CatchSelfTailCall) body).getBody();
    	} else {
    		return body;
    	}
    }

    @Override
    public Object execute(final VirtualFrame frame) {
	if (!SpecializationConfiguration.UniversalTCO) {
            final Object[] arguments = frame.getArguments();
            if (arguments.length != nArguments + 1) {
                transferToInterpreter();
                throwArityException(arguments.length - 1, nArguments);
            }
            final Object[] captureds = (Object[]) arguments[0];
            if (captureds.length != nCaptured) {
                transferToInterpreter();
                InternalPorcEError.capturedLengthError(nCaptured, captureds.length);
            }
	}

        try {
            final Object ret = body.execute(frame);
            return ret;
        } catch (KilledException | HaltException e) {
            transferToInterpreter();
            Logger.log(Level.SEVERE, () -> "Caught " + e + " in root node " + this, e);
            return PorcEUnit.SINGLETON;
        } catch (ControlFlowException e) {
            throw e;
        } catch (Exception e) {
            transferToInterpreter();
            execution.notifyOfException(e, this);
            return PorcEUnit.SINGLETON;
        }
    }

    @TruffleBoundary
    private static void throwArityException(final int nReceived, final int nExpected) {
        throw new ArityMismatchException(nExpected, nReceived);
    }

    public static PorcERootNode create(final PorcELanguage language, final FrameDescriptor descriptor, final Expression body, final int nArguments, final int nCaptured, PorcEExecution execution) {
    	// Add self tail call catcher to the body during construction.
        PorcERootNode r = new PorcERootNode(language, descriptor, CatchSelfTailCall.create(body), nArguments, nCaptured, execution);
        Truffle.getRuntime().createCallTarget(r);
        return r;
    }

    @Override
    public String toString() {
        return String.format("PorcE.%s", getName());
    }
}
