package orc.ast.oil.xml;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlSeeAlso;

@XmlSeeAlso(value={Bar.class, Call.class, Definitions.class, Silent.class, Pull.class, Push.class, Semicolon.class, WithLocation.class})
public abstract class Expression implements Serializable {
	public abstract orc.ast.oil.Expr unmarshal();
}