//
// SpecializationConfiguration.java -- Java static class SpecializationConfiguration
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public abstract class SpecializationConfiguration {
    public static abstract class StopWatches {
        public static final boolean workerEnabled = orc.run.StopWatches.workerEnabled();
        public static final boolean callsEnabled = orc.run.StopWatches.callsEnabled();
    }

    /*static {
        if (System.getProperty("orc.porce.truffleASTInlining") != null) {
            throw new Error("truffleASTInlining is disabled in the source code. See InternalCPSDispatch.java.");
        }
    }*/

    @CompilationFinal
    public static final boolean TruffleASTInlining = Boolean
                .parseBoolean(System.getProperty("orc.porce.truffleASTInlining", "false"));

    @CompilationFinal
    public static final int TruffleASTInliningLimit = Integer
                .parseInt(System.getProperty("orc.porce.truffleASTInliningLimit", "600"));

    @CompilationFinal
    public static final int GetFieldMaxCacheSize = Integer
            .parseInt(System.getProperty("orc.porce.cache.getFieldMaxCacheSize", "4"));

    @CompilationFinal
    public static final int InternalCallMaxCacheSize = Integer
            .parseInt(System.getProperty("orc.porce.cache.internalCallMaxCacheSize", "6"));
    @CompilationFinal
    public static final int ExternalDirectCallMaxCacheSize = Integer
            .parseInt(System.getProperty("orc.porce.cache.externalDirectCallMaxCacheSize", "4"));
    @CompilationFinal
    public static final int ExternalCPSCallMaxCacheSize = Integer
            .parseInt(System.getProperty("orc.porce.cache.externalCPSCallMaxCacheSize", "4"));

    /**
     * The maximum number of ns a functions may take and still have the spawn inlined.
     *
     * The system property is in floating-point ms.
     */
    @CompilationFinal
    public static final int InlineAverageTimeLimit = (int) (Double
            .parseDouble(System.getProperty("orc.porce.inlineAverageTimeLimit", "0.1")) * 1000000);

    /**
     * The number of calls before computing the time per call.
     *
     * This is computed from graal.TruffleCompilationThreshold if that is set to a small value (less than 105).
     * Otherwise it defaults to 100.
     */
    @CompilationFinal
    public static final int MinCallsForTimePerCall;

    static {
        int v = -1;
        v = Integer.parseInt(System.getProperty("orc.porce.cache.minCallsForTimePerCall", "-1"));
        if (v < 0) {
            v = Math.min(Integer.parseInt(System.getProperty("graal.TruffleCompilationThreshold", "-1")) - 5, 100);
        }
        if (v < 0) {
            v = 100;
        }
        MinCallsForTimePerCall = v;
        System.setProperty("orc.porce.cache.minCallsForTimePerCall", Integer.toString(MinCallsForTimePerCall));
    }

    @CompilationFinal
    public static final boolean UniversalTCO = Boolean
            .parseBoolean(System.getProperty("orc.porce.universalTCO", "false"));

    @CompilationFinal
    public static final boolean SelfTCO = Boolean.parseBoolean(System.getProperty("orc.porce.selfTCO", "true"));

    @CompilationFinal
    public static final boolean InlineForceResolved = Boolean
            .parseBoolean(System.getProperty("orc.porce.optimizations.inlineForceResolved", "true"));

    @CompilationFinal
    public static final boolean InlineForceHalted = Boolean
            .parseBoolean(System.getProperty("orc.porce.optimizations.inlineForceHalted", "true"));

    @CompilationFinal
    public static final boolean SpecializeOnCounterStates = Boolean
            .parseBoolean(System.getProperty("orc.porce.optimizations.specializeOnCounterStates", "true"));

    @CompilationFinal
    public static final boolean ExternalCPSDirectSpecialization = Boolean
            .parseBoolean(System.getProperty("orc.porce.optimizations.externalCPSDirectSpecialization", "true"));

    @CompilationFinal
    public static final boolean KnownSiteSpecialization = Boolean
            .parseBoolean(System.getProperty("orc.porce.optimizations.knownSiteSpecialization", "true"));

    @CompilationFinal
    public static final boolean EnvironmentCaching = Boolean
            .parseBoolean(System.getProperty("orc.porce.optimizations.environmentCaching", "true"));

    @CompilationFinal
    public static final double MinimumEarlyHaltProbability = Double
            .parseDouble(System.getProperty("orc.porce.minimumEarlyHaltProbability", "0.5"));

    @CompilationFinal
    public static final boolean UseExternalCallKindDecision = Boolean
            .parseBoolean(System.getProperty("orc.porce.useExternalCallKindDecision", "false"));

    @CompilationFinal
    public static final boolean ProfileCallGraph = Boolean
            .parseBoolean(System.getProperty("orc.porce.profileCallGraph", "false"));

    @CompilationFinal
    public static final boolean ProfileFunctionTime = Boolean
            .parseBoolean(System.getProperty("orc.porce.profileFunctionTime", "false"));

    static {
        if(ProfileFunctionTime && !ProfileCallGraph) {
            Logger.warning(() -> "orc.porce.profileFunctionTime requires orc.porce.profileCallGraph, but profileCallGraph=false");
        }
    }

    @CompilationFinal
    public static final boolean InitiallyParallel = Boolean
            .parseBoolean(System.getProperty("orc.porce.initiallyParallel", "true"));

    @CompilationFinal
    public static final boolean UseControlledParallelism = Boolean
            .parseBoolean(System.getProperty("orc.porce.useControlledParallelism", "false"));

    static {
        if(UseExternalCallKindDecision && UseControlledParallelism) {
            Logger.warning(() -> "Both orc.porce.useControlledParallelism and orc.porce.useExternalCallKindDecision are set. orc.porce.useControlledParallelism wins.");
        }
        String parallelismMode;
        if (UseControlledParallelism) {
            parallelismMode = "execution count ordering heuristic";
        } else if (UseExternalCallKindDecision) {
            parallelismMode = "external table";
        } else {
            parallelismMode = "combined runtime heuristic (old and noisy!!)";
        }
        Logger.info(() -> "Parallelism mode is: " + parallelismMode);
    }

    @CompilationFinal
    public static final long MinimumExecutionCountForParallelismController = Long
            .parseLong(System.getProperty("orc.porce.minimumExecutionCountForParallelismController", "100000"));
}
