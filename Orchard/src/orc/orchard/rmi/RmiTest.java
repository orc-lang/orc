//
// RmiTest.java -- Java class RmiTest
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

package orc.orchard.rmi;

import java.net.URI;
import java.rmi.Naming;
import java.util.List;

import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.events.JobEvent;
import orc.orchard.events.PromptEvent;
import orc.orchard.java.CompilerService;

public class RmiTest {
	@SuppressWarnings("unused")
	public static void main(final String[] args) throws Exception {
		// Create an executor
		final URI executorURI = new URI("rmi://localhost/orchard/executor");
		new ExecutorService(executorURI);
		ExecutorServiceInterface executor;
		executor = (ExecutorServiceInterface) Naming.lookup(executorURI.toString());

		final CompilerService compiler = new CompilerService();
		// Check security validation
		String oil = compiler.compile("", "class String = java.lang.String 1");
		try {
			System.out.println(oil);
			executor.submit("", oil);
			System.err.println("Failed to catch validation error.");
			return;
		} catch (final InvalidOilException e) {
			System.out.println(e.toString());
		}
		;

		// Compile a program
		oil = compiler.compile("", "def M(x) = x | Rwait(1000) >> M(x+1) M(1)");

		// Submit the program
		String job = executor.submit("", oil);
		System.out.println("Job ID: " + job);
		System.out.println(executor.jobState("", job));
		executor.startJob("", job);
		System.out.println(executor.jobState("", job));
		Thread.sleep(3000);
		System.out.println("Done waiting");
		for (int i = 0; i < 5; ++i) {
			List<JobEvent> events;
			try {
				events = executor.jobEvents("", job);
				System.out.println(events.toString());
				executor.purgeJobEvents("", job);
			} catch (final InterruptedException e) {
				System.out.println("Timed out");
				--i;
			}
		}
		executor.cancelJob("", job);
		executor.finishJob("", job);

		// Test the Prompt site
		job = executor.compileAndSubmit("", "Prompt(\"Hello?\")");
		executor.startJob("", job);
		List<JobEvent> events = executor.jobEvents("", job);
		System.out.println(events.toString());
		executor.purgeJobEvents("", job);
		final int promptID = ((PromptEvent) events.get(0)).promptID;
		executor.respondToPrompt("", job, promptID, "Hi");
		events = executor.jobEvents("", job);
		System.out.println(events.toString());
		executor.purgeJobEvents("", job);
		System.out.println(executor.jobState("", job));
		executor.finishJob("", job);
	}
}
