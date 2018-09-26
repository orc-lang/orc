//
// HasCalledRoots.java -- Scala class/trait/object HasCalledRoots
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

import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import org.graalvm.collections.Pair;

public interface HasCalledRoots {
    public CalledRootsProfile getCalledRootsProfile();
    public ProfilingScope getProfilingScope();
    public Set<Pair<HasCalledRoots, PorcERootNode>> getAllCalledRoots();
    public long getTotalCalls();
    public void addCalledRoot(CallTarget t);
}
