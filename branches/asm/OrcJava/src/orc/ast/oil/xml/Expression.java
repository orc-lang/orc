package orc.ast.oil.xml;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlSeeAlso;

import orc.Config;
import orc.error.compiletime.CompilationException;

@XmlSeeAlso(value={Bar.class, Call.class, Definitions.class, Silent.class, Pull.class, Push.class, Semicolon.class, WithLocation.class, TypeAscription.class, TypeDeclaration.class})
public abstract class Expression implements Serializable {
	public abstract orc.ast.oil.Expr unmarshal(Config config) throws CompilationException;
}