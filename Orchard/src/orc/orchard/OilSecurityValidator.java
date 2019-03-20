//
// OilSecurityValidator.java -- Java class OilSecurityValidator
// Project Orchard
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import scala.collection.JavaConversions;

import orc.ast.AST;
import orc.ast.oil.nameless.Constant;
import orc.ast.oil.nameless.NamelessAST;
import orc.compile.parse.OrcSourceRange;
import orc.error.compiletime.CompileLogger.Severity;
import orc.orchard.errors.OrcProgramProblem;

/**
 * Check an OIL expression for security violations.
 *
 * @author quark
 */
public class OilSecurityValidator {
    private boolean hasProblems = false;
    private final List<SecurityProblem> problems = new LinkedList<>();

    public boolean hasProblems() {
        return hasProblems;
    }

    public List<SecurityProblem> getProblems() {
        return problems;
    }

    private static Set<String> allowedClasses;
    static {
        allowedClasses = new HashSet<>(); // a Trie might be more efficient if it were standard

        // java.lang
        allowedClasses.add("java.lang.StrictMath");
        allowedClasses.add("java.lang.Math");
        allowedClasses.add("java.lang.Boolean");
        allowedClasses.add("java.lang.Byte");
        allowedClasses.add("java.lang.Character");
        allowedClasses.add("java.lang.Short");
        allowedClasses.add("java.lang.Integer");
        allowedClasses.add("java.lang.Long");
        allowedClasses.add("java.lang.Double");
        allowedClasses.add("java.lang.Float");
        allowedClasses.add("java.lang.String");
        allowedClasses.add("java.lang.StringBuffer");
        allowedClasses.add("java.lang.StringBuilder");

        // java.util
        allowedClasses.add("java.util.ArrayList");
        allowedClasses.add("java.util.Arrays");
        allowedClasses.add("java.util.BitSet");
        allowedClasses.add("java.util.Calendar");
        allowedClasses.add("java.util.Collections");
        allowedClasses.add("java.util.Currency");
        allowedClasses.add("java.util.GregorianCalendar");
        allowedClasses.add("java.util.HashMap");
        allowedClasses.add("java.util.HashSet");
        allowedClasses.add("java.util.Hashtable");
        allowedClasses.add("java.util.IdentityHashMap");
        allowedClasses.add("java.util.LinkedHashMap");
        allowedClasses.add("java.util.LinkedHashSet");
        allowedClasses.add("java.util.LinkedList");
        allowedClasses.add("java.util.PriorityQueue");
        allowedClasses.add("java.util.Random");
        allowedClasses.add("java.util.Stack");
        allowedClasses.add("java.util.StringTokenizer");
        allowedClasses.add("java.util.TreeMap");
        allowedClasses.add("java.util.TreeSet");
        allowedClasses.add("java.util.Vector");

        // java.util.concurrent
        allowedClasses.add("java.util.concurrent.BlockingDeque");
        allowedClasses.add("java.util.concurrent.BlockingQueue");
        allowedClasses.add("java.util.concurrent.ConcurrentMap");
        allowedClasses.add("java.util.concurrent.ConcurrentNavigableMap");
        allowedClasses.add("java.util.concurrent.ArrayBlockingQueue");
        allowedClasses.add("java.util.concurrent.ConcurrentHashMap");
        allowedClasses.add("java.util.concurrent.ConcurrentHashMap.KeySetView");
        allowedClasses.add("java.util.concurrent.ConcurrentLinkedDeque");
        allowedClasses.add("java.util.concurrent.ConcurrentLinkedQueue");
        allowedClasses.add("java.util.concurrent.ConcurrentSkipListMap");
        allowedClasses.add("java.util.concurrent.ConcurrentSkipListSet");
        allowedClasses.add("java.util.concurrent.CopyOnWriteArrayList");
        allowedClasses.add("java.util.concurrent.CopyOnWriteArraySet");
        allowedClasses.add("java.util.concurrent.CountDownLatch");
        allowedClasses.add("java.util.concurrent.CyclicBarrier");
        allowedClasses.add("java.util.concurrent.DelayQueue");
        allowedClasses.add("java.util.concurrent.Exchanger");
        allowedClasses.add("java.util.concurrent.LinkedBlockingDeque");
        allowedClasses.add("java.util.concurrent.LinkedBlockingQueue");
        allowedClasses.add("java.util.concurrent.LinkedTransferQueue");
        allowedClasses.add("java.util.concurrent.PriorityBlockingQueue");
        allowedClasses.add("java.util.concurrent.Semaphore");
        allowedClasses.add("java.util.concurrent.SynchronousQueue");

        // orc.lib
        allowedClasses.add("orc.lib.state.Set");
        allowedClasses.add("orc.lib.state.Map");
        allowedClasses.add("orc.lib.state.Interval");
        allowedClasses.add("orc.lib.state.Intervals");
        allowedClasses.add("orc.lib.net.Upcoming");
        allowedClasses.add("orc.lib.net.Geocoder");
        allowedClasses.add("orc.lib.net.GoogleCalendar");
        allowedClasses.add("orc.lib.net.NOAAWeather");

        // orc.lib.orchard
        allowedClasses.add("orc.lib.orchard.OrcVersion");
        allowedClasses.add("orc.lib.orchard.forms.Form");
        allowedClasses.add("orc.lib.orchard.forms.Textbox");
        allowedClasses.add("orc.lib.orchard.forms.Textarea");
        allowedClasses.add("orc.lib.orchard.forms.Checkbox");
        allowedClasses.add("orc.lib.orchard.forms.Button");
        allowedClasses.add("orc.lib.orchard.forms.IntegerField");
        allowedClasses.add("orc.lib.orchard.forms.UploadField");
        allowedClasses.add("orc.lib.orchard.forms.FormInstructions");
        allowedClasses.add("orc.lib.orchard.forms.PasswordField");
        allowedClasses.add("orc.lib.orchard.forms.Mandatory");
        allowedClasses.add("orc.lib.orchard.forms.FieldGroup");
        allowedClasses.add("orc.lib.orchard.forms.DateField");
        allowedClasses.add("orc.lib.orchard.forms.DateTimeRangesField");

        // org.joda.time
        allowedClasses.add("org.joda.time.DateTime");
        allowedClasses.add("org.joda.time.LocalDateTime");
        allowedClasses.add("org.joda.time.LocalDate");
        allowedClasses.add("org.joda.time.format.DateTimeFormat");
        allowedClasses.add("org.joda.time.format.ISODateTimeFormat");

        // scala.collection
        allowedClasses.add("scala.collection.BitSet");
        allowedClasses.add("scala.collection.Map");
        allowedClasses.add("scala.collection.Set");
        allowedClasses.add("scala.collection.SortedMap");
        allowedClasses.add("scala.collection.SortedSet");

        allowedClasses.add("scala.collection.immutable.BitSet");
        allowedClasses.add("scala.collection.immutable.HashMap");
        allowedClasses.add("scala.collection.immutable.HashSet");
        allowedClasses.add("scala.collection.immutable.IntMap");
        allowedClasses.add("scala.collection.immutable.List");
        allowedClasses.add("scala.collection.immutable.ListMap");
        allowedClasses.add("scala.collection.immutable.LongMap");
        allowedClasses.add("scala.collection.immutable.Map");
        allowedClasses.add("scala.collection.immutable.Queue");
        allowedClasses.add("scala.collection.immutable.Set");
        allowedClasses.add("scala.collection.immutable.Stack");
        allowedClasses.add("scala.collection.immutable.SortedMap");
        allowedClasses.add("scala.collection.immutable.SortedSet");
        allowedClasses.add("scala.collection.immutable.TreeHashMap");
        allowedClasses.add("scala.collection.immutable.TreeMap");
        allowedClasses.add("scala.collection.immutable.TreeSet");
        allowedClasses.add("scala.collection.immutable.Vector");

        allowedClasses.add("scala.collection.mutable.ArrayBuffer");
        allowedClasses.add("scala.collection.mutable.ArrayStack");
        allowedClasses.add("scala.collection.mutable.BitSet");
        allowedClasses.add("scala.collection.mutable.Buffer");
        allowedClasses.add("scala.collection.mutable.DoubleLinkedList");
        allowedClasses.add("scala.collection.mutable.HashMap");
        allowedClasses.add("scala.collection.mutable.HashSet");
        allowedClasses.add("scala.collection.mutable.LinkedHashMap");
        allowedClasses.add("scala.collection.mutable.LinkedHashSet");
        allowedClasses.add("scala.collection.mutable.LinkedList");
        allowedClasses.add("scala.collection.mutable.ListBuffer");
        allowedClasses.add("scala.collection.mutable.ListMap");
        allowedClasses.add("scala.collection.mutable.Map");
        allowedClasses.add("scala.collection.mutable.MutableList");
        allowedClasses.add("scala.collection.mutable.Queue");
        allowedClasses.add("scala.collection.mutable.Set");
        allowedClasses.add("scala.collection.mutable.Stack");
        allowedClasses.add("scala.collection.mutable.WeakHashMap");
    }

    public void validate(final NamelessAST astNode) {
        for (final AST node : JavaConversions.asJavaIterable(astNode.subtrees())) {
            final NamelessAST child = (NamelessAST) node;
            if (child instanceof Constant) {
                final Object value = ((Constant) child).value();
                if (!(value instanceof java.lang.Number ||
                        value instanceof java.lang.Boolean ||
                        value instanceof java.lang.Character ||
                        value instanceof java.lang.Enum<?> ||
                        value instanceof java.lang.Math ||
                        value instanceof java.lang.CharSequence ||
                        value instanceof java.lang.Void ||
                        value instanceof orc.values.sites.Site)) {
                    final String className = value.getClass().getName();
                    if (!allowedClasses.contains(className) && !allowedClasses.contains(className + "$")) {
                        hasProblems = true;
                        problems.add(new SecurityProblem("Security: Access denied to Java class '" + className + "'.", node.sourceTextRange().isDefined() ? node.sourceTextRange().get() : null));
                    }
                }
            }
            validate(child);
        }
    }

    public static class SecurityProblem extends OrcProgramProblem {

        protected SecurityProblem() {
            super();
        }

        public SecurityProblem(final String message, final OrcSourceRange position) {
            super();
            this.severity = Severity.ERROR.ordinal();
            this.message = message;
            this.pathname = position != null ? position.start().resource().descr() : "";
            this.line = position != null ? position.start().line() : -1;
            this.column = position != null ? position.start().column() : -1;
            if (position != null) {
                this.longMessage = position.toString() + ": " + message;
            } else {
                this.longMessage = message;
            }
        }

    }

}
