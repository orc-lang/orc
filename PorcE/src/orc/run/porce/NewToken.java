
package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import orc.run.porce.runtime.Counter;

@NodeChild("counter")
public class NewToken extends Expression {
	private final BranchProfile resurrectProfile = BranchProfile.create();
	
    @Specialization
    public PorcEUnit run(final Counter counter) {
        if (counter.newTokenOptimized()) {
        	resurrectProfile.enter();
        	counter.doResurrect();
        }
        return PorcEUnit.SINGLETON;
    }

    public static NewToken create(final Expression parent) {
        return NewTokenNodeGen.create(parent);
    }
}
