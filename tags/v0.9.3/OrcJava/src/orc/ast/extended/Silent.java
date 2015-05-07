package orc.ast.extended;

public class Silent extends Expression {

	@Override
	public orc.ast.simple.Expression simplify() {
		
		return new orc.ast.simple.Silent();
	}
	public String toString() {
		return ".";
	}
}
