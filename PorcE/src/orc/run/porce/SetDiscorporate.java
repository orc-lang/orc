
package orc.run.porce;

import orc.run.porce.runtime.Counter;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild(value = "c", type = Expression.class)
public abstract class SetDiscorporate extends Expression {
    @Specialization
    public PorcEUnit run(final Counter c) {
        c.setDiscorporate();
        return PorcEUnit.SINGLETON;
    }

    public static SetDiscorporate create(final Expression c) {
        return SetDiscorporateNodeGen.create(c);
    }
}
