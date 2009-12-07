package orc.ast.xml.expression;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.Config;
import orc.error.compiletime.CompilationException;

@XmlSeeAlso(value={Parallel.class, Call.class, DeclareDefs.class, Stop.class, Pruning.class, Sequential.class, Otherwise.class, WithLocation.class, HasType.class, DeclareType.class})
public abstract class Expression implements Serializable {
	public abstract orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException;
}