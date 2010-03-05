//
// FileField.java -- Java class FileField
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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

public final class FileField extends JPanel {
	private final String title;
	private final boolean open;
	private final FileFilter filter;
	private final JTextField name = new JTextField();
	{
		name.setEditable(false);
	}

	public FileField(final String title, final boolean open, final FileFilter filter) {
		super(new BorderLayout(5, 0));
		this.title = title;
		this.open = open;
		this.filter = filter;
		add(name, BorderLayout.CENTER);
		name.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				chooseFile();
			}
		});
		final JButton choose = new JButton("...");
		choose.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				chooseFile();
			}
		});
		add(choose, BorderLayout.EAST);
	}

	public void addActionListener(final ActionListener listener) {
		name.addActionListener(listener);
	}

	private void chooseFile() {
		File wd;
		if (name.getText().equals("")) {
			wd = new File(System.getProperty("user.dir"));
		} else {
			wd = new File(name.getText()).getParentFile();
		}
		final JFileChooser chooser = new JFileChooser(wd);
		chooser.setDialogTitle(title);
		chooser.setFileFilter(filter);
		final int status = open ? chooser.showOpenDialog(this) : chooser.showSaveDialog(this);
		if (status == JFileChooser.APPROVE_OPTION) {
			name.setText(chooser.getSelectedFile().getPath());
		}
		name.postActionEvent();
	}

	public File getFile() {
		return new File(name.getText());
	}

	public void setFile(final File file) {
		name.setText(file.getPath());
	}
}
