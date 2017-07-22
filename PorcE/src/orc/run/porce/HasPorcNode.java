package orc.run.porce;

import orc.ast.porc.PorcAST;
import scala.Option;

public interface HasPorcNode {
	public Option<PorcAST> porcNode();
}
