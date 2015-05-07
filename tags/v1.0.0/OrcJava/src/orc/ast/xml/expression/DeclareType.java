package orc.ast.xml.expression;

import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.ast.xml.type.Type;


/**
 * Introduce a type alias.
 * 
 * @author quark
 */
public class DeclareType extends Expression {
	@XmlElement(required=true)
	public Type type;
	@XmlElement(required=true)
	public Expression body;
	
	public DeclareType() {}
	
	public DeclareType(Type type, Expression body) {
		this.type = type;
		this.body = body;
	}

	public String toString() {
		return super.toString() + "(type " + type + " in " + body + ")";
	}
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.DeclareType(type.unmarshal(), body.unmarshal(config));
	}
}
