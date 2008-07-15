package orc.orchard.rmi;

import java.net.URI;
import java.rmi.Naming;
import java.util.List;

import orc.orchard.JobEvent;
import orc.orchard.PromptEvent;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.java.CompilerService;
import orc.orchard.oil.Oil;

public class RmiTest {
	public static void main(String[] args) throws Exception {
		CompilerService compiler = new CompilerService();
		// Check security validation
		Oil oil = compiler.compile("", "class String = java.lang.String 1");
		try {
			System.out.println(oil);
			new orc.orchard.java.ExecutorService().submit("", oil);
			System.err.println("Failed to catch validation error.");
			return;
		} catch (InvalidOilException e) {
			System.out.println(e.toString());
		};

		// Compile a program
		oil = compiler.compile("", "def M(x) = x | Rtimer(1000) >> M(x+1) M(1)");
		
		// Create an executor
		URI executorURI = new URI("rmi://localhost/orchard/executor");
		new ExecutorService(executorURI);
		ExecutorServiceInterface executor;
		executor = (ExecutorServiceInterface)Naming.lookup(executorURI.toString());

		// Submit the program
		String job = executor.submit("", oil);
		System.out.println("Job ID: " + job);
		System.out.println(executor.jobState("", job));
		executor.startJob("", job);
		System.out.println(executor.jobState("", job));
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// ignore
		}
		System.out.println("Done waiting");
		for (int i = 0; i < 5; ++i) {
			List<JobEvent> events;
			try {
				events = executor.jobEvents("", job);
				System.out.println(events.toString());
				executor.purgeJobEvents("", job);
			} catch (InterruptedException e) {
				System.out.println("Timed out");
				--i;
			}
		}
		executor.haltJob("", job);
		executor.finishJob("", job);
		
		// Test the Prompt site
		job = executor.compileAndSubmit("", "Prompt(\"Hello?\")");
		executor.startJob("", job);
		List<JobEvent> events = executor.jobEvents("", job);
		System.out.println(events.toString());
		executor.purgeJobEvents("", job);
		int promptID = ((PromptEvent)events.get(0)).promptID;
		executor.respondToPrompt("", job, promptID, "Hi");
		events = executor.jobEvents("", job);
		System.out.println(events.toString());
		executor.purgeJobEvents("", job);
		System.out.println(executor.jobState("", job));
		executor.finishJob("", job);
	}
}
