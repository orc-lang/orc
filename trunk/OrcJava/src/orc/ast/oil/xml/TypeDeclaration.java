package orc.ast.oil.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import orc.Config;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.runtime.nodes.Node;
import orc.ast.oil.xml.type.Type;


/**
 * Introduce a type alias.
 * 
 * @author quark
 */
public class TypeDeclaration extends Expression {
	@XmlElement(required=true)
	public Type type;
	@XmlElement(required=true)
	public Expression body;
	
	public TypeDeclaration() {}
	
	public TypeDeclaration(Type type, Expression body) {
		this.type = type;
		this.body = body;
	}

	public String toString() {
		return super.toString() + "(type " + type + " in " + body + ")";
	}
	@Override
	public orc.ast.oil.expression.Expr unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.TypeDecl(type.unmarshal(config), body.unmarshal(config));
	}
}
