
package orc.run.porce;

import scala.Option;

import orc.ast.porc.PorcAST;

public interface HasPorcNode {
    public Option<PorcAST> porcNode();
}
