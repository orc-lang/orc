//
// GetMethod.java -- Truffle node GetMethod
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.Accessor;
import orc.ErrorAccessor;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.Field;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import static orc.run.porce.GetField.*;

@NodeChild(value = "object", type = Expression.class)
@Introspectable
@ImportStatic({ SpecializationConfiguration.class, GetField.class })
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

    @Specialization(guards = { "canGetNode.executeCanGet(frame, accessor, obj)" }, limit = "GetFieldMaxCacheSize")
    public Object cachedAccessor(final VirtualFrame frame, final Object obj,
            @Cached("create()") AccessorCanGet canGetNode,
            @Cached("create()") AccessorGet getNode,
            @Cached("getAccessorWithBoundary(obj)") final Accessor accessor) {
        try {
            if (accessor instanceof ErrorAccessor) {
                return obj;
            } else {
                return getNode.executeGet(frame, accessor, obj);
            }
        } catch (final Exception e) {
            CompilerDirectives.transferToInterpreter();
            execution.notifyOfException(e, this);
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
            execution.notifyOfException(e, this);
            throw HaltException.SINGLETON();
        }
    }

    @TruffleBoundary
    protected Accessor getAccessorWithBoundary(final Object t) {
        return execution.runtime().getAccessor(t, field);
    }

    public static GetMethod create(final Expression object, final PorcEExecution execution) {
        return GetMethodNodeGen.create(execution, object);
    }
}
