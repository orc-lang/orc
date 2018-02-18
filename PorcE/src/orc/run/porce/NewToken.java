
package orc.run.porce;

import orc.run.porce.runtime.Counter;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

@NodeChild("counter")
@ImportStatic(SpecializationConfiguration.class)
public class NewToken extends Expression {
	private final BranchProfile resurrectProfile = BranchProfile.create();
	
    @Specialization(guards = { "SpecializeOnCounterStates" })
    public PorcEUnit specialized(final Counter counter) {
        if (counter.newTokenOptimized()) {
        	resurrectProfile.enter();
        	counter.doResurrect();
        }
        return PorcEUnit.SINGLETON;
    }
    
    @Specialization
    public PorcEUnit run(final Counter counter) {
    	counter.newToken();
        return PorcEUnit.SINGLETON;
    }

    public static NewToken create(final Expression parent) {
        return NewTokenNodeGen.create(parent);
    }
}
