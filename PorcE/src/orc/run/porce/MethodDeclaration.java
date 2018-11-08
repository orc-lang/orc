//
// MethodDeclaration.java -- Truffle nodes MethodDeclaration.*
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.NewContinuation.StopCachingException;
import orc.run.porce.runtime.PorcEClosure;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class MethodDeclaration {

    public static class MethodClosure {
        final Object[] environment;

        public MethodClosure(final Object[] environment) {
            this.environment = environment;
        }
    }

    @Introspectable
    @ImportStatic(SpecializationConfiguration.class)
	public static class NewMethodClosure extends Expression {
		@Children
		protected final Expression[] capturedExprs;
		@Child
		protected Expression capturedTerminator;

		protected final int nMethods;

		private volatile MethodClosure closureCache = null;

		public NewMethodClosure(final Expression capturedTerminator, final Expression[] capturedExprs,
				final int nMethods) {
			this.capturedTerminator = capturedTerminator;
			this.capturedExprs = capturedExprs;
			this.nMethods = nMethods;
		}

		@SuppressWarnings("boxing")
        @Specialization(guards = { "EnvironmentCaching" }, rewriteOn = StopCachingException.class)
		@ExplodeLoop
		public Object cached(final VirtualFrame frame) throws StopCachingException {
			final int len = capturedExprs.length + nMethods;
			CompilerAsserts.compilationConstant(len);

			MethodClosure closure = closureCache;

			if (closure != null) {
				// If we have a cached environment then check that the cache is
				// still valid.
				for (int i = 0; i < capturedExprs.length; i++) { // Contains break
					if (closure.environment[i] != capturedExprs[i].execute(frame)) {
						closure = null;
						// If we had a cached closure and invalidated it, don't cache again.
						closureCache = null;
						throw new StopCachingException();
					}
				}
			}

			if (closure == null) {
				// If we don't have a cached environment build one and set the
				// cache.
				final Object[] capturedValues = new Object[len];
				for (int i = 0; i < capturedExprs.length; i++) {
					capturedValues[i] = capturedExprs[i].execute(frame);
				}
				closure = new MethodClosure(capturedValues);

				closureCache = closure;
			}

			return closure;
		}

		@Specialization(replaces = { "cached" })
		@ExplodeLoop
		public Object universal(final VirtualFrame frame) {
			final Object[] capturedValues = new Object[capturedExprs.length + nMethods];
            CompilerAsserts.compilationConstant(capturedValues.length);
			for (int i = 0; i < capturedExprs.length; i++) {
				capturedValues[i] = capturedExprs[i].execute(frame);
			}
			return new MethodClosure(capturedValues);
		}

		public static NewMethodClosure create(final Expression capturedTerminator, final Expression[] capturedExprs, final int nMethods) {
			return MethodDeclarationFactory.NewMethodClosureNodeGen.create(capturedTerminator, capturedExprs, nMethods);
		}
    }

    @NodeChild("closure")
    public static class NewMethod extends Expression {
    	final int index;
    	final RootCallTarget callTarget;
    	final boolean isRoutine;

		protected NewMethod(final int index, final RootCallTarget callTarget, final boolean isRoutine) {
			this.index = index;
			this.callTarget = callTarget;
			this.isRoutine = isRoutine;
		}

        @Specialization
        public PorcEClosure run(final MethodClosure closure) {
        	if(closure.environment[index] == null) {
        		// This races with itself if the closure is reused. But that doesn't matter since any instance is equivalent.
	            final PorcEClosure m = new PorcEClosure(closure.environment, callTarget, isRoutine);
	            NodeBase.UNSAFE.fullFence();
	            closure.environment[index] = m;
        	}
            return (PorcEClosure) closure.environment[index];
        }

        public static NewMethod create(final Expression closure, final int index, final PorcERootNode rootNode, final boolean isRoutine) {
            final RootCallTarget callTarget = rootNode.getCallTarget();
            return MethodDeclarationFactory.NewMethodNodeGen.create(index, callTarget, isRoutine, closure);
        }
    }

}
