//
// MaximumValueProfile.java -- Truffle profile MaximumValueProfile
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

/**
 * A value profile which tracks the maximum value observed.
 * 
 * @author amp
 */
public final class MaximumValueProfile {
	private MaximumValueProfile() {
	}

	@CompilationFinal
	private int max = Integer.MIN_VALUE;

	/**
	 * Profile the value and return the maximum value observed.
	 * 
	 * This will invalidate if v is greater than the compiled in maximum.
	 */
	public int max(int v) {
		if (v > max) {
			CompilerDirectives.transferToInterpreterAndInvalidate();
			max = v;
			return v;
		}
		return max;
	}
	
	@Override
	public String toString() {
		return "MaximumValueProfile(max=" + max + ")@" + Integer.toHexString(this.hashCode());
	}
	
	static public MaximumValueProfile create() {
		return new MaximumValueProfile();
	}
}
