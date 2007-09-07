package orc.ast.extended;

import java.util.List;

public class XQuery extends Expression {

	private List<Expression> args;
	public XQuery(List<Expression> args)
	{
		this.args = args;
	}
	@Override
	public orc.ast.simple.Expression simplify() {
		// TODO Auto-generated method stub
		return null;
	}

}
