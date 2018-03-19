//
// InternalCPSDispatch.java -- Java class InternalCPSDispatch
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.call;

import orc.error.runtime.HaltException;
import orc.run.porce.Expression;
import orc.run.porce.PorcERootNode;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.SelfTailCallException;
import orc.run.porce.runtime.TailCallException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;


@Instrumentable(factory = InternalCPSDispatchWrapper.class)
public class InternalCPSDispatch extends Dispatch {
	@Child
	protected InternalCPSDispatch.InternalCPSDispatchInternal internal;
	
	protected InternalCPSDispatch(final boolean forceInline, final PorcEExecution execution) {
		super(execution);
		internal = InternalCPSDispatch.InternalCPSDispatchInternal.createBare(forceInline, execution);
	}
	 
	protected InternalCPSDispatch(final InternalCPSDispatch orig) {
		super(orig.internal.execution);
		internal = InternalCPSDispatch.InternalCPSDispatchInternal.createBare(orig.internal.forceInline, orig.internal.execution);
	}
	 
	@Override
	public void setTail(boolean v) {
		super.setTail(v);
		internal.setTail(v);
	}
	
	@Override
	public void executeDispatchWithEnvironment(VirtualFrame frame, Object target, Object[] arguments) {
		arguments[0] = ((PorcEClosure)target).environment;
		internal.execute(frame, target, arguments);
	}
	
	@Override
    public void executeDispatch(VirtualFrame frame, Object target, Object[] arguments) {
		internal.execute(frame, target, buildArguments((PorcEClosure)target, arguments));
	}
	
	protected static Object[] buildArguments(PorcEClosure target, Object[] arguments) {
		//CompilerAsserts.compilationConstant(arguments.length);
		Object[] newArguments = new Object[arguments.length + 1];
		newArguments[0] = target.environment;
		System.arraycopy(arguments, 0, newArguments, 1, arguments.length);
		return newArguments;
	}
	
	static InternalCPSDispatch createBare(final boolean forceInline, PorcEExecution execution) {
		return new InternalCPSDispatch(forceInline, execution);
	}
	
	static InternalCPSDispatch createBare(PorcEExecution execution) {
		return new InternalCPSDispatch(false, execution);
	}
	
	public static Dispatch create(final PorcEExecution execution, boolean isTail) {
		return create(false, execution, isTail);
	}
	
	public static Dispatch create(final boolean forceInline, final PorcEExecution execution, boolean isTail) {
		if (isTail)
			return createBare(forceInline, execution);
		else
			return CatchTailDispatch.create(createBare(forceInline, execution), execution);
	}

	@ImportStatic({ SpecializationConfiguration.class })
	@Introspectable
	@Instrumentable(factory = InternalCPSDispatchInternalWrapper.class)
	public static abstract class InternalCPSDispatchInternal extends DispatchBase {
		protected final boolean forceInline;

		protected InternalCPSDispatchInternal(final boolean forceInline, final PorcEExecution execution) {
			super(execution);
			this.forceInline = forceInline;
		}

		protected InternalCPSDispatchInternal(final InternalCPSDispatchInternal orig) {
			super(orig.execution);
			this.forceInline = orig.forceInline;
		}

		@CompilationFinal
		private RootNode rootNode;

		protected final BranchProfile exceptionProfile = BranchProfile.create();

		public RootNode getRootNodeCached() {
			if (CompilerDirectives.inInterpreter()) {
				rootNode = getRootNode();
			}
			return rootNode;
		}

		public abstract void execute(VirtualFrame frame, Object target, Object[] arguments);

	// TODO: It would probably improve compile times to split tail and non-tail cases into separate classes so only one set has to be checked for any call.

		// Tail calls

		@Specialization(guards = { "SelfTCO", "isTail", "getRootNodeCached() == target.body.getRootNode()" })
		public void selfTail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments) {
			Object[] frameArguments = frame.getArguments();
			System.arraycopy(arguments, 0, frameArguments, 0, arguments.length);
			throw new SelfTailCallException();
		}

		// The RootNode guard is required so that selfTail can be activated even
		// after tail has activated.
		@Specialization(guards = { "UniversalTCO", "isTail", "getRootNodeCached() != target.body.getRootNode()" })
		public void tail(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
				@Cached("createBinaryProfile()") ConditionProfile reuseTCE) {
			Object[] thisArguments = frame.getArguments();
    	if (reuseTCE.profile(
    			/*arguments.length <= 16 &&*/ thisArguments.length == 17 && thisArguments[16] instanceof TailCallException)) {
				TailCallException tce = (TailCallException) thisArguments[16];
				System.arraycopy(arguments, 0, tce.arguments, 0, arguments.length);
				tce.target = target;
				throw tce;
			}

			throw TailCallException.create(target, arguments);
		}

		// Non-tail calls

    // This is disabled since it's not likely to be useful and the trick with createVirtualFrame might be a problem.
		// This just guarentees that is cannot be an issue.
    // If you reenable it also add specificInline back into the replaces clause of universal.
	    /*
		@Specialization(guards = { "TruffleASTInlining", "forceInline", "body != null", "matchesSpecific(target, expected)" }, 
				limit = "InternalCallMaxCacheSize")
	    public void specificInline(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
	    		@Cached("target") PorcEClosure expected, 
	    		@Cached("getPorcEBody(target)") Expression body, @Cached("getPorcEFrameDescriptor(target)") FrameDescriptor fd) {
			final VirtualFrame nestedFrame = Truffle.getRuntime().createVirtualFrame(arguments, fd);
			body.execute(nestedFrame);
	    }
	    */

		@Specialization(guards = { "target.body == expected" }, limit = "InternalCallMaxCacheSize")
		public void specific(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
				@Cached("target.body") RootCallTarget expected, @Cached("create(expected)") DirectCallNode call) {
			CompilerDirectives.interpreterOnly(() -> {
				if (forceInline)
					call.forceInlining();
			});

            try {
                call.call(arguments);
            } catch (final TailCallException e) {
                throw e;
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                throw HaltException.SINGLETON();
            }
		}

		@Specialization(replaces = { "specific" })
		public void universal(final VirtualFrame frame, final PorcEClosure target, final Object[] arguments,
				@Cached("create()") IndirectCallNode call) {
		    try {
              call.call(target.body, arguments);
            } catch (final TailCallException e) {
                throw e;
            } catch (final Throwable e) {
                exceptionProfile.enter();
                execution.notifyOfException(e, this);
                throw HaltException.SINGLETON();
            }
		}

		static InternalCPSDispatchInternal createBare(final boolean forceInline, final PorcEExecution execution) {
			return InternalCPSDispatchFactory.InternalCPSDispatchInternalNodeGen.create(forceInline, execution);
		}

		/* Utilties */

		protected static Expression getPorcEBody(PorcEClosure target) {
			RootNode r = target.body.getRootNode();
			if (r instanceof PorcERootNode) {
				return (Expression) ((PorcERootNode) r).getBody().copy();
			} else {
				return null;
			}
		}

		protected static FrameDescriptor getPorcEFrameDescriptor(PorcEClosure target) {
			RootNode r = target.body.getRootNode();
			if (r instanceof PorcERootNode) {
				return r.getFrameDescriptor();
			} else {
				return null;
			}
		}
	}
}
