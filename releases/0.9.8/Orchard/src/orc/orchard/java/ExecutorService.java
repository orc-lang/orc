package orc.orchard.java;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import orc.orchard.AbstractExecutorService;
import orc.orchard.errors.*;
import orc.orchard.events.*;

public class ExecutorService extends AbstractExecutorService {
	
	public static void main(String[] args)
	throws FileNotFoundException, IOException, QuotaException,
		InvalidProgramException, InvalidOilException, InvalidJobStateException,
		InvalidJobException, InterruptedException
	{
		String program;
		if (args.length > 0) {
			program = getStreamContent(new FileInputStream(args[0]));
		} else {
			program = getStreamContent(System.in);
		}
		final ExecutorService executor = new ExecutorService();
		executor.logger.setLevel(Level.OFF);
		final String job = executor.compileAndSubmit("", program);
		executor.startJob("", job);
		List<JobEvent> events;
		do {
			events = executor.jobEvents("", job);
			executor.purgeJobEvents("", job);
			for (JobEvent event : events) {
				event.accept(new Visitor<Void>() {
					public Void visit(PrintlnEvent event) {
						System.out.println(event.line);
						return null;
					}
					public Void visit(PromptEvent event) {
						String response = JOptionPane.showInputDialog(event.message);
						try {
							executor.respondToPrompt("", job, event.promptID, response);
						} catch (RemoteException e) {
							System.err.println("ERROR: " + e.getMessage());
						} catch (InvalidPromptException e) {
							System.err.println("ERROR: " + e.getMessage());
						} catch (InvalidJobException e) {
							System.err.println("ERROR: " + e.getMessage());
						}
						return null;
					}
					public Void visit(PublicationEvent event) {
						System.out.println(event.value.toString());
						return null;
					}
					public Void visit(RedirectEvent event) {
						System.err.println("REDIRECT: " + event.url);
						return null;
					}
					public Void visit(TokenErrorEvent event) {
						System.err.println("ERROR: " + event.message);
						return null;
					}
				});
			}
			Thread.yield();
		} while (!events.isEmpty());
	}
	
	private static String getStreamContent(InputStream stream) throws IOException {
		// read program from stdin
		InputStreamReader reader = new InputStreamReader(stream);
		StringBuilder sb = new StringBuilder();
		int blen;
		char[] buffer = new char[1024];
		while ((blen = reader.read(buffer)) > 0) {
			sb.append(buffer, 0, blen);
		}
		return sb.toString();
	}
}