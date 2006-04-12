/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime.values;

/**
 * Interface for value containers
 * @author wcook
 */
public interface Value {

	/**
	 * Check if a value container is bound
	 * @return true if it is unbound
	 */
	GroupCell asUnboundCell();

	/**
	 * If the container is bound, return the underlying java value
	 * @return any value
	 */
	Object asBasicValue();
}
