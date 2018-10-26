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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;

import orc.run.porce.PorcERootNode;
import orc.values.Format;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ValueProfile;

/**
 * A class representing PorcE closures including both Orc methods (def, site)
 * and internal Porc continuations.
 * <p>
 * Serialization: PorcEClosure relies on an external helper during serialization
 * and deserialization to translate instance state to/from a Serializable
 * marshaledFieldData. This is the only field written to the I/O stream.
 * 
 * @author amp
 */
final public class PorcEClosure implements Serializable, ObjectInputValidation {
    private static final long serialVersionUID = 3075892152696469588L;
    /**
     * @serialField marshaledFieldData Serializable
     */
    private static final ObjectStreamField[] serialPersistentFields = { new ObjectStreamField("marshaledFieldData", Serializable.class), new ObjectStreamField("isRoutine", Boolean.TYPE) };
    private Serializable marshaledFieldData;

    /*
     * NOTE! If fields are added/removed, update our PorcEClosure-custom
     * serialization logic.
     */

    public final Object[] environment;
    public final RootCallTarget body;

    public final boolean isRoutine;

    // TODO: PERFORMANCE: Using a frame instead of an array for captured values
    // may perform better. Though that will mainly be true when we start using
    // native values.
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
            final PorcERootNode root = (PorcERootNode) body.getRootNode();
            return root.getTimePerCall();
        } else {
            return Long.MAX_VALUE;
        }
    }

    public long getTimePerCall(final ValueProfile bodyProfile) {
        final RootNode r = bodyProfile.profile(body.getRootNode());
        if (r instanceof PorcERootNode) {
            final PorcERootNode root = (PorcERootNode) r;
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

    /*-
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

    public Serializable getMarshaledFieldData() {
        return marshaledFieldData;
    }

    public void setMarshaledFieldData(final Serializable marshaledFieldData) {
        this.marshaledFieldData = marshaledFieldData;
    }

    private void writeObject(final ObjectOutputStream out) throws IOException {
        if (marshaledFieldData == null) {
            throw new InvalidObjectException("Attempt to serialize PorcEClosure without having set marshaledFieldData");
        }
        out.defaultWriteObject();
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        in.registerValidation(this, 0);
    }

    @Override
    public void validateObject() throws InvalidObjectException {
        if (body == null) {
            throw new InvalidObjectException("Attempt to deserialize PorcEClosure without having initialized body from marshaledFieldData");
        }
        if (environment == null) {
            throw new InvalidObjectException("Attempt to deserialize PorcEClosure without having initialized environment from marshaledFieldData");
        }
    }

    public void marshalingInitFieldData(final Object[] newEnvironment, final RootCallTarget newBody) {
        if (environment != null || body != null) {
            throw new IllegalStateException("marshalingInitFieldData on already initialized PorcEClosure");
        }

        /* Use reflection to cram unmarshaled initial values into final fields */
        try {
            final Field environmentField = this.getClass().getField("environment");
            final Field bodyField = this.getClass().getField("body");
            environmentField.setAccessible(true);
            bodyField.setAccessible(true);
            environmentField.set(this, newEnvironment);
            bodyField.set(this, newBody);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new AssertionError("Failed to reflectively initialize PorcEClosure 'environment' or 'body' field during deserialization", e);
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
        // FIXME: The recursion check will be SLOW. But it may not matter.
        // However it may not even be good to have this
        // as a default.
        final StringBuilder sb = new StringBuilder();
        final RootNode rootNode = body.getRootNode();
        sb.append(rootNode.getName());
        final Class<? extends RootNode> rootNodeClass = rootNode.getClass();
        scala.collection.Iterator<orc.ast.porc.Variable> closureVariablesIter = null;
        if (rootNode instanceof PorcERootNode) {
            final PorcERootNode r = (PorcERootNode) rootNode;
            closureVariablesIter = ((scala.collection.IterableLike<orc.ast.porc.Variable, ?>) r.getClosureVariables()).toIterator();
        } else {
            sb.append(':');
            sb.append(rootNodeClass.getSimpleName());
        }
        if (PorcERuntime.displayClosureValues() && !toStringRecursionCheck.get()) {
            toStringRecursionCheck.set(true);
            sb.append('[');
            boolean notFirst = false;
            for (final Object v : environment) {
                if (notFirst) {
                    sb.append(", ");
                }
                if (closureVariablesIter != null && closureVariablesIter.hasNext()) {
                    final orc.ast.porc.Variable var = closureVariablesIter.next();
                    sb.append(var);
                    sb.append("=");
                }
                final String s = Format.formatValue(v);
                sb.append(s.substring(0, s.length() < 50 ? s.length() : 49));
                notFirst = true;
            }
            sb.append(']');
            toStringRecursionCheck.set(false);
        }
        return sb.toString();
    }

}
