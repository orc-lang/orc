package orc.orchard.interfaces;

import java.io.Serializable;

import orc.ast.simple.Expression;

public interface Oil extends Serializable {
	public Expression getExpression();
}