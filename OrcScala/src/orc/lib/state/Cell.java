//
// Cell.java -- Java class Cell
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state;

import java.util.LinkedList;
import java.util.Queue;

import orc.CallContext;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.CellType;
import orc.run.distrib.AbstractLocation;
import orc.run.distrib.ClusterLocations;
import orc.run.distrib.DOrcPlacementPolicy;
import orc.types.Type;
import orc.values.sites.TypedSite;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.DotSite;
import orc.values.sites.compatibility.EvalSite;
import orc.values.sites.compatibility.SiteAdaptor;

/**
 * Write-once cell. Read operations block while the cell is empty. Write
 * operatons fail once the cell is full.
 *
 * @author dkitchin
 */
public class Cell extends EvalSite implements TypedSite {

    @Override
    public Object evaluate(final Args args) throws TokenException {
        if (args.size() == 0) {
            return new CellInstance();
        } else {
            throw new ArityMismatchException(0, args.size());
        }
    }

    @Override
    public Type orcType() {
        return CellType.getBuilder();
    }

    protected class CellInstance extends DotSite implements DOrcPlacementPolicy {

        protected Queue<CallContext> readQueue;
        Object contents;

        CellInstance() {
            this.contents = null;

            /*
             * Note that the readQueue also signals whether the cell contents
             * have been assigned. If it is non-null (as it is initially), the
             * cell is empty. If it is null, the cell has been written. This
             * allows the cell to contain a null value if needed, and it also
             * frees the memory associated with the read queue once the cell has
             * been assigned.
             */
            this.readQueue = new LinkedList<CallContext>();
        }

        @Override
        protected void addMembers() {
            addMember("read", new readMethod());
            addMember("write", new writeMethod());
            addMember("readD", new SiteAdaptor() {
                @Override
                public void callSite(final Args args, final CallContext caller) throws TokenException {
                    synchronized (CellInstance.this) {
                        if (readQueue != null) {
                            caller.halt();
                        } else {
                            caller.publish(object2value(contents));
                        }
                    }
                }

                @Override
                public boolean nonBlocking() {
                    return true;
                }
            });
        }

        protected class readMethod extends SiteAdaptor {
            @Override
            public void callSite(final Args args, final CallContext reader) {
                synchronized (CellInstance.this) {
                    /*
                     * If the read queue is not null, the cell has not been set.
                     * Add this caller to the read queue.
                     */
                    if (readQueue != null) {
                        reader.setQuiescent();
                        readQueue.add(reader);
                    }
                    /* Otherwise, return the contents of the cell */
                    else {
                        reader.publish(object2value(contents));
                    }
                }
            }

            @Override
            public boolean nonBlocking() {
                return true;
            }
        }

        protected class writeMethod extends SiteAdaptor {
            @Override
            public void callSite(final Args args, final CallContext writer) throws TokenException {
                synchronized (CellInstance.this) {

                    final Object val = args.getArg(0);

                    /*
                     * If the read queue is not null, the cell has not yet been
                     * set.
                     */
                    if (readQueue != null) {
                        /* Set the contents of the cell */
                        contents = val;

                        /*
                         * Wake up all queued readers and report the written
                         * value to them.
                         */
                        for (final CallContext reader : readQueue) {
                            reader.publish(object2value(val));
                        }

                        /*
                         * Null out the read queue. This indicates that the cell
                         * has been written. It also allowed the associated
                         * memory to be reclaimed.
                         */
                        readQueue = null;

                        /* A successful write publishes a signal. */
                        writer.publish(signal());
                    } else {
                        /* A failed write kills the writer. */
                        writer.halt();
                    }
                }
            }

            @Override
            public boolean nonBlocking() {
                return true;
            }
        }

        @Override
        public boolean nonBlocking() {
            return true;
        }

        @Override
        public <L extends AbstractLocation> scala.collection.immutable.Set<L> permittedLocations(final ClusterLocations<L> locations) {
            return locations.hereSet();
        }

    }

    @Override
    public boolean nonBlocking() {
        return true;
    }

    @Override
    public int minPublications() {
        return 0;
    }

    @Override
    public int maxPublications() {
        return 1;
    }

    @Override
    public boolean effectFree() {
        return true;
    }
}
