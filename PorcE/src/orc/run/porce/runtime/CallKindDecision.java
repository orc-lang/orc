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

import java.util.ArrayList;
import java.util.regex.Pattern;

import orc.run.porce.Logger;
import orc.run.porce.NodeBase;
import orc.run.porce.PorcERootNode;
import orc.run.porce.SpecializationConfiguration;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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
        private ArrayList<Entry> table;

        /**
         * Build a table containing the specified entries.
         *
         */
        @SuppressWarnings("unchecked")
        public Table(scala.Tuple2<scala.Tuple2<String, String>, CallKindDecision>... entries) {
            table = new ArrayList<Entry>(entries.length);
            add(entries);
        }

        public Table add(@SuppressWarnings("unchecked") scala.Tuple2<scala.Tuple2<String, String>, CallKindDecision>... entries) {
            table.ensureCapacity(table.size() + entries.length);
            for (int i = 0; i < entries.length; i++) {
                table.add(new Entry(entries[i]._1()._1(), entries[i]._1()._2(), entries[i]._2()));
            }
            return this;
        }

        @TruffleBoundary
        public CallKindDecision get(String source, String target) {
            CompilerAsserts.compilationConstant(table);
            if (SpecializationConfiguration.UseExternalCallKindDecision) {
                for (int i = 0; i < table.size(); i++) {
                    Entry e = table.get(i);
                    CompilerAsserts.compilationConstant(e);
                    if (e.source == source && e.target == target ||
                            e.sourceRE.matcher(source).lookingAt() && e.targetRE.matcher(target).lookingAt()) {
                        return e.kind;
                    }
                }
            }
            return ANY;
        }
    }

    private static final class Entry {
        final String source;
        final String target;
        final Pattern sourceRE;
        final Pattern targetRE;
        final CallKindDecision kind;
        public Entry(String source, String target, CallKindDecision kind) {
            this.source = source.intern();
            this.sourceRE = Pattern.compile(source);
            this.target = target.intern();
            this.targetRE = Pattern.compile(target);
            this.kind = kind;
        }
    }
}
