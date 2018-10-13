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
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Level;

import scala.Option;
import scala.collection.Seq;

import orc.ast.hasOptionalVariableName;
import orc.ast.ASTWithIndex;
import orc.ast.porc.PorcAST;
import orc.ast.porc.Variable;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.HaltException;
import orc.run.porce.call.CatchSelfTailCall;
import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.SourceSectionFromPorc;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.AssumedValue;

public class PorcERootNode extends RootNode implements HasPorcNode, HasId, ProfilingScope, NodeBaseInterface {

    // TODO: PERFORMANCE: Replace these with CountSumValue.
    // The challenge of using racy counters is to make sure that the values in them are never invalid to the point of breaking the semantics.
    private final AtomicLong totalSpawnedTime = new AtomicLong(0);
    private final AtomicLong totalSpawnedCalls = new AtomicLong(0);

    private final LongAdder totalTime = new LongAdder();
    private final LongAdder totalSiteCalls = new LongAdder();
    private final LongAdder totalContinuationSpawns = new LongAdder();
    private final LongAdder totalCalls = new LongAdder();

    @SuppressWarnings("boxing")
    private final AssumedValue<Boolean> isProfilingFlag = new AssumedValue<Boolean>(false);
    public static final boolean profileTime = false;

    @Override
    public ProfilingScope getProfilingScope() {
        return this;
    }

    public final long getTotalSpawnedTime() {
        return totalSpawnedTime.get();
    }

    public final long getTotalSpawnedCalls() {
        return totalSpawnedCalls.get();
    }

    @Override
    @TruffleBoundary(allowInlining = false)
    public final long getTotalTime() {
        return totalTime.sum();
    }

    @Override
    @TruffleBoundary(allowInlining = false)
    public final long getTotalCalls() {
        return totalCalls.sum();
    }

    @Override
    @TruffleBoundary(allowInlining = false)
    public long getSiteCalls() {
        return totalSiteCalls.sum();
    }

    @Override
    @TruffleBoundary(allowInlining = false)
    public long getContinuationSpawns() {
        return totalContinuationSpawns.sum();
    }

    @Override
    @SuppressWarnings("boxing")
    public final boolean isProfilingComplete() {
        return !isProfilingFlag.get();
    }

    @Override
    @SuppressWarnings("boxing")
    public final boolean isProfiling() {
        return CompilerDirectives.inCompiledCode() && isProfilingFlag.get();
    }

    @Override
    public long getTime() {
        if (profileTime && isProfiling()) {
            return System.nanoTime();
        } else {
            return 0;
        }
    }

    @TruffleBoundary(allowInlining = false)
    private static void longAdderAdd(LongAdder adder, long n) {
        adder.add(n);
    }


    @Override
    public void removeTime(long start) {
        if (profileTime && isProfiling()) {
            longAdderAdd(totalTime, -(getTime() - start));
        }
    }

    @Override
    final public void addTime(long start) {
        if (isProfiling()) {
            longAdderAdd(totalCalls, 1);
            if (profileTime) {
                longAdderAdd(totalTime, getTime() - start);
            }
        }
    }

    @Override
    final public void incrSiteCall() {
        if (isProfiling()) {
            longAdderAdd(totalSiteCalls, 1);
        }
    }

    @Override
    final public void incrContinuationSpawn() {
        if (isProfiling()) {
            longAdderAdd(totalContinuationSpawns, 1);
        }
    }


    @CompilationFinal
    private boolean internal = true;

    @CompilationFinal
    private long timePerCall = -1;

    final public void addSpawnedCall(long time) {
        if (timePerCall < 0) {
            totalSpawnedTime.getAndAdd(time);
            long v = totalSpawnedCalls.getAndIncrement();
            if (v >= SpecializationConfiguration.MinCallsForTimePerCall) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getTimePerCall();
            }
        }
    }

    final public boolean shouldTimeCall() {
    	return /*CompilerDirectives.inCompiledCode() &&*/ timePerCall < 0;
    }

    final public long getTimePerCall() {
        if (shouldTimeCall()) {
            long n = totalSpawnedCalls.get();
            long t = totalSpawnedTime.get();

            if (n >= SpecializationConfiguration.MinCallsForTimePerCall) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                timePerCall = t / n;
            } else if (n >= 5) {
                return t / n;
            }
        }

        if (timePerCall < 0) {
            return Long.MAX_VALUE;
        } else {
            return timePerCall;
        }
    }

    private Option<PorcAST> porcNode = Option.apply(null);

    public void setPorcAST(final PorcAST ast) {
        CompilerAsserts.neverPartOfCompilation();
        porcNode = Option.apply(ast);
        section = SourceSectionFromPorc.apply(ast);
        internal = !(ast instanceof orc.ast.porc.Method);
        internedName = getName().intern();
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
    private String internedName;
    @Override
    public String getContainingPorcCallableName() {
        return internedName;
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

    @Child
    protected Expression body;

    @Child
    protected FlushAllCounters flushAllCounters;

    private Seq<Variable> argumentVariables = null;
    private Seq<Variable> closureVariables = null;

    public void setVariables(Seq<Variable> argumentVariables, Seq<Variable> closureVariables) {
        assert this.argumentVariables == null;
        assert this.closureVariables == null;
        this.argumentVariables = argumentVariables;
        this.closureVariables = closureVariables;
    }

    public Seq<Variable> getArgumentVariables() {
        assert this.argumentVariables != null : this;
        return this.argumentVariables;
    }

    public Seq<Variable> getClosureVariables() {
        assert this.closureVariables != null : this;
        return this.closureVariables;
    }

    private final int nArguments;
    private final int nCaptured;

    @CompilationFinal
    private RootCallTarget trampolineCallTarget;

    public RootCallTarget getTrampolineCallTarget() {
        if (trampolineCallTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            atomic(() -> {
                if (trampolineCallTarget == null) {
                    RootCallTarget v = Truffle.getRuntime().createCallTarget(
                            new InvokeWithTrampolineRootNode(getLanguage(PorcELanguage.class), this, execution));
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
    private final Object methodKey;

    public PorcERootNode(final PorcELanguage language, final FrameDescriptor descriptor,
            final Expression body, final int nArguments, final int nCaptured, final Object methodKey,
            PorcEExecution execution) {
        super(language, descriptor);
        this.body = insert(body);
        this.nArguments = nArguments;
        this.nCaptured = nCaptured;
        this.execution = execution;
        this.methodKey = methodKey;
        this.flushAllCounters = insert(FlushAllCounters.create(-1, execution));
        this.flushAllCounters.setTail(true);
    }

    /**
     * @return An object which is unique to the Orc method containing this root node.
     *
     * The returned object is generally the Porc method name as a porc.Var.
     */
    public Object getMethodKey() {
        return methodKey;
    }

    public PorcEExecution getExecution() {
        return execution;
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
                throw new ArityMismatchException(arguments.length - 1, nArguments);
            }
            final Object[] captureds = (Object[]) arguments[0];
            if (captureds.length != nCaptured) {
                throw InternalPorcEError.capturedLengthError(nCaptured, captureds.length);
            }
        }

        long startTime = getTime();

        try {
            final Object ret = body.execute(frame);
            // Flush all negative counters to trigger halts quickly
            flushAllCounters.execute(frame);
            addTime(startTime);
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

    public static PorcERootNode create(final PorcELanguage language, final FrameDescriptor descriptor,
            final Expression body, final int nArguments, final int nCaptured, final Object methodKey,
            PorcEExecution execution) {
        // Add self tail call catcher to the body during construction.
        PorcERootNode r = new PorcERootNode(language, descriptor,
                CatchSelfTailCall.create(body), nArguments, nCaptured, methodKey,
                execution);
        Truffle.getRuntime().createCallTarget(r);
        return r;
    }

    @Override
    public String toString() {
        return String.format("PorcE[%s%s].%s", isInternal() ? "<" : "", methodKey, getName());
    }
}
