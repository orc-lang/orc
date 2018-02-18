
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
