package orc.orchard.test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.AbstractCompilerService;
import orc.orchard.Publication;
import orc.orchard.api.ExecutorServiceInterface;
import orc.orchard.api.JobServiceInterface;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.InvalidProgramException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.java.CompilerService;
import orc.orchard.oil.Oil;

public class RmiTest {
	public static void main(String[] args) throws InvalidOilException {
		CompilerService compiler = new CompilerService();
		Oil oil;
		try {
			oil = compiler.compile("site Rtimer = orc.lib.time.Rtimer site \"op+\" = orc.lib.math.Add def M(x) = x | Rtimer(1000) >> M(x+1) M(1)");
		} catch (InvalidProgramException e) {
			// this is impossible by construction
			throw new AssertionError(e);			
		}
		URI executorURI;
		try {
			executorURI = new URI("rmi://localhost/orchard/executor");
		} catch (URISyntaxException e) {
			// this is impossible by construction
			throw new AssertionError(e);
		}
		if (args.length > 0) {
			try {
				executorURI = new URI(args[0]);
			} catch (URISyntaxException e) {
				System.err.println("Invalid URI '" + executorURI + "'");
				return;
			}
		}
		ExecutorServiceInterface executor;
		try {
			executor = (ExecutorServiceInterface)Naming.lookup(executorURI.toString());
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + executorURI + "'");
			return;
		} catch (NotBoundException e) {
			System.err.println("URI not bound '" + executorURI);
			return;
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		}
		URI jobURI;
		try {
			jobURI = executor.submit(oil);
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (QuotaException e) {
			System.err.println("Quota error: " + e.toString());
			return;
		} catch (InvalidOilException e) {
			System.err.println("OIL error: " + e.toString());
			return;
		}
		System.out.println("Job URI: " + jobURI);
		JobServiceInterface job;
		try {
			job = (JobServiceInterface)Naming.lookup(jobURI.toString());
		} catch (MalformedURLException e) {
			System.err.println("Invalid URI '" + jobURI + "'");
			return;
		} catch (NotBoundException e) {
			System.err.println("URI not bound '" + jobURI);
			return;
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		}
		System.out.println("Bound job");
		try {
			System.out.println(job.state());
			job.start();
			System.out.println(job.state());
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// ignore
			}
			System.out.println("Done waiting");
			for (int i = 0; i < 5; ++i) {
				try {
					System.out.println(job.listen().toString());
				} catch (InterruptedException e) {
					System.out.println("Timed out");
					--i;
				}
			}
			List<Publication> pubs = job.publications();
			System.out.println(pubs.toString());
			job.halt();
			job.finish();
		} catch (RemoteException e) {
			System.err.println("Communication error: " + e.toString());
			return;
		} catch (InvalidJobStateException e) {
			// impossible by construction
			throw new AssertionError(e);
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
}
