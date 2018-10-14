//
// CallKindDecision.java -- Scala class/trait/object CallKindDecision
// Project PorcE
//
// Created by amp on Oct 10, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime;

import orc.run.porce.Logger;
import orc.run.porce.NodeBase;
import orc.run.porce.PorcERootNode;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ExplodeLoop;

/**
 *
 *
 * @author amp
 */
public enum CallKindDecision {
    INLINE,
    CALL,
    SPAWN,
    ANY;

    public static CallKindDecision get(NodeBase source, PorcERootNode target) {
        String sourceName = source.getContainingPorcCallableName();
        String targetName = target.getContainingPorcCallableName();

        PorcEExecution execution = target.getExecution();

        Table table = execution.callKindTable();

        CallKindDecision decision = table.get(sourceName, targetName);
        Logger.finest(() -> String.format("Found (%s [%s], %s [%s]) -> %s", sourceName, source, targetName, target, decision));
        return decision;
    }

    public static class Table {
        @CompilationFinal(dimensions=1)
        private Entry[] table;

        /**
         * Build a table containing the specified entries.
         *
         */
        @SuppressWarnings("unchecked")
        public Table(scala.Tuple2<scala.Tuple2<String, String>, CallKindDecision>... entries) {
            table = new Entry[entries.length];
            for (int i = 0; i < entries.length; i++) {
                table[i] = new Entry(entries[i]._1()._1(), entries[i]._1()._2(), entries[i]._2());
            }
        }

        @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
        public CallKindDecision get(String source, String target) {
            CompilerAsserts.compilationConstant(table);
            for (int i = 0; i < table.length; i++) {
                Entry e = table[i];
                CompilerAsserts.compilationConstant(e);
                if (e.source == source && e.target == target) {
                    return e.kind;
                }
            }
            return ANY;
        }
    }

    private static class Entry {
        final String source;
        final String target;
        final CallKindDecision kind;
        public Entry(String source, String target, CallKindDecision kind) {
            this.source = source.intern();
            this.target = target.intern();
            this.kind = kind;
        }
    }
}
