//
// HasPorcNode.java -- Java interface HasPorcNode
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import scala.Option;

import orc.ast.porc.PorcAST;

public interface HasPorcNode {
    public Option<PorcAST.Z> porcNode();
}
