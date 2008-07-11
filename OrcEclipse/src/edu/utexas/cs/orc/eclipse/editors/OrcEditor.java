package edu.utexas.cs.orc.eclipse.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class OrcEditor extends TextEditor {

	//private ColorManager colorManager;

	public OrcEditor() {
		super();
		//colorManager = new ColorManager();
		//setSourceViewerConfiguration(new XMLConfiguration(colorManager));
		//setDocumentProvider(new XMLDocumentProvider());
	}
	public void dispose() {
		//colorManager.dispose();
		super.dispose();
	}
}
