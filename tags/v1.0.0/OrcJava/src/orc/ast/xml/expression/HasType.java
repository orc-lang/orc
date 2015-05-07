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
 * 
 * An expression with an ascribed type.
 * 
 * @author quark
 *
 */
public class HasType extends Expression {
	@XmlElement(required=true)
	public Expression body;
	@XmlElement(required=true)
	public Type type;
	@XmlAttribute(required=true)
	public boolean checked;
	
	public HasType() {}
	
	public HasType(Expression body, Type type, boolean checked) {
		this.body = body;
		this.type = type;
		this.checked = checked;
	}

	public String toString() {
		return super.toString() + "(" + body + " :: " + type + ")";
	}
	@Override
	public orc.ast.oil.expression.Expression unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.HasType(body.unmarshal(config), type.unmarshal(), checked);
	}
}
