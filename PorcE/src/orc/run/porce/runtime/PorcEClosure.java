//
// PorcEClosure.java -- Java class PorcEClosure
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime;

import orc.run.porce.PorcERootNode;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;

/**
 * A class representing PorcE closures including both Orc methods (def, site) and internal Porc continuations.
 *
 * @author amp
 */
final public class PorcEClosure {
    public final Object[] environment;
    public final RootCallTarget body;

    public final boolean isRoutine;

    // TODO: PERFORMANCE: Using a frame instead of an array for captured values may perform better. Though that will mainly be true when we start using native values.
	public PorcEClosure(final Object[] environment, final RootCallTarget body, final boolean isRoutine) {
		CompilerDirectives.interpreterOnly(() -> {
			if (body == null) {
				throw new IllegalArgumentException("body == null");
			}
			if (environment == null) {
				throw new IllegalArgumentException("environment == null");
			}
		});

		this.environment = environment;
		this.body = body;
		this.isRoutine = isRoutine;
	}
	
	public long getTimePerCall() {
		if (body.getRootNode() instanceof PorcERootNode) {
			PorcERootNode root = (PorcERootNode)body.getRootNode();
			return root.getTimePerCall();
		} else {
			return Long.MAX_VALUE;
		}
	}

    public Object callFromRuntimeArgArray(final Object[] values) {
        values[0] = environment;
        return body.call(values);
    }

    public Object callFromRuntime() {
        return body.call((Object) environment);
    }

    /*
    public Object callFromRuntime(final Object p1) {
        return body.call(environment, p1);
    }

    public Object callFromRuntime(final Object p1, final Object p2) {
        return body.call(environment, p1, p2);
    }

    public Object callFromRuntime(final Object p1, final Object p2, final Object p3) {
        return body.call(environment, p1, p2, p3);
    }

    public Object callFromRuntimeVarArgs(final Object[] args) {
        final Object[] values = new Object[args.length + 1];
        values[0] = environment;
        System.arraycopy(args, 0, values, 1, args.length);
        return body.call(values);
    }
    */
    
	@Override
	public String toString() {
		return body.getRootNode().getName();
	}

}
