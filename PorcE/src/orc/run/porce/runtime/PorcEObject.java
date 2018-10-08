//
// PorcEObject.java -- Java class PorcEObject
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime;

import scala.Function1;

import orc.Accessor;
import orc.values.NoSuchMemberAccessor;
import orc.OrcRuntime;
import orc.run.distrib.DOrcMarshalingReplacement;
import orc.values.Field;
import orc.values.Format;
import orc.values.HasMembers;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

/**
 * This represents Orc objects inside the PorcE runtime.
 *
 * @author amp
 */
public final class PorcEObject implements HasMembers, DOrcMarshalingReplacement {
    public final Field[] fieldNames;
    public final Object[] fieldValues;

    public PorcEObject(final Field[] fieldNames, final Object[] fieldValues) {
        assert fieldNames.length == fieldValues.length;

        this.fieldNames = fieldNames;
        this.fieldValues = fieldValues;
    }

    private final ThreadLocal<Boolean> toStringRecursionCheck = new ThreadLocal<Boolean>() {
        @Override
        @SuppressWarnings("boxing")
        protected Boolean initialValue() {
            return false;
        }
    };

    @Override
    @SuppressWarnings("boxing")
    public String toString() {
        // FIXME: The recursion check will be SLOW. But it may not matter. However it may not even be good to have this as a default.
        if (toStringRecursionCheck.get()) {
            return "[... recursive ...]";
        } else {
            toStringRecursionCheck.set(true);
        	StringBuilder sb = new StringBuilder();
        	sb.append("{ ");
        	for (int i = 0; i < fieldNames.length; i++) {
    			Field field = fieldNames[i];
    			Object value = fieldValues[i];
    			if (i > 0) {
    				sb.append(" # ");
    			}
    			sb.append(field);
    			sb.append(" = ");
    			sb.append(Format.formatValue(value));
    		}
        	sb.append(" }");
            toStringRecursionCheck.set(false);
        	return sb.toString();
        }
    }

    private static class ObjectAccessor implements orc.values.sites.SimpleAccessor {
        @CompilationFinal(dimensions = 1)
        private final Field[] theseFieldNames;
        private final int index;

        public ObjectAccessor(Field[] theseFieldNames, int index) {
            super();
            this.theseFieldNames = theseFieldNames;
            this.index = index;
        }

        @Override
        public boolean canGet(final Object target) {
            return target instanceof PorcEObject && theseFieldNames == ((PorcEObject) target).fieldNames;
        }

        @Override
        public Object get(final Object target) {
            return ((PorcEObject) target).fieldValues[index];
        }
    }


    @Override
    public Accessor getAccessor(final OrcRuntime runtime, final Field field) {
        for (int i = 0; i < fieldNames.length; i++) {
            if (field.equals(fieldNames[i])) {
                return new ObjectAccessor(fieldNames, i);
            }
        }
        return new NoSuchMemberAccessor(this, field.name());
    }

    @Override
    public boolean isReplacementNeededForMarshaling(final Function1<Object, Object> marshalValueWouldReplace) {
        return JavaMarshalingUtilities.existsMarshalValueWouldReplace(fieldValues, marshalValueWouldReplace);
    }

    @Override
    public Object replaceForMarshaling(final Function1<Object, Object> marshaler) {
        return new PorcEObject(fieldNames, JavaMarshalingUtilities.mapMarshaler(fieldValues, marshaler));
    }

    @Override
    public boolean isReplacementNeededForUnmarshaling(final Function1<Object, Object> unmarshalValueWouldReplace) {
        return JavaMarshalingUtilities.existsMarshalValueWouldReplace(fieldValues, unmarshalValueWouldReplace);
    }

    @Override
    public Object replaceForUnmarshaling(final Function1<Object, Object> unmarshaler) {
        return new PorcEObject(fieldNames, JavaMarshalingUtilities.mapMarshaler(fieldValues, unmarshaler));
    }
}
