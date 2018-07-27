//
// AccessorGet.java -- Truffle node AccessorGet
// Project PorcE
//
// Created by amp on Jun 25, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.Accessor;
import orc.CallContext;
import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.HaltException;
import orc.error.runtime.JavaException;
import orc.run.extensions.SiteInvoker;
import orc.run.porce.NodeBase;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.StackCheckingDispatch;
import orc.run.porce.runtime.CPSCallContext;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.sites.InvocableInvoker;
import orc.values.sites.OverloadedDirectInvokerBase1;
import orc.values.sites.OverloadedDirectInvokerBase2;
import orc.values.sites.OrcJavaCompatibility;
import orc.values.Signal$;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * @author amp
 */
@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
abstract class AccessorGet extends NodeBase {
    public abstract Object executeGet(VirtualFrame frame, Accessor accessor, Object target);

    @Specialization(guards = { "isPartiallyEvaluable(accessor)" })
    public Object partiallyEvaluable(Accessor accessor, Object target) {
        return accessor.get(target);
    }

    protected static boolean isPartiallyEvaluable(Accessor accessor) {
        return AccessorCanGet.isPartiallyEvaluable(accessor);
    }

    @Specialization
    @TruffleBoundary(allowInlining = true)
    public Object unknown(Accessor accessor, Object target) {
        return accessor.get(target);
    }


    public static AccessorGet create() {
        return AccessorGetNodeGen.create();
    }
}
