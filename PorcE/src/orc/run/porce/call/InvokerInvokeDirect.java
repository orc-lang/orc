//
// InvokerInvokeDirect.java -- Truffle node InvokerInvokeDirect
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

package orc.run.porce.call;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;

import orc.DirectInvoker;
import orc.compile.orctimizer.OrcAnnotation;
import orc.error.runtime.JavaException;
import orc.run.porce.NodeBase;
import orc.run.porce.SpecializationConfiguration;
import orc.run.porce.runtime.CallAdapter;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.NumericsConfig;
import orc.values.sites.InvocableInvoker;
import orc.values.sites.OrcJavaCompatibility;
import orc.values.sites.OverloadedDirectInvokerBase1;
import orc.values.sites.OverloadedDirectInvokerBase2;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;

/**
 * A node to call canInvoke on invokers.
 *
 * This is a separate node so that it can specialize on Invoker classes which can be optimized by Truffle.
 *
 * @author amp
 */
@ImportStatic({ SpecializationConfiguration.class })
@Introspectable
abstract class InvokerInvokeDirect extends NodeBase {
    protected final PorcEExecution execution;
    private final BranchProfile exceptionProfile = BranchProfile.create();

    protected InvokerInvokeDirect(PorcEExecution execution) {
        this.execution = execution;
    }

    /**
     * Dispatch the call to the target with the given arguments.
     *
     * @param frame
     *            The frame we are executing in.
     * @param invoker
     *            The call invoker.
     * @param target
     *            The call target.
     * @param arguments
     *            The arguments to the call as expected by the invoker (without PCT).
     */
    public abstract Object executeInvokeDirect(VirtualFrame frame, DirectInvoker invoker, Object target,
            Object[] arguments);

    // TODO: SMELL: Most of this class reimplementations various sites to specialize them to
    //   PorcE by using partial evaluation or inlining publications.

    @SuppressWarnings("unchecked")
    @Specialization(guards = { "KnownSiteSpecialization" })
    public <T1> Object overloadedDirectInvokerBase1(OverloadedDirectInvokerBase1<T1> invoker, Object target,
            Object[] arguments) {
        if (orc.run.StopWatches.callsEnabled()) {
            long s = orc.run.StopWatches.implementationTime().start();
            try {
                return callFunction1(invoker.implementation(), (T1) arguments[0]);
            } finally {
                orc.run.StopWatches.implementationTime().stop(s);
            }
        } else {
            return callFunction1(invoker.implementation(), (T1) arguments[0]);
        }
    }

    @TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
    private <T1> Object callFunction1(scala.Function1<T1, Object> f, T1 a) {
        return f.apply(a);
    }

    @SuppressWarnings("unchecked")
    @Specialization(guards = { "KnownSiteSpecialization" })
    public <T1, T2> Object overloadedDirectInvokerBase2(OverloadedDirectInvokerBase2<T1, T2> invoker, Object target,
            Object[] arguments) {
        if (orc.run.StopWatches.callsEnabled()) {
            long s = orc.run.StopWatches.implementationTime().start();
            try {
                return callFunction2(invoker.implementation(), (T1) arguments[0], (T2) arguments[1]);
            } finally {
                orc.run.StopWatches.implementationTime().stop(s);
            }
        } else {
            scala.Function2<T1, T2, Object> impl = invoker.implementation();
            return callFunction2(impl, (T1) arguments[0], (T2) arguments[1]);
        }
    }

    @TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
    private <T1, T2> Object callFunction2(scala.Function2<T1, T2, Object> f, T1 a, T2 b) {
        return f.apply(a, b);
    }

    @Specialization(guards = { "!invoker.invocable().isVarArgs()", "KnownSiteSpecialization",
            "invoker.invocable().parameterTypes() == parameterTypes" })
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
    public Object invocableInvoker(InvocableInvoker invoker, Object target, Object[] arguments,
            @Cached(value = "invoker.invocable().parameterTypes()", dimensions = 1) final Class<?>[] parameterTypes,
            @Cached("apply(invoker.invocable())") CallAdapter callAdapter) {
        CompilerAsserts.compilationConstant(invoker);
        Object theObject = invoker.getRealTarget(target);
        OrcJavaCompatibility.Invocable invocable = invoker.invocable();

        long jcs = 0;
        if (orc.run.StopWatches.callsEnabled()) {
            jcs = orc.run.StopWatches.javaCallTime().start();
        }
        try {
            if (theObject == null && !invocable.isStatic()) {
                CompilerDirectives.transferToInterpreter();
                throw new NullPointerException(
                        "Instance method called without a target object (i.e. non-static method called on a class)");
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                final Object a = arguments[i];
                final Class<?> cls = parameterTypes[i];
                if (!cls.isPrimitive()) {
                    arguments[i] = cls.cast(orc2javaOpt(a, cls));
                } else {
                    arguments[i] = orc2javaOpt(a, cls);
                }
            }
            long jis = 0;
            if (orc.run.StopWatches.callsEnabled()) {
                jis = orc.run.StopWatches.javaImplTime().start();
            }
            Object r;
            try {
                r = invoker.boxedReturnType().cast(callAdapter.invoke(theObject, arguments));
            } finally {
                if (orc.run.StopWatches.callsEnabled()) {
                    orc.run.StopWatches.javaImplTime().stop(jis);
                }
            }
            return java2orcOpt(r);
        } catch (ExceptionInInitializerError e) {
            exceptionProfile.enter();
            throw new JavaException(e.getCause());
        } catch (Throwable e) {
            exceptionProfile.enter();
            throw new JavaException(e);
        } finally {
            if (orc.run.StopWatches.callsEnabled()) {
                orc.run.StopWatches.javaCallTime().stop(jcs);
            }
        }
    }

    private static boolean isNonevaluableNumber(Object a) {
        return a instanceof scala.math.BigDecimal || a instanceof scala.math.BigInt ||
                a instanceof java.math.BigDecimal || a instanceof java.math.BigInteger;
    }

    private static Object orc2javaOpt(Object a, Class<?> cls) {
        if (isNonevaluableNumber(a)) {
            return orc2javaWithBoundary(a, cls);
        } else {
            return OrcJavaCompatibility.orc2javaAsFixedPrecision(a, cls);
        }
    }

    private static Object java2orcOpt(Object r) {
        if ((!NumericsConfig.preferLong() || !NumericsConfig.preferDouble() ||
                isNonevaluableNumber(r))) {
            return java2orcWithBoundary(r);
        } else {
            return OrcJavaCompatibility.java2orc(r);
        }
    }


    @Specialization(guards = { "KnownSiteSpecialization" })
    public Object javaFieldAssignSite(orc.values.sites.JavaFieldAssignSite.Invoker invoker, Object target, Object[] arguments) {
        long jcs = 0;
        if (orc.run.StopWatches.callsEnabled()) {
            jcs = orc.run.StopWatches.javaCallTime().start();
        }
        try {
            Object v = orc2javaOpt(arguments[0], invoker.componentType());
            long jis = 0;
            if (orc.run.StopWatches.callsEnabled()) {
                jis = orc.run.StopWatches.javaImplTime().start();
            }
            try {
                orc.values.sites.JavaFieldAssignSite self = (orc.values.sites.JavaFieldAssignSite)target;
                callMethodHandleSetter(invoker.mh(), self.theObject(), v);
                return orc.values.Signal$.MODULE$;
            } finally {
                if (orc.run.StopWatches.callsEnabled()) {
                    orc.run.StopWatches.javaImplTime().stop(jis);
                }
            }
        } catch (InvocationTargetException | ExceptionInInitializerError e) {
            exceptionProfile.enter();
            throw new JavaException(e.getCause());
        } catch (Throwable e) {
            exceptionProfile.enter();
            throw new JavaException(e);
        } finally {
            if (orc.run.StopWatches.callsEnabled()) {
                orc.run.StopWatches.javaCallTime().stop(jcs);
            }
        }
    }

    @Specialization(guards = { "KnownSiteSpecialization" })
    public Object javaFieldDerefSite(orc.values.sites.JavaFieldDerefSite.Invoker invoker, Object target, Object[] arguments) {
        long jcs = 0;
        if (orc.run.StopWatches.callsEnabled()) {
            jcs = orc.run.StopWatches.javaCallTime().start();
        }
        try {
            Object r;
            long jis = 0;
            if (orc.run.StopWatches.callsEnabled()) {
                jis = orc.run.StopWatches.javaImplTime().start();
            }
            try {
                orc.values.sites.JavaFieldDerefSite self = (orc.values.sites.JavaFieldDerefSite) target;
                r = callMethodHandleGetter(invoker.mh(), self.theObject());
            } finally {
                if (orc.run.StopWatches.callsEnabled()) {
                    orc.run.StopWatches.javaImplTime().stop(jis);
                }
            }
            return java2orcOpt(r);
        } catch (InvocationTargetException | ExceptionInInitializerError e) {
            exceptionProfile.enter();
            throw new JavaException(e.getCause());
        } catch (Throwable e) {
            exceptionProfile.enter();
            throw new JavaException(e);
        } finally {
            if (orc.run.StopWatches.callsEnabled()) {
                orc.run.StopWatches.javaCallTime().stop(jcs);
            }
        }
    }

    @Specialization(guards = { "KnownSiteSpecialization" })
    public Object javaArrayAssignSite(orc.values.sites.JavaArrayAssignSite.Invoker invoker, Object target, Object[] arguments) {
        long jcs = 0;
        if (orc.run.StopWatches.callsEnabled()) {
            jcs = orc.run.StopWatches.javaCallTime().start();
        }
        try {
            Object v = orc2javaOpt(arguments[0], invoker.componentType());
            long jis = 0;
            if (orc.run.StopWatches.callsEnabled()) {
                jis = orc.run.StopWatches.javaImplTime().start();
            }
            try {
                orc.values.sites.JavaArrayAssignSite self = (orc.values.sites.JavaArrayAssignSite)target;
                callMethodHandleArraySetter(invoker.mh(), self.theArray(), self.index(), v);
                return orc.values.Signal$.MODULE$;
            } finally {
                if (orc.run.StopWatches.callsEnabled()) {
                    orc.run.StopWatches.javaImplTime().stop(jis);
                }
            }
        } catch (InvocationTargetException | ExceptionInInitializerError e) {
            exceptionProfile.enter();
            throw new JavaException(e.getCause());
        } catch (Throwable e) {
            exceptionProfile.enter();
            throw new JavaException(e);
        } finally {
            if (orc.run.StopWatches.callsEnabled()) {
                orc.run.StopWatches.javaCallTime().stop(jcs);
            }
        }
    }

    @Specialization(guards = { "KnownSiteSpecialization" })
    public Object javaArrayDerefSite(orc.values.sites.JavaArrayDerefSite.Invoker invoker, Object target, Object[] arguments) {
        long jcs = 0;
        if (orc.run.StopWatches.callsEnabled()) {
            jcs = orc.run.StopWatches.javaCallTime().start();
        }
        try {
            Object r;
            long jis = 0;
            if (orc.run.StopWatches.callsEnabled()) {
                jis = orc.run.StopWatches.javaImplTime().start();
            }
            try {
                orc.values.sites.JavaArrayDerefSite self = (orc.values.sites.JavaArrayDerefSite) target;
                r = callMethodHandleArrayGetter(invoker.mh(), self.theArray(), self.index());
            } finally {
                if (orc.run.StopWatches.callsEnabled()) {
                    orc.run.StopWatches.javaImplTime().stop(jis);
                }
            }
            return java2orcOpt(r);
        } catch (InvocationTargetException | ExceptionInInitializerError e) {
            exceptionProfile.enter();
            throw new JavaException(e.getCause());
        } catch (Throwable e) {
            exceptionProfile.enter();
            throw new JavaException(e);
        } finally {
            if (orc.run.StopWatches.callsEnabled()) {
                orc.run.StopWatches.javaCallTime().stop(jcs);
            }
        }
    }

    @TruffleBoundary(allowInlining = true)
    private static Object orc2javaWithBoundary(Object v, Class<?> cls) {
        return OrcJavaCompatibility.orc2java(v, cls);
    }

    @TruffleBoundary(allowInlining = true)
    private static Object java2orcWithBoundary(Object v) {
        return OrcJavaCompatibility.java2orc(v);
    }

    private static Object callMethodHandleGetter(MethodHandle mh, Object theObject) throws Throwable {
        return mh.invokeExact(theObject);
    }

    private static void callMethodHandleSetter(MethodHandle mh, Object theObject, Object arg) throws Throwable {
        mh.invokeExact(theObject, arg);
    }

    private static Object callMethodHandleArrayGetter(MethodHandle mh, Object theObject, int arg) throws Throwable {
        return mh.invokeExact(theObject, arg);
    }

    private static void callMethodHandleArraySetter(MethodHandle mh, Object theObject, int arg1, Object arg2) throws Throwable {
        mh.invokeExact(theObject, arg1, arg2);
    }

    @Specialization(guards = { "isPartiallyEvaluable(invoker)", "KnownSiteSpecialization" })
    public Object partiallyEvaluable(DirectInvoker invoker, Object target, Object[] arguments) {
        return invoker.invokeDirect(target, arguments);
    }

    protected static boolean isPartiallyEvaluable(DirectInvoker invoker) {
        return invoker instanceof orc.values.sites.InlinableInvoker;
    }

    @Specialization
    @TruffleBoundary(allowInlining = true, transferToInterpreterOnException = false)
    public Object unknown(DirectInvoker invoker, Object target, Object[] arguments) {
        return invoker.invokeDirect(target, arguments);
    }

    public static InvokerInvokeDirect create(PorcEExecution execution) {
        return InvokerInvokeDirectNodeGen.create(execution);
    }

}
