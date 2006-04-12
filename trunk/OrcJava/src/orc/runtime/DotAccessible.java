/*
 * Copyright 2006, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import orc.runtime.values.BaseValue;

/**
 * This is an interface for any object allows access to i.e. 
 * returns some object, when a dot operator is applied on it
 * Here are some example of DotAccessible objects:
 * 1. A Record allows access to its field objects through a 
 *    dot operator
 * 2. WebServicePlugin objects allows access to its 
 *    WebServiceSite objects through a dot operator 
 * @author pooja
 */

public interface DotAccessible 
{
	public BaseValue dotAccessibleVal(String valName);
}
