
package orc.run.porce;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.PorcEClosure;

public class NewContinuation extends Expression {
	@Children
	protected final Expression[] capturedVariables;
	protected final RootCallTarget callTarget;

	private volatile PorcEClosure closureCache = null;

	@CompilerDirectives.CompilationFinal
	private volatile long closureCacheChangeCount = 0;
	@CompilerDirectives.CompilationFinal
	private volatile long closureCacheUseCount = 0;

	static class StopCachingException extends ControlFlowException {
		private static final long serialVersionUID = 1L;
	}

	@Override
	public void setPorcAST(final PorcAST ast) {
		((PorcERootNode) callTarget.getRootNode()).setPorcAST(ast);
		super.setPorcAST(ast);
	}

	protected NewContinuation(final Expression[] capturedVariables, final RootNode rootNode) {
		this.capturedVariables = capturedVariables;
		this.callTarget = Truffle.getRuntime().createCallTarget(rootNode);
	}

	boolean isCachable() {
		if (CompilerDirectives.inInterpreter() && closureCacheChangeCount > 0) {
			long usePerChange = closureCacheUseCount / closureCacheChangeCount;
			return usePerChange >= 7 || closureCacheUseCount < 100;
		} else {
			return true;
		}
	}

	@Specialization(rewriteOn = StopCachingException.class)
	@ExplodeLoop
	public Object cached(final VirtualFrame frame) {
		CompilerAsserts.compilationConstant(capturedVariables.length);

		if (!isCachable()) {
			CompilerAsserts.neverPartOfCompilation("Cache invalidation should not be in compiled code.");
			throw new StopCachingException();
		}

		PorcEClosure closure = closureCache;

		if (closure != null) {
			// If we have a cached closure then check that the cache is
			// still valid.
			for (int i = 0; i < capturedVariables.length; i++) { // Contains break
				if (closure.environment[i] != capturedVariables[i].execute(frame)) {
					closure = null;
					break;
				}
			}
		}

		if (closure == null) {
			// If we don't have a cached closure build one and set the
			// cache.
			Object[] capturedValues = new Object[capturedVariables.length];
			for (int i = 0; i < capturedVariables.length; i++) {
				capturedValues[i] = capturedVariables[i].execute(frame);
			}

			closure = new PorcEClosure(capturedValues, callTarget, false);
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
						+ changeCount + "/" + useCount + ") with " + capturedVariables.length + " captured variables");
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
		final Object[] capturedValues = new Object[capturedVariables.length];
		for (int i = 0; i < capturedVariables.length; i++) {
			capturedValues[i] = capturedVariables[i].execute(frame);
		}
		return new PorcEClosure(capturedValues, callTarget, false);
	}

	public static NewContinuation create(final Expression[] capturedVariables, final RootNode rootNode) {
		return NewContinuationNodeGen.create(capturedVariables, rootNode);
	}
}
