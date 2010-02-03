//
// ConfigPanel.java -- Java class ConfigPanel
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.gui;

import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;

import orc.Config;

import org.kohsuke.args4j.CmdLineException;

/**
 * Panel for editing configuration settings.
 * @author quark
 */
public final class ConfigPanel extends TwoColumnPanel {
	private final FileField traceOutFile = new FileField("Save Trace", false, new FileFilter() {
		@Override
		public boolean accept(final File f) {
			return f.isDirectory() || f.getName().endsWith(".trace");
		}

		@Override
		public String getDescription() {
			return "Orc Trace";
		}
	});
	private final FileField oilOutFile = new FileField("Save OIL", false, new FileFilter() {
		@Override
		public boolean accept(final File f) {
			return f.isDirectory() || f.getName().endsWith(".oil");
		}

		@Override
		public String getDescription() {
			return "OIL Scripts";
		}
	});
	private final JTextField includePath = new JTextField();
	private final JTextField classPath = new JTextField();
	private final JCheckBox typeChecking = new JCheckBox("Type checking enabled");
	private final JCheckBox noPrelude = new JCheckBox("Prelude disabled");
	private final SpinnerNumberModel numSiteThreads = new SpinnerNumberModel(1, 1, 100, 1);

	public ConfigPanel() {
		// Here's the layout:
		//
		//  |-----------------------|
		//  | field1                |
		//  |-----------------------|
		//  |    label2 | field2    |
		//  |-----------------------|
		//  |          ...          |
		//  |-----------------------|
		//  |            save cancel|
		//  |-----------------------|

		add(new JLabel("Save OIL to..."));
		add(oilOutFile);
		add(new JLabel("Save trace to..."));
		add(traceOutFile);
		add(new JLabel("Include path - separate entries with " + System.getProperty("path.separator")));
		add(includePath);
		add(new JLabel("Class path - separate entries with " + System.getProperty("path.separator")));
		add(classPath);
		add(typeChecking);
		add(noPrelude);
		addRow(new JLabel("Site threads:"), new JSpinner(numSiteThreads));
	}

	/** Call to load the fields from the model. */
	public void load(final Config config) {
		includePath.setText(config.getIncludePath());
		classPath.setText(config.getClassPath());
		typeChecking.setSelected(config.getTypeChecking());
		noPrelude.setSelected(config.getNoPrelude());
		numSiteThreads.setValue(config.getNumSiteThreads());
		oilOutFile.setFile(config.getOilOutputFile());
	}

	/** Call to save the fields to the model. 
	 * @throws CmdLineException */
	public void save(final Config config) throws CmdLineException {
		config.setTypeChecking(typeChecking.isSelected());
		config.setNoPrelude(noPrelude.isSelected());
		config.setIncludePath(includePath.getText());
		config.setClassPath(classPath.getText());
		config.setNumSiteThreads(numSiteThreads.getNumber().intValue());
		config.setOilOutputFile(oilOutFile.getFile());
		config.setTraceOutputFile(traceOutFile.getFile());
		//config.setFullTraceFile(null);
	}
}
