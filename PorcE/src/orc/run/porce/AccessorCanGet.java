//
// AccessorCanGet.java -- Truffle node AccessorCanGet
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
import orc.Invoker;
import orc.run.porce.NodeBase;
import orc.values.sites.InvocableInvoker;
import orc.values.sites.OverloadedDirectInvokerBase1;
import orc.values.sites.OverloadedDirectInvokerBase2;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * @author amp
 */
@Introspectable
abstract class AccessorCanGet extends NodeBase {
    public abstract boolean executeCanGet(VirtualFrame frame, Accessor accessor, Object target);

    @Specialization(guards = { "isPartiallyEvaluable(accessor)" })
    public boolean partiallyEvaluable(Accessor accessor, Object target) {
        return accessor.canGet(target);
    }

    protected static boolean isPartiallyEvaluable(Accessor accessor) {
        return accessor instanceof orc.values.sites.SimpleAccessor ||
                accessor instanceof orc.ErrorAccessor ||
                accessor instanceof orc.values.FastRecord.AccessorImpl ||
                false;
    }

    @Specialization
    @TruffleBoundary(allowInlining = true)
    public boolean unknown(Accessor accessor, Object target) {
        return accessor.canGet(target);
    }

    public static AccessorCanGet create() {
        return AccessorCanGetNodeGen.create();
    }

}
