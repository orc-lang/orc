package orc.orchard.oil;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlSeeAlso;
import orc.orchard.errors.InvalidOilException;

@XmlSeeAlso(value={Bar.class, Call.class, Definitions.class, Null.class, Pull.class, Push.class, Semicolon.class})
public abstract class Expression implements Serializable {
	public abstract orc.ast.oil.Expr unmarshal() throws InvalidOilException;
}