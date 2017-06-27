package orc.run.porce;

import com.oracle.truffle.api.dsl.TypeSystem;

import orc.Future;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.PorcEClosure;

@TypeSystem({PorcEUnit.class, PorcEClosure.class, Counter.class, Terminator.class, Future.class})
public class PorcETypes {

}
