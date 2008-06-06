package orc.orchard.test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import orc.orchard.error.*;
import orc.orchard.interfaces.*;
import orc.orchard.rmi.CompilerService;

public class RmiTest {
	public static void main(String[] args) {
		URI executorURI;
		try {
			executorURI = new URI("rmi://localhost/orchard");
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
		CompilerService compiler = new orc.orchard.rmi.CompilerService();
		ExecutorService executor;
		try {
			executor = (ExecutorService)Naming.lookup(executorURI.toString());
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
		Oil oil;
		try {
			oil = compiler.compile("def M(x) = x | Rtimer(1000) >> M(x+1) M(1)");
		} catch (InvalidProgramException e) {
			// this is impossible by construction
			throw new AssertionError(e);			
		}
		URI jobURI;
		try {
			System.out.println(executor.jobs());
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
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
		System.out.println("Job URI: " + jobURI);
		JobService job;
		try {
			job = (JobService)Naming.lookup(jobURI.toString());
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
			System.out.println(executor.jobs());
			System.out.println(job.state());
			job.start();
			System.out.println(job.state());
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// ignore
			}
			System.out.println("Done waiting");
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			System.out.println(job.listen().toString());
			List<Publication> pubs = job.publications();
			System.out.println(pubs.toString());
			job.abort();
			System.out.println(job.state());
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
