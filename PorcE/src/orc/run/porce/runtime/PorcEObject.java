package orc.run.porce.runtime;

import orc.Accessor;
import orc.NoSuchMemberAccessor;
import orc.values.Field;
import orc.values.sites.AccessorValue;

public class PorcEObject implements AccessorValue {
	public final Field[] fieldNames;
	public final Object[] fieldValues;

	public PorcEObject(Field[] fieldNames, Object[] fieldValues) {
		assert fieldNames.length == fieldValues.length;
		
		this.fieldNames = fieldNames;
		this.fieldValues = fieldValues;
	}

	@Override
	public Accessor getAccessor(Field field) {
		for (int i = 0; i < fieldNames.length; i++) {
			if(field.equals(fieldNames[i])) {
				final int index = i;
				final Field[] theseFieldNames = fieldNames;
				return new Accessor() {
					@Override
					public boolean canGet(Object target) {
						return target instanceof PorcEObject && theseFieldNames == ((PorcEObject)target).fieldNames;
					}

					@Override
					public Object get(Object target) {
						return ((PorcEObject)target).fieldValues[index];
					}
				};
			}
		}
		return new NoSuchMemberAccessor(this, field.name());
	}
}
