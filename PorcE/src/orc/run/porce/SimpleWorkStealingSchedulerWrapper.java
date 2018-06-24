//
// SimpleWorkStealingSchedulerWrapper.java -- Java wrapper class SimpleWorkStealingSchedulerWrapper
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.extensions.SimpleWorkStealingScheduler;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public abstract class SimpleWorkStealingSchedulerWrapper {
    public static final boolean traceTasks = SimpleWorkStealingScheduler.traceTasks();
    public static final SimpleWorkStealingScheduler.SchedulableExecutionType SchedulerExecution = 
	    orc.run.extensions.SimpleWorkStealingScheduler.SchedulerExecution$.MODULE$;
    public static final SimpleWorkStealingScheduler.SchedulableExecutionType StackExecution = 
	    orc.run.extensions.SimpleWorkStealingScheduler.StackExecution$.MODULE$;
    public static final SimpleWorkStealingScheduler.SchedulableExecutionType InlineExecution = 
	    orc.run.extensions.SimpleWorkStealingScheduler.InlineExecution$.MODULE$;

  public static void shareSchedulableID(Object d, Object s) {
    if (traceTasks) {
      Boundaries.shareSchedulableID(d, s);
    }
  }

  public static long getSchedulableID(Object s) {
    if (traceTasks) {
      return Boundaries.getSchedulableID(s);
    } else {
      return 0;
    }
  }

  public static void traceTaskParent(Object parent, Object child) {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child);
    }
  }

  /*
  def traceTaskParent(parent: AnyRef, child: Long): Unit = {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child)
    }
  }*/

  public static void traceTaskParent(long parent, long child) {
    if (traceTasks) {
      Boundaries.traceTaskParent(parent, child);
    }
  }

  public static void enterSchedulable(Object s, SimpleWorkStealingScheduler.SchedulableExecutionType t) {
    if (traceTasks) {
      Boundaries.enterSchedulable(s, t);
    }
  }

  public static long enterSchedulableInline() {
    if (traceTasks) {
      return Boundaries.enterSchedulableInline();
    } else {
	return 0;
    }
  }

  public static Object currentSchedulable() {
    if (traceTasks) {
      return Boundaries.currentSchedulable();
    } else {
      return null;
    }
  }

  public static void exitSchedulable(Object s) {
    if (traceTasks) {
      Boundaries.exitSchedulable(s);
    }
  }

  public static void exitSchedulable(Object s, Object old) {
    if (traceTasks) {
      Boundaries.exitSchedulable(s, old);
    }
  }

  public static void exitSchedulable(long s, Object old) {
    if (traceTasks) {
      Boundaries.exitSchedulable(s, old);
    }
  }

  static abstract class Boundaries {
    @TruffleBoundary
    public static void shareSchedulableID(Object d, Object s) {
      SimpleWorkStealingScheduler.shareSchedulableID(d, s);
    }

    @TruffleBoundary
    public static long getSchedulableID(Object s) {
      return SimpleWorkStealingScheduler.getSchedulableID(s);
    }

    @TruffleBoundary
    public static void traceTaskParent(Object parent, Object child) {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child);
    }


    /*@TruffleBoundary
    def traceTaskParent(parent: AnyRef, child: Long): Unit = {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child)
    }*/

    @TruffleBoundary
    public static void traceTaskParent(long parent, long child) {
      SimpleWorkStealingScheduler.traceTaskParent(parent, child);
    }

    @TruffleBoundary
    public static void enterSchedulable(Object s, SimpleWorkStealingScheduler.SchedulableExecutionType t) {
      SimpleWorkStealingScheduler.enterSchedulable(s, t);
    }

    @TruffleBoundary
    public static long enterSchedulableInline() {
      long s = SimpleWorkStealingScheduler.newSchedulableID();
      SimpleWorkStealingScheduler.traceTaskParent(SimpleWorkStealingScheduler.currentSchedulable(), s);
      SimpleWorkStealingScheduler.enterSchedulable(s, InlineExecution);
      return s;
    }

    @TruffleBoundary
    public static Object currentSchedulable() {
      return SimpleWorkStealingScheduler.currentSchedulable();
    }

    @TruffleBoundary
    public static void exitSchedulable(Object s) {
      SimpleWorkStealingScheduler.exitSchedulable(s);
    }

    @TruffleBoundary
    public static void exitSchedulable(Object s, Object old) {
      SimpleWorkStealingScheduler.exitSchedulable(s, old);
    }
    
    @TruffleBoundary
    public static void exitSchedulable(long s, Object old) {
      SimpleWorkStealingScheduler.exitSchedulable(s, old);
    }
  }
}
