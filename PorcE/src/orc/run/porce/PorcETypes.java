
package orc.run.porce;

import com.oracle.truffle.api.dsl.TypeSystem;
import orc.Future;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEObject;
import orc.run.porce.runtime.Terminator;

@TypeSystem({ PorcEUnit.class, PorcEClosure.class, Counter.class, Terminator.class, Future.class, PorcEObject.class })
public class PorcETypes {
}
