
package orc.run.porce.runtime;

import scala.Function1;

import orc.Accessor;
import orc.NoSuchMemberAccessor;
import orc.OrcRuntime;
import orc.run.distrib.DOrcMarshalingReplacement;
import orc.values.Field;
import orc.values.Format;
import orc.values.sites.AccessorValue;

public final class PorcEObject implements AccessorValue, DOrcMarshalingReplacement {
    public final Field[] fieldNames;
    public final Object[] fieldValues;

    // TODO: PERFORMANCE: Using a frame instead of an array for field values may
    // perform better. Though that will mainly be true when we start using
    // native values.
    public PorcEObject(final Field[] fieldNames, final Object[] fieldValues) {
        assert fieldNames.length == fieldValues.length;

        this.fieldNames = fieldNames;
        this.fieldValues = fieldValues;
    }
    
    @Override
    public String toString() {
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
    	return sb.toString();
    }

    @Override
    public Accessor getAccessor(final OrcRuntime runtime, final Field field) {
        for (int i = 0; i < fieldNames.length; i++) {
            if (field.equals(fieldNames[i])) {
                final int index = i;
                final Field[] theseFieldNames = fieldNames;
                return new Accessor() {
                    @Override
                    public boolean canGet(final Object target) {
                        return target instanceof PorcEObject && theseFieldNames == ((PorcEObject) target).fieldNames;
                    }

                    @Override
                    public Object get(final Object target) {
                        return ((PorcEObject) target).fieldValues[index];
                    }
                };
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
