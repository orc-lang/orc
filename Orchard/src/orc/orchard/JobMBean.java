//
// JobMBean.java -- Java interface JobMBean
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.util.Date;

import orc.orchard.jmx.JMXDescription;

public interface JobMBean {
	@JMXDescription("When was this job started?")
	public Date getStartDate();

	@JMXDescription("What state is this job in?")
	public String getState();

	@JMXDescription("Cancel the job (but do not remove it)")
	public void cancel();

	@JMXDescription("Cancel the job (if needed) and remove it")
	public void finish();

	@JMXDescription("Return the total number of events generated by the job")
	public int getTotalNumEvents();
}
