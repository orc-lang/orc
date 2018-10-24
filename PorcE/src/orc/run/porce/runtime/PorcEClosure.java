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
import orc.values.Format;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;

/**
 * A class representing PorcE closures including both Orc methods (def, site) and internal Porc continuations.
 *
 * @author amp
 */
@ValueType
final public class PorcEClosure {
    public final Object[] environment;
    public final RootCallTarget body;

    public final boolean isRoutine;

    // TODO: PERFORMANCE: Using a frame instead of an array for captured values may perform better. Though that will
    // mainly be true when we start using native values.
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
            PorcERootNode root = (PorcERootNode) body.getRootNode();
            return root.getTimePerCall();
        } else {
            return Long.MAX_VALUE;
        }
    }

    public long getTimePerCall(ValueProfile bodyProfile) {
        RootNode r = bodyProfile.profile(body.getRootNode());
        if (r instanceof PorcERootNode) {
            PorcERootNode root = (PorcERootNode) r;
            return root.getTimePerCall();
        } else {
            return Long.MAX_VALUE;
        }
    }

    private static final ThreadLocal<Boolean> toStringRecursionCheck = new ThreadLocal<Boolean>() {
        @Override
        @SuppressWarnings("boxing")
        protected Boolean initialValue() {
            return false;
        }
    };

    @Override
    @SuppressWarnings("boxing")
    public String toString() {
        // FIXME: The recursion check will be SLOW. But it may not matter. However it may not even be good to have this
        // as a default.
        StringBuilder sb = new StringBuilder();
        RootNode rootNode = body.getRootNode();
        sb.append(rootNode.getName());
        Class<? extends RootNode> rootNodeClass = rootNode.getClass();
        scala.collection.Iterator<orc.ast.porc.Variable> closureVariablesIter = null;
        if (rootNode instanceof PorcERootNode) {
            PorcERootNode r = (PorcERootNode) rootNode;
            closureVariablesIter = ((scala.collection.IterableLike<orc.ast.porc.Variable, ?>)r.getClosureVariables()).toIterator();
        } else {
            sb.append(':');
            sb.append(rootNodeClass.getSimpleName());
        }
        if (PorcERuntime.displayClosureValues() && !toStringRecursionCheck.get()) {
            toStringRecursionCheck.set(true);
            sb.append('[');
            boolean notFirst = false;
            for (Object v : environment) {
                if (notFirst) {
                    sb.append(", ");
                }
                if (closureVariablesIter != null && closureVariablesIter.hasNext()) {
                    orc.ast.porc.Variable var = closureVariablesIter.next();
                    sb.append(var);
                    sb.append("=");
                }
                String s = Format.formatValue(v);
                sb.append(s.substring(0, s.length() < 50 ? s.length() : 49));
                notFirst = true;
            }
            sb.append(']');
            toStringRecursionCheck.set(false);
        }
        return sb.toString();
    }

}
