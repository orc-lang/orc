//
// ParallelismNode.java -- Scala class/trait/object ParallelismNode
// Project PorcE
//
// Created by amp on Nov 10, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.utilities.AssumedValue;

/**
 *
 *
 * @author amp
 */
public interface ParallelismNode {

    /**
     * @return Return true if this node provides a choice.
     */
    boolean isParallelismChoiceNode();

    /**
     * @return Return the number of times this node has executed while isProfiling is true.
     */
    long getExecutionCount();

    /**
     * Increment the execution count.
     *
     * This is intensionally racy. Counts may be lost.
     */
    void incrExecutionCount();

    /**
     * Specify whether this node should allow additional parallelism or not.
     *
     * @param isParallel If true, this node will check queue sizes and create parallelism as needed.
     */
    void setParallel(boolean isParallel);

    /**
     * @return true if this node can create parallelism.
     */
    boolean getParallel();

    RootNode getRootNode();
}
