//
// EvalSite.java -- Scala class/trait/object EvalSite
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 2, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility;

import orc.TokenAPI;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.type.Type;

/**
 * 
 *
 * @author jthywiss
 */
public abstract class EvalSite extends SiteAdaptor {

  @Override
  public void callSite(Args args, TokenAPI caller) throws TokenException {
    caller.publish(object2value(evaluate(args)));
  }

  public Type type() throws TypeException {
      return Type.BOT;
  }

  public abstract Object evaluate(Args args) throws TokenException;
}
