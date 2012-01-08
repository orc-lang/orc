//
// WorkflowEmailNotifier.java -- Java class WorkflowEmailNotifier
// Project OrcWikiPlugin
//
// $Id$
//
// Created by jthywiss on Jan 6, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.jspwiki;

import java.security.Principal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import javax.mail.MessagingException;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WorkflowEvent;
import com.ecyrd.jspwiki.plugin.InitializablePlugin;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import com.ecyrd.jspwiki.util.MailUtil;
import com.ecyrd.jspwiki.util.WikiBackgroundThread;
import com.ecyrd.jspwiki.workflow.Decision;
import com.ecyrd.jspwiki.workflow.Fact;
import com.ecyrd.jspwiki.workflow.Step;
import com.ecyrd.jspwiki.workflow.Workflow;

/**
 * This JSPWiki plugin sends e-mail notification messages to the assigned users
 * when a workflow is waiting on their action.
 * 
 * @author jthywiss
 */
public class WorkflowEmailNotifier implements WikiPlugin, InitializablePlugin {

	protected static WorkflowEmailNotifierWatcher ourWatcher = null;

	/** Property name for scanning interval, in seconds */
	public static final String PROP_WORKFLOW_NOTIFIER_INTERVAL = "workflowEmailNotifier.scanInterval";

	/** Default value for scanning interval, in seconds */
	public static final int DEFAULT_WORKFLOW_NOTIFIER_INTERVAL = 900;

	/**
	 * PluginManager calls this once on a throw-away instance. Begin watching
	 * this engine's workflow decision queue.
	 */
	@Override
	public synchronized void initialize(final WikiEngine engine) throws PluginException {
		assert ourWatcher == null : "Multiple initialize of WorkflowEmailNotifier plugin";
		ourWatcher = new WorkflowEmailNotifierWatcher(engine);
		ourWatcher.start();
	}

	/**
	 * This plugin should never be executed, but if so, it returns an empty
	 * string.
	 */
	@Override
	public String execute(final WikiContext context, @SuppressWarnings("rawtypes") final Map params) throws PluginException {
		return "";
	}

	/**
	 * Listens for WorkflowEvents in the engine and sends e-mail messages when a
	 * workflow waits on an actor.
	 * 
	 * @author jthywiss
	 */
	protected static class WorkflowEmailNotifierWatcher extends WikiBackgroundThread {

		protected static DateFormat dateTimeFormatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss zzz");

		protected Set<Integer> notifiedWfIds = new HashSet<Integer>();

		protected final WorkflowEventListener wfEventListener = new WorkflowEventListener();

		/**
		 * Watch this engine.
		 * 
		 * @param engine
		 */
		public WorkflowEmailNotifierWatcher(final WikiEngine engine) {
			super(engine, TextUtil.getIntegerProperty(engine.getWikiProperties(), PROP_WORKFLOW_NOTIFIER_INTERVAL, DEFAULT_WORKFLOW_NOTIFIER_INTERVAL) /* seconds */);
			/*Thread.*/setName("WorkflowEmailNotifier");
		}

		/* (non-Javadoc)
		 * @see com.ecyrd.jspwiki.util.WikiBackgroundThread#startupTask()
		 */
		@Override
		public void startupTask() throws Exception {
			super.startupTask();
		}

		/* (non-Javadoc)
		 * @see com.ecyrd.jspwiki.util.WikiBackgroundThread#backgroundTask()
		 */
		@SuppressWarnings("boxing")
		@Override
		public void backgroundTask() throws Exception {
			
			if (getEngine().getWorkflowManager() == null) {
				/* WorkflowManager is not running, so there's nothing to do. */
				return;
			}

			@SuppressWarnings("unchecked")
			final Collection<Workflow> wfis = getEngine().getWorkflowManager().getWorkflows();
			for (final Workflow wfi : wfis) {
				boolean notify = false;
				synchronized (notifiedWfIds) {
					if (wfi.getCurrentState() == Workflow.WAITING && !notifiedWfIds.contains(wfi.getId())) {
						wfi.addWikiEventListener(wfEventListener);
						notifiedWfIds.add(wfi.getId());
						notify = true;
					}
				}
				if (notify) {
					notifyWfAssignees(wfi);
				}
			}
		}

		/* (non-Javadoc)
		 * @see com.ecyrd.jspwiki.util.WikiBackgroundThread#shutdownTask()
		 */
		@Override
		public void shutdownTask() throws Exception {
			synchronized (notifiedWfIds) {
				notifiedWfIds.clear();
			}
			super.shutdownTask();
		}

		protected void notifyWfAssignees(final Workflow wfProcInst) {

			// Get list of "to" mailboxes
			final Principal assigneePrinc = wfProcInst.getCurrentActor();
			final Collection<String> assigneeMailboxes = new HashSet<String>();
			mailboxesForPrincipal(assigneePrinc, assigneeMailboxes);
			if (assigneeMailboxes.isEmpty()) {
				/* No e-mail addresses found for the assignee principal.
				 * Nothing to do.
				 */
				return;
			}
			final StringBuilder mailToString = new StringBuilder();
			for (final String s : assigneeMailboxes) {
				mailToString.append(s);
				mailToString.append(", ");
			}
			/* Trim trailing ", " */
			mailToString.setLength(mailToString.length() - 2);

			final Step workItem = wfProcInst.getCurrentStep();
			final Decision wiDecision = workItem instanceof Decision ? (Decision) workItem : null;
			final String workItemTitle = StripHtml.stripXhtml(formatLocalizedMsg(workItem.getMessageKey(), workItem.getMessageArguments()));

			// Compute subject line
			final StringBuilder subject = new StringBuilder();
			subject.append(getEngine().getApplicationName());
			subject.append(": ");
			subject.append(workItemTitle);

			// Build content
			final StringBuilder content = new StringBuilder();
			subject.append(getEngine().getApplicationName());
			content.append(" <");
			content.append(getEngine().getBaseURL());
			content.append("> has a new workflow item for your action.\n");
			if (wiDecision != null) {
				content.append("\nWork item ID: ");
				content.append(wiDecision.getId());
			}
			content.append("\nWork item description: ");
			content.append(workItemTitle);
			content.append("\nWork item requested by: ");
			content.append(workItem.getOwner());
			content.append("\nWork item received: ");
			content.append(dateTimeFormatter.format(workItem.getStartTime()));
			content.append("\nWork item assigned to: ");
			content.append(workItem.getActor().getName());
			content.append(" (");
			content.append(assigneeMailboxes.size());
			content.append(" users)");
			if (wiDecision != null && !wiDecision.getFacts().isEmpty()) {
				content.append("\nWork item details:");
				for (final Object o : wiDecision.getFacts()) {
					final Fact currFact = (Fact) o;
					content.append("\n  - ");
					content.append(getLocalizedMsg(currFact.getMessageKey()));
					content.append(": ");
					content.append(currFact.getValue());
				}
			}
			content.append("\n\nTo act on this item, go to the wiki at URL: ");
			content.append(getEngine().getBaseURL());
			content.append(" , log in, and select Workflow from the \"More\" menu.\n");

			try {
				MailUtil.sendMessage(getEngine(), mailToString.toString(), subject.toString(), content.toString());
			} catch (final MessagingException e) {
				getEngine().getServletContext().log("WorkflowEmailNotifier wiki plugin: Exception when sending mail message: " + e, e);
			}

		}

		protected String getLocalizedMsg(final String key) throws MissingResourceException {
			return getEngine().getInternationalizationManager().get("templates.default", null, key);
		}

		protected String formatLocalizedMsg(final String key, final Object[] args) throws MissingResourceException {
			return new MessageFormat(getLocalizedMsg(key)).format(args);
		}

		protected void mailboxesForPrincipal(final Principal princ, final Collection<String> mailboxes) {
			if (AuthenticationManager.isUserPrincipal(princ)) {
				try {
					mailboxes.add(getEngine().getUserManager().getUserDatabase().find(princ.getName()).getEmail());
				} catch (final NoSuchPrincipalException e) {
					final AssertionError ae = new AssertionError("User \"" + princ.getName() + "\" not found, but AuthenticationManager claimed it was a UserPrincipal");
					ae.initCause(e);
					throw ae;
				}
			} else {
				Group group;
				try {
					group = getEngine().getGroupManager().getGroup(princ.getName());
					for (final Principal member : group.members()) {
						mailboxesForPrincipal(member, mailboxes);
					}
				} catch (final NoSuchPrincipalException e) {
					// Disregard -- might be a principal type other than a group
				}
			}
		}

		protected class WorkflowEventListener implements WikiEventListener {
			/* (non-Javadoc)
			 * @see com.ecyrd.jspwiki.event.WikiEventListener#actionPerformed(com.ecyrd.jspwiki.event.WikiEvent)
			 */
			@SuppressWarnings("boxing")
			@Override
			public void actionPerformed(final WikiEvent event) {
				switch (event.getType()) {
				case WorkflowEvent.ABORTED:
				case WorkflowEvent.RUNNING:
				case WorkflowEvent.COMPLETED:
					final Workflow wfProcInst = ((WorkflowEvent) event).getWorkflow();
					synchronized (notifiedWfIds) {
						notifiedWfIds.remove(wfProcInst.getId());
					}
					break;
				default:
					break;
				}
			}
		}

	}

}
