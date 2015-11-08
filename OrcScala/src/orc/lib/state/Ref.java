//
// Ref.java -- Java class Ref
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.LinkedList;
import java.util.Queue;

import orc.Handle;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.RefType;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Rewritable mutable reference. The reference can be initialized with a value,
 * or left initially empty. Read operations block if the reference is empty.
 * Write operations always succeed.
 *
 * @author dkitchin
 */
public class Ref extends EvalSite implements TypedSite {

	@Override
	public Object evaluate(final Args args) throws TokenException {

		// If we were passed an argument, store it in the ref as an initial
		// value
		if (args.size() == 1) {
			return new RefInstance(args.getArg(0));
		}
		// With no args, create an initially empty reference
		else if (args.size() == 0) {
			return new RefInstance();
		} else {
			throw new ArityMismatchException(1, args.size());
		}
	}

	@Override
	public Type orcType() {
		return RefType.getBuilder();
	}

	public static class RefInstance extends DotSite {

		protected Queue<Handle> readQueue;
		Object contents;

		public RefInstance() {
			this.contents = null;

			/*
			 * Note that the readQueue also signals whether the reference has
			 * been assigned. If it is non-null (as it is initially), the
			 * reference is unassigned. If it is null, the reference has been
			 * assigned.
			 *
			 * This allows the reference to contain a null value if
			 * needed, and it also frees the memory associated with the read
			 * queue once the reference has been assigned.
			 */
			this.readQueue = new LinkedList<Handle>();
		}

		/*
		 * Create the reference with an initial value already assigned. In this
		 * case, we don't need a reader queue.
		 */
		public RefInstance(final Object initial) {
			this.contents = initial;
			this.readQueue = null;
		}

		@Override
		protected void addMembers() {
			addMember("read", new readMethod());
			addMember("write", new writeMethod());
			addMember("readD", new SiteAdaptor() {
				@Override
				public void callSite(final Args args, final Handle caller) throws TokenException {
					synchronized (RefInstance.this) {
						if (readQueue != null) {
							caller.halt();
						} else {
							caller.publish(object2value(contents));
						}
					}
				}
                @Override
                public boolean nonBlocking() { return true; }
			});
		}

		protected class readMethod extends SiteAdaptor {
			@Override
			public void callSite(final Args args, final Handle reader) {
				synchronized (RefInstance.this) {
					/*
					 * If the read queue is not null, the ref has not been set.
					 * Add this caller to the read queue.
					 */
					if (readQueue != null) {
						reader.setQuiescent();
						readQueue.add(reader);
					}
					/* Otherwise, return the contents of the ref */
					else {
						reader.publish(object2value(contents));
					}
				}
			}
		}

		protected class writeMethod extends SiteAdaptor {
			@Override
			public void callSite(final Args args, final Handle writer) throws TokenException {
				synchronized (RefInstance.this) {

					final Object val = args.getArg(0);

					/* Set the contents of the ref */
					contents = val;

					/*
					 * If the read queue is not null, the ref has not yet been
					 * set.
					 */
					if (readQueue != null) {

						/*
						 * Wake up all queued readers and report the written
						 * value to them.
						 */
						for (final Handle reader : readQueue) {
							reader.publish(object2value(val));
						}

						/*
						 * Null out the read queue. This indicates that the ref
						 * has been written. It also allowed the associated
						 * memory to be reclaimed.
						 */
						readQueue = null;
					}

					/* A write always succeeds and publishes a signal. */
					writer.publish(object2value(signal()));
				}
			}
            @Override
            public boolean nonBlocking() { return true; }
		}

		@Override
		public String toString() {
			return "Ref(" + contents + ")";
		}
	}

    @Override
    public boolean nonBlocking() { return true; }
}
