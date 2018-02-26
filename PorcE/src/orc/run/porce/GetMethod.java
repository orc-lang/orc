
package orc.run.porce;

import orc.Accessor;
import orc.CaughtEvent;
import orc.ErrorAccessor;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.Field;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;

@NodeChild(value = "object", type = Expression.class)
@Introspectable
public class GetMethod extends Expression {
    protected final PorcEExecution execution;
    protected static final Field field = Field.create("apply");

    protected GetMethod(final PorcEExecution execution) {
        this.execution = execution;
    }

    @Specialization
    public Object closure(final PorcEClosure obj) {
        return obj;
    }

    @Specialization(guards = { "canGetWithBoundary(accessor, obj)" }, limit = "getMaxCacheSize()")
    public Object cachedAccessor(final Object obj, @Cached("getAccessorWithBoundary(obj)") final Accessor accessor) {
        try {
            if (accessor instanceof ErrorAccessor) {
                return obj;
            } else {
                return accessWithBoundary(accessor, obj);
            }
        } catch (final Exception e) {
            CompilerDirectives.transferToInterpreter();
            execution.notifyOrcWithBoundary(new CaughtEvent(e));
            throw HaltException.SINGLETON();
        }
    }

    @Specialization(replaces = { "cachedAccessor" })
    public Object slowPath(final Object obj) {
    	// This additional argument is useful for debugging: , @Cached("getAccessorWithBoundary(obj)") final Accessor firstAccessor
        try {
            final Accessor accessor = getAccessorWithBoundary(obj);
            if (accessor instanceof ErrorAccessor) {
                return obj;
            } else {
                return accessWithBoundary(accessor, obj);
            }
        } catch (final Exception e) {
            CompilerDirectives.transferToInterpreter();
            execution.notifyOrcWithBoundary(new CaughtEvent(e));
            throw HaltException.SINGLETON();
        }
    }

    @TruffleBoundary
    protected Accessor getAccessorWithBoundary(final Object t) {
        return execution.runtime().getAccessor(t, field);
    }

    @TruffleBoundary(allowInlining = true, throwsControlFlowException = true)
    protected static Object accessWithBoundary(final Accessor accessor, final Object obj) {
        return accessor.get(obj);
    }

    @TruffleBoundary(allowInlining = true)
    protected static boolean canGetWithBoundary(final Accessor accessor, final Object obj) {
        return accessor.canGet(obj);
    }

    protected static boolean isNotError(final Accessor accessor) {
        return !(accessor instanceof ErrorAccessor);
    }

    protected static int getMaxCacheSize() {
        return SpecializationConfiguration.GetFieldMaxCacheSize;
    }

    public static GetMethod create(final Expression object, final PorcEExecution execution) {
        return GetMethodNodeGen.create(execution, object);
    }
}
