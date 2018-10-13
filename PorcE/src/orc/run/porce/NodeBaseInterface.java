//
// NodeBaseInterface.java -- Scala class/trait/object NodeBaseInterface
// Project PorcE
//
// Created by amp on Oct 10, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.ast.hasOptionalVariableName;
import orc.ast.porc.PorcAST;
import scala.Option;

/**
 *
 *
 * @author amp
 */
public interface NodeBaseInterface {

    /**
     * @return The name of the closest containing Porc callable node. This string will be interned.
     */
    String getContainingPorcCallableName();

    /**
     * Set the Porc AST of this node and its children (if they don't yet have a node).
     * @param ast The Porc AST node to use.
     */
    void setPorcAST(PorcAST ast);

    /**
     * @return The Porc AST node associated with this PorcE node.
     */
    Option<PorcAST> porcNode();

    /**
     * @return
     */
    ProfilingScope getProfilingScope();

}