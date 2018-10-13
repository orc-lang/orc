//
// ProfilingScope.java -- Scala class/trait/object ProfilingScope
// Project PorcE
//
// Created by amp on Sep 25, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

/**
 * A profiling scopes defines a region which is profiled as a unit.
 *
 * @author amp
 */
public interface ProfilingScope {
    /**
     * @return true, if this scope should capture the profile.
     *
     * Do not use this in specialization choices!!! The returned value may
     * depend on whether code is compiled.
     */
    boolean isProfiling();

    /**
     * @return true, if this scope has completed profiling for the moment and optimization which would invalidate profiling can be applied.
     *
     * The returned value may not depend on whether the code is compiled.
     */
    boolean isProfilingComplete();

    // Get profiling results:

    /**
     * @return The total time spent in spawned calls.
     */
//    long getTotalSpawnedTime();

    /**
     * @return The total number of spawned calls.
     */
//    long getTotalSpawnedCalls();

    /**
     * @return The total time spent in this root node (excluding callees).
     */
    long getTotalTime();

    /**
     * @return The total number of calls.
     */
    long getTotalCalls();

    long getSiteCalls();

    long getContinuationSpawns();

    // Updating profiling counters:

    /**
     * @return The time which can be used as a start time for other timing calls.
     */
    long getTime();

    /**
     * Add time to the total spawned time and increment the number of spawned calls.
     *
     * @param start
     *            The start time of the range
     */
//    void addSpawnedCall(long start);

    /**
     * Add time to the total time and increment the number of calls.
     *
     * @param start
     *            The start time of the range
     */
    void addTime(long start);

    /**
     * Remove time from the total time. This is used for eliminating the time of called continuations.
     *
     * @param start
     *            The start time of the range
     */
    void removeTime(long start);

    /**
     * Increment the site call count.
     */
    void incrSiteCall();

    /**
     * Increment the continuation spawn count.
     */
    void incrContinuationSpawn();

    /**
     * A no-op implementation of ProfilingScope used when no other implementation is available or profiling is turned
     * off.
     *
     * @author amp
     */
    static public final class ProfilingScopeDisabled implements ProfilingScope {
        @Override
        public boolean isProfiling() {
            return false;
        }

        @Override
        public boolean isProfilingComplete() {
            return true;
        }

        @Override
        public long getTotalCalls() {
            return 0;
        }

        @Override
        public long getTime() {
            return 0;
        }

        @Override
        public void addTime(long start) {
        }

        @Override
        public void removeTime(long start) {
        }

        @Override
        public void incrSiteCall() {
        }

        @Override
        public void incrContinuationSpawn() {
        }

        @Override
        public long getTotalTime() {
            return 0;
        }

        @Override
        public long getSiteCalls() {
            return 0;
        }

        @Override
        public long getContinuationSpawns() {
            return 0;
        }
    }

    static public final ProfilingScope DISABLED = new ProfilingScopeDisabled();
}