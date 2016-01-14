//
// JobMBean.java -- Java interface JobMBean
// Project Orchard
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.Date;

import orc.orchard.jmx.JMXDescription;

@JMXDescription("Orchard Job")
public interface JobMBean {
    @JMXDescription("Date job was started")
    public Date getStartDate();

    @JMXDescription("Duration of job execution")
    public String getRunningTime();

    @JMXDescription("Current job state (NEW/RUNNING/BLOCKED/DONE)")
    public String getState();

    @JMXDescription("Username that started job, or (guest)")
    public String getOwner();

    @JMXDescription("Cancel the job (but do not remove it)")
    public void cancel();

    @JMXDescription("Cancel the job (if needed) and remove it")
    public void finish();

    @JMXDescription("Cumulative number of UI events during job run")
    public int getTotalNumEvents();

    @JMXDescription("Number of buffers, but not yet read, UI events")
    public int getNumBufferedEvents();

    @JMXDescription("Number of pending prompts")
    public int getNumPendingPrompts();

    @JMXDescription("Number of non-halted/killed tokens, IF counting is enabled")
    public int getTokenCount();
}
