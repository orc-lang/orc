package orc.orchard.oil;

public class Constant extends Value {
	public Object value;
	public Constant() {}
	public Constant(Object value) {
		this.value = value;
	}
	public String toString() {
		return super.toString() + "(" + value.getClass().toString() + "(" + value + "))";
	}
	@Override
	public orc.ast.oil.arg.Arg unmarshal() {
		return new orc.ast.oil.arg.Constant(value);
	}
}
