
package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import orc.run.porce.runtime.Counter;

@NodeChild("counter")
public class HaltToken extends Expression {
    @Specialization
    public PorcEUnit run(final Counter counter) {
        // TODO: PERFORMANCE: Figure out a way to have haltToken return a closure to call. This could be passed to a call node which could efficiently invoke it here.
        counter.haltToken();
        return PorcEUnit.SINGLETON;
    }

    public static HaltToken create(final Expression parent) {
        return HaltTokenNodeGen.create(parent);
    }
}
