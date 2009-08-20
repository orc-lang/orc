package orc.ast.oil.xml;

import javax.xml.bind.annotation.XmlElement;
import orc.Config;
import orc.error.compiletime.CompilationException;

public class Catch extends Expression {
	@XmlElement(required=true)
	public Expression tryExpr;
	@XmlElement(required=true)
	public Definition catchHandler;
	public Catch() {}
	public Catch(Definition catchHandler, Expression tryExpr) {
		this.tryExpr = tryExpr;
		this.catchHandler = catchHandler;
	}
	public String toString() {
		return super.toString() + "try(" + tryExpr.toString() + ") catch" + catchHandler.toString();
	}
	@Override
	public orc.ast.oil.expression.Expr unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.expression.Catch(catchHandler.unmarshal(config), tryExpr.unmarshal(config));
	}
}
