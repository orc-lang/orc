package orc.orchard.oil;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import orc.orchard.InvalidOilException;

@XmlSeeAlso(value={Bar.class, Call.class, Definitions.class, Null.class, Pull.class, Push.class, Semicolon.class})
public abstract class Expression {
	public abstract orc.ast.oil.Expr unmarshal() throws InvalidOilException;
}