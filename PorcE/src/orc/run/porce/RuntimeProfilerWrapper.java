//
// RuntimeProfilerWrapper.java -- Java wrapper class RuntimeProfilerWrapper
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.RuntimeProfiler;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public final class RuntimeProfilerWrapper {
	public static final boolean profileRuntime = RuntimeProfiler.profileRuntime();

	public static final long CallDispatch = RuntimeProfiler.CallDispatch();
	public static final long SiteImplementation = RuntimeProfiler.SiteImplementation();
	public static final long JavaDispatch = RuntimeProfiler.JavaDispatch();

	public static String regionToString(Long region) {
		return RuntimeProfiler.regionToString(region);
	}

	public static void traceEnter(long region) {
		if (profileRuntime) {
			Boundaries.traceEnter(region, -1);
		}
	}

	public static void traceEnter(long region, int id) {
		if (profileRuntime) {
			Boundaries.traceEnter(region, id);
		}
	}

	public static void traceExit(long region) {
		if (profileRuntime) {
			Boundaries.traceExit(region, -1);
		}
	}

	public static void traceExit(long region, int id) {
		if (profileRuntime) {
			Boundaries.traceExit(region, id);
		}
	}

	public static class Boundaries {
		@TruffleBoundary(allowInlining = true)
		static void traceEnter(long region, int id) {
			RuntimeProfiler.traceEnter(region, id);
		}

		@TruffleBoundary(allowInlining = true)
		static void traceExit(long region, int id) {
			RuntimeProfiler.traceExit(region, id);
		}
	}
}
