//
// Versioned.java -- Java class Versioned
// Project Orca
//
// $Id$
//
// Created by dkitchin on Dec 15, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.trans;

/**
 * 
 *
 * @author dkitchin
 */
public interface Versioned<T> {

	public void merge(T other);
	public T branch();
	
}
