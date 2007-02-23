/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;
import java.io.*;

/**
 * Base class that for value containers
 * @author wcook
 */
public class BaseValue implements Value, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Determine if the value is unbound
	 *  
	 * @see orc.runtime.values.Value#asUnboundCell()
	 */
	public GroupCell asUnboundCell() {
		return null;
	}

	/** 
	 * Extract the underlying Java value of the container
	 * 
	 * @see orc.runtime.values.Value#asBasicValue()
	 */
	public Object asBasicValue() {
		return this;
	}
	
	public boolean Callable0() {
		return false; //overridden by Closure and Site
	}

}
