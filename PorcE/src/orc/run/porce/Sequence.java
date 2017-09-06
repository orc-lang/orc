
package orc.run.porce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class Sequence extends Expression {
    @Children
    protected final Expression[] exprs;

    public Sequence(final Expression[] exprs) {
        this.exprs = exprs;
    }

    @Override
    @ExplodeLoop
    public Object execute(final VirtualFrame frame) {
        for (int i = 0; i < exprs.length - 1; i++) {
            final Expression expr = exprs[i];
            expr.executePorcEUnit(frame);
        }
        return exprs[exprs.length - 1].execute(frame);
    }

    private void addExprsToList(final List<Expression> l) {
        Arrays.asList(exprs).forEach((expr) -> {
            if (expr instanceof Sequence) {
                ((Sequence) expr).addExprsToList(l);
            } else {
                l.add(expr);
            }
        });
    }

    /**
     * Smart constructor for Sequence objects.
     */
    public static Expression create(final Expression[] exprs) {
        final List<Expression> l = new ArrayList<>(exprs.length);
        Arrays.asList(exprs).forEach((expr) -> {
            /*if (expr instanceof Sequence) {
                ((Sequence) expr).addExprsToList(l);
            } else*/ {
                l.add(expr);
            }
        });

        assert l.size() > 0;

        if (l.size() == 1) {
            return l.get(0);
        } else {
            return new Sequence(l.toArray(new Expression[l.size()]));
        }
    }
}
