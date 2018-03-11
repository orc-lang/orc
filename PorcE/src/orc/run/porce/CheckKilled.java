//
// CheckKilled.java -- Truffle node CheckKilled
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

@NodeChild("terminator")
public class CheckKilled extends Expression {
	private final BranchProfile killedProfile = BranchProfile.create();
	
    @Specialization
    public PorcEUnit run(final Terminator terminator) {
    	try {
    		terminator.checkLive();
    	} catch(KilledException k) {
    		killedProfile.enter();
    		throw k;
    	}
        return PorcEUnit.SINGLETON;
    }

    public static CheckKilled create(final Expression parent) {
        return CheckKilledNodeGen.create(parent);
    }
}
