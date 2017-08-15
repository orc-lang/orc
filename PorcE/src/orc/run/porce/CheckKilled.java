
package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import orc.run.porce.runtime.Terminator;

@NodeChild("terminator")
public class CheckKilled extends Expression {
    @Specialization
    public PorcEUnit run(final Terminator terminator) {
        terminator.checkLive();
        return PorcEUnit.SINGLETON;
    }

    public static CheckKilled create(final Expression parent) {
        return CheckKilledNodeGen.create(parent);
    }
}
