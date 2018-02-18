
package orc.run.porce;

import orc.run.porce.runtime.Future;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild(value = "future", type = Expression.class)
@NodeChild(value = "value", type = Expression.class)
public abstract class Bind extends Expression {
    @Specialization
    public PorcEUnit bindFuture(final Future future, final Object value) {
        future.bind(value);
        return PorcEUnit.SINGLETON;
    }

    public static Bind create(final Expression future, final Expression value) {
        return BindNodeGen.create(future, value);
    }
}
