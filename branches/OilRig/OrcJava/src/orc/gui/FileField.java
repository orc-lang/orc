/**
 * Copyright (c) 2009, The University of Texas at Austin ("U. T. Austin")
 * All rights reserved.
 *
 * <p>You may distribute this file under the terms of the OSI Simplified BSD License,
 * as defined in the LICENSE file found in the project's top-level directory.
 */
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
	private String title;
	private boolean open;
	private FileFilter filter;
	private JTextField name = new JTextField();
	{
		name.setEditable(false);
	}
	public FileField(String title, boolean open, FileFilter filter) {
		super(new BorderLayout(5, 0));
		this.title = title;
		this.open = open;
		this.filter = filter;
		add(name, BorderLayout.CENTER);
		name.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				chooseFile();
			}
		});
		JButton choose = new JButton("...");
		choose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseFile();
			}
		});
		add(choose, BorderLayout.EAST);
	}
	
	public void addActionListener(ActionListener listener) {
		name.addActionListener(listener);
	}
	
	private void chooseFile() {
		File wd;
		if (name.getText().equals("")) {
			wd = new File(System.getProperty("user.dir"));
		} else {
			wd = new File(name.getText()).getParentFile();
		}
		JFileChooser chooser = new JFileChooser(wd);
		chooser.setDialogTitle(title);
		chooser.setFileFilter(filter);
		int status = open ? chooser.showOpenDialog(this) : chooser.showSaveDialog(this);
		if (status == JFileChooser.APPROVE_OPTION) {
			name.setText(chooser.getSelectedFile().getPath());
		}
		name.postActionEvent();
	}
	
	public File getFile() {
		return new File(name.getText());
	}
	
	public void setFile(File file) {
		name.setText(file.getPath());
	}
}