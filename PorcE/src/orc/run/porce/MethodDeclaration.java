
package orc.run.porce;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import orc.run.porce.NewContinuation.StopCachingException;
import orc.run.porce.runtime.PorcEClosure;

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
		private volatile long closureCacheChangeCount = 0;
		private volatile long closureCacheUseCount = 0;

		public NewMethodClosure(final Expression capturedTerminator, final Expression[] capturedExprs,
				final int nMethods) {
			this.capturedTerminator = capturedTerminator;
			this.capturedExprs = capturedExprs;
			this.nMethods = nMethods;
		}

		boolean isCachable() {
			if (CompilerDirectives.inInterpreter() && closureCacheChangeCount > 0) {
				long usePerChange = closureCacheUseCount / closureCacheChangeCount;
				return usePerChange >= 7 || closureCacheUseCount < 50;
			} else {
				return true;
			}
		}

		@Specialization(guards = { "EnvironmentCaching" }, rewriteOn = StopCachingException.class)
		@ExplodeLoop
		public Object cached(final VirtualFrame frame) throws StopCachingException {
			CompilerAsserts.compilationConstant(capturedExprs.length);
			int len = capturedExprs.length + nMethods;

			if (!isCachable()) {
				CompilerAsserts.neverPartOfCompilation("Cache invalidation should not be in compiled code.");
				throw new StopCachingException();
			}

			MethodClosure closure = closureCache;

			if (closure != null) {
				// If we have a cached environment then check that the cache is
				// still valid.
				for (int i = 0; i < capturedExprs.length; i++) { // Contains break
					if (closure.environment[i] != capturedExprs[i].execute(frame)) {
						closure = null;
						break;
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

				closureCacheChangeCount++;
				closureCache = closure;

				if (CompilerDirectives.inInterpreter())
					closureCacheChangeCount++;
			}

			if (CompilerDirectives.inInterpreter()) {
				closureCacheUseCount++;
				long useCount = closureCacheUseCount;

				/*
				long changeCount = closureCacheChangeCount;
				long reuseCount = useCount - changeCount;

				if ((useCount % 100) == 0) {
					Logger.info(() -> this + ": Allocating closure (" + (((float) reuseCount) / useCount) + " | "
							+ changeCount + "/" + useCount + ") with " + len + " captured variables");
				}
				*/

				if (useCount > 5000) {
					closureCacheUseCount = 0;
					closureCacheChangeCount = 0;
				}
			}

			return closure;
		}

		@Specialization(replaces = { "cached" })
		@ExplodeLoop
		public Object universal(final VirtualFrame frame) {
			final Object[] capturedValues = new Object[capturedExprs.length + nMethods];
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
