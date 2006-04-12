/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

/**
 * A value container for a literal value
 * @author wcook
 */
public class Constant extends BaseValue {

	Object value;

	public Constant(Object value) {
		this.value = value;
	}

	public Object asBasicValue() {
		return value;
	}
}
