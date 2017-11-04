
package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.run.porce.runtime.KilledException;
import orc.run.porce.runtime.Terminator;

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
