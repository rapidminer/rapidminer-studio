/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.gui.processeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * A text area for editing the process as XML. This editor is the second possible way to edit or
 * create RapidMiner processes. All changes are reflected by the process. However, it should be more
 * convenient to use the tree view for process design. <br/>
 *
 * This XML editor support very simple syntax highlighting based on keyword parsing.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class XMLEditor extends JPanel implements ProcessEditor, Dockable {

	private static final long serialVersionUID = 4172143138689034659L;

	public static final String XML_EDITOR_DOCK_KEY = "xml_editor";

	private final DockKey DOCK_KEY = new ResourceDockKey(XML_EDITOR_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	private final RSyntaxTextArea editor;

	private final MainFrame mainFrame;

	public XMLEditor(MainFrame mainFrame) {
		super(new BorderLayout());
		this.mainFrame = mainFrame;

		// create text area
		this.editor = new RSyntaxTextArea(new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_XML)) {

			@Override
			public synchronized void setText(String t) {
				super.setText(t);
			}
		};
		this.editor.setAnimateBracketMatching(true);
		this.editor.setAutoIndentEnabled(true);
		this.editor.setSelectionColor(Colors.TEXT_HIGHLIGHT_BACKGROUND);
		this.editor.setBorder(null);

		JToolBar toolBar = new ExtendedJToolBar();
		toolBar.setBorder(null);
		toolBar.add(new ResourceAction(true, "xml_editor.apply_changes") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				try {
					validateProcess();
				} catch (IOException | XMLException e1) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.processeditor.XMLEditor.failed_to_parse_process");
				}
			}
		});
		toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.TEXTFIELD_BORDER));

		add(toolBar, BorderLayout.NORTH);
		RTextScrollPane rTextScrollPane = new RTextScrollPane(editor);
		rTextScrollPane.setBorder(null);
		add(rTextScrollPane, BorderLayout.CENTER);
	}

	public void setText(String text) {
		this.editor.setText(text);
	}

	@Override
	public void processUpdated(Process process) {
		setText(process.getRootOperator().getXML(false));
	}

	@Override
	public void processChanged(Process process) {
		processUpdated(process);
	}

	/** Just jumps to the position of the currently selected operator. */
	@Override
	public void setSelection(List<Operator> selection) {
		if (!selection.isEmpty()) {
			Operator currentOperator = selection.get(0);
			String name = currentOperator.getName();
			synchronized (this.editor) {
				String text = this.editor.getText();
				int start = text.indexOf("\"" + name + "\"");
				int end = start + name.length() + 1;
				if (start >= 0 && editor.getDocument().getLength() > end) {
					this.editor.select(start + 1, end);
					this.editor.setCaretPosition(end);
				}
			}
		}
	}

	/**
	 * Validates the process represented by the current xml. Will update the actual process if the xml representation
	 * differs. This diff will ignore white space changes, but will update the displayed xml with the parser
	 * conform version.
	 */
	public synchronized void validateProcess() throws IOException, XMLException {
		String editorContent = getXMLFromEditor();
		Process oldProcess = RapidMinerGUI.getMainFrame().getProcess();
		String oldXML = oldProcess.getRootOperator().getXML(false);
		if (oldXML.trim().equals(editorContent)) {
			return;
		}
		Process newProcess = new Process(editorContent);
		ProcessRendererView processRenderer = mainFrame.getProcessPanel().getProcessRenderer();
		ProcessDrawUtils.ensureOperatorsHaveLocation(newProcess, processRenderer.getModel());
		String newXML = newProcess.getRootOperator().getXML(false);
		if (!newXML.equals(oldXML)) {
			newProcess.setProcessLocation(oldProcess.getProcessLocation());
			mainFrame.setProcess(newProcess, false);
			processRenderer.getModel().setProcess(newProcess, false, false);
		} else {
			setText(oldXML);
		}
	}

	public String getXMLFromEditor() {
		return this.editor.getText().trim();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
}
