//
// NewContinuation.java -- Truffle node NewContinuation
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.ast.porc.PorcAST;
import orc.run.porce.runtime.PorcEClosure;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.SlowPathException;

@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
public class NewContinuation extends Expression {
	@Children
	protected final Expression[] capturedVariables;
	protected final RootCallTarget callTarget;
	protected final boolean reuseClosure;

	private volatile PorcEClosure closureCache = null;

	@CompilerDirectives.CompilationFinal
	private volatile long closureCacheChangeCount = 0;
	@CompilerDirectives.CompilationFinal
	private volatile long closureCacheUseCount = 0;

	static final class StopCachingException extends SlowPathException {
		private static final long serialVersionUID = 1L;
	}

	@Override
	public void setPorcAST(final PorcAST ast) {
		((PorcERootNode) callTarget.getRootNode()).setPorcAST(ast);
		super.setPorcAST(ast);
	}

	protected NewContinuation(final Expression[] capturedVariables, final RootNode rootNode, final boolean reuseClosure) {
		this.capturedVariables = capturedVariables;
		this.callTarget = rootNode.getCallTarget();
		this.reuseClosure = reuseClosure;
	}

	private boolean isCachable() {
		if (CompilerDirectives.inInterpreter() && closureCacheChangeCount > 0) {
			long usePerChange = closureCacheUseCount / closureCacheChangeCount;
			return usePerChange >= 7 || closureCacheUseCount < 100;
		} else {
			return true;
		}
	}

	@Specialization(guards = { "reuseClosure" })
	public Object reuse(final VirtualFrame frame) {
		final Object[] capturedValues = (Object[]) frame.getArguments()[0];
		return new PorcEClosure(capturedValues, callTarget, false);
	}
	
	@Specialization(guards = { "EnvironmentCaching" }, rewriteOn = StopCachingException.class)
	@ExplodeLoop
	public Object cached(final VirtualFrame frame) throws StopCachingException {
		CompilerAsserts.compilationConstant(capturedVariables.length);

		/*
		if (!isCachable()) {
			CompilerAsserts.neverPartOfCompilation("Cache invalidation should not be in compiled code.");
			throw new StopCachingException();
		}
		*/

		PorcEClosure closure = closureCache;

		if (closure != null) {
			// If we have a cached closure then check that the cache is
			// still valid.
			for (int i = 0; i < capturedVariables.length; i++) { // Contains break
				if (closure.environment[i] != capturedVariables[i].execute(frame)) {
					closure = null;
					// If we had a cached closure and invalidated it, don't cache again.
					closureCache = null;
					throw new StopCachingException();
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

			/*
			if (CompilerDirectives.inInterpreter())
				closureCacheChangeCount++;
				*/
		}

		/*
		if (CompilerDirectives.inInterpreter()) {
			closureCacheUseCount++;
			long useCount = closureCacheUseCount;

			if (useCount > 5000) {
				closureCacheUseCount = 0;
				closureCacheChangeCount = 0;
			}
		}
		*/

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

	public static NewContinuation create(final Expression[] capturedVariables, final RootNode rootNode, final boolean reuseClosure) {
		return NewContinuationNodeGen.create(capturedVariables, rootNode, reuseClosure);
	}
}
