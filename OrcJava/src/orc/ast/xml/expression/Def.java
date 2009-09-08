package orc.ast.xml.expression;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.ast.xml.type.Type;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * FIXME: should include type info
 * @author quark
 */
public class Def implements Serializable {
	@XmlAttribute(required=true)
	public int arity;
	@XmlElement(required=true)
	public Expression body;
	public SourceLocation location;
	@XmlAttribute(required=false)
	public String name;
	@XmlAttribute(required=true)
	public int typeArity;
	@XmlElementWrapper(required=false)
	@XmlElement(name="argType")
	public Type[] argTypes;
	@XmlElement(required=false)
	public Type resultType;
	public Def() {}
	public Def(int arity, Expression body, int typeArity, Type[] argTypes, Type resultType, SourceLocation location, String name) {
		this.arity = arity;
		this.body = body;
		this.typeArity = typeArity;
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.location = location;
		this.name = name;
	}
	public String toString() {
		return "(" + super.toString() + "(" + arity + ") = " + body + ")";
	}
	public orc.ast.oil.expression.Def unmarshal(Config config) throws CompilationException {
		
		List<orc.ast.oil.type.Type> newArgTypes = Type.unmarshalAll(argTypes);
		orc.ast.oil.type.Type newResultType = (resultType != null ? resultType.unmarshal() : null);
		
		return new orc.ast.oil.expression.Def(arity, body.unmarshal(config), typeArity,
				newArgTypes, newResultType, location, name);
	}
}
