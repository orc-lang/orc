//
// PorcETypes.java -- Truffle type system PorcETypes
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.Future;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEObject;
import orc.run.porce.runtime.Terminator;

import com.oracle.truffle.api.dsl.TypeSystem;

@TypeSystem({ PorcEUnit.class, PorcEClosure.class, Counter.class, Terminator.class, Future.class, PorcEObject.class })
public class PorcETypes {
    /* Using Truffle's default TypeCheck and TypeCast implementations. */
}
