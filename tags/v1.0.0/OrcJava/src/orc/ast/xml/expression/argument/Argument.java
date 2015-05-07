package orc.ast.xml.expression.argument;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.Config;
import orc.ast.xml.expression.Expression;
import orc.error.compiletime.CompilationException;

/**
 * Arguments to sites and expressions.
 * @author quark
 */
@XmlSeeAlso(value={Constant.class, Field.class, Site.class, Variable.class})
public abstract class Argument extends Expression {
	@Override
	public abstract orc.ast.oil.expression.argument.Argument unmarshal(Config config) throws CompilationException;
}
