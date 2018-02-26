
package orc.run.porce;

import orc.run.porce.runtime.Future;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild(value = "future", type = Expression.class)
public abstract class BindStop extends Expression {
    @Specialization
    public PorcEUnit bindFutureStop(final Future future) {
        future.stop();
        return PorcEUnit.SINGLETON;
    }

    public static BindStop create(final Expression future) {
        return BindStopNodeGen.create(future);
    }
}
