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
package com.rapidminer.gui.attributeeditor;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.attributeeditor.actions.ClearAction;
import com.rapidminer.gui.attributeeditor.actions.CloseAction;
import com.rapidminer.gui.attributeeditor.actions.LoadDataAction;
import com.rapidminer.gui.attributeeditor.actions.LoadSeriesDataAction;
import com.rapidminer.gui.attributeeditor.actions.OpenAttributeFileAction;
import com.rapidminer.gui.attributeeditor.actions.SaveAttributeFileAction;
import com.rapidminer.gui.attributeeditor.actions.SaveDataAction;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Tools;


/**
 * The dialog for the attribute editor. This dialog is used to display the data, load data, and
 * create attribute description files. Some actions are provided for these purposes.
 *
 * @see com.rapidminer.gui.attributeeditor.AttributeEditor
 * @author Ingo Mierswa, Simon Fischer
 */
public class AttributeEditorDialog extends ButtonDialog implements WindowListener {

	private static final long serialVersionUID = 6448298163392765295L;

	private final AttributeEditor attributeEditor;

	public transient Action OPEN_ATTRIBUTE_FILE_ACTION;

	public transient Action SAVE_ATTRIBUTE_FILE_ACTION;

	public transient Action LOAD_DATA_ACTION;

	public transient Action LOAD_SERIES_DATA_ACTION;

	public transient Action SAVE_DATA_ACTION;

	public transient Action CLEAR_ACTION;

	public transient Action CLOSE_ACTION;

	public AttributeEditorDialog(Operator exampleSource, File file) {
		this(exampleSource);
		if (file != null) {
			attributeEditor.openAttributeFile(file);
		}
	}

	public AttributeEditorDialog(Operator exampleSource) {
		super(ApplicationFrame.getApplicationFrame(), "attribute_editor", ModalityType.APPLICATION_MODAL, new Object[] {});

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(this);

		DataControl control = new DataControl(0, 0, "Example", "Attribute", false);
		attributeEditor = new AttributeEditor(exampleSource, control);
		control.addViewChangeListener(attributeEditor);
		getContentPane().add(control, BorderLayout.WEST);
		getContentPane().add(new ExtendedJScrollPane(attributeEditor), BorderLayout.CENTER);
		control.update();

		// initialize actions
		initActions();

		// menu bar
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new ResourceMenu("attribute_editor_file");
		fileMenu.add(OPEN_ATTRIBUTE_FILE_ACTION);
		fileMenu.add(SAVE_ATTRIBUTE_FILE_ACTION);
		fileMenu.add(LOAD_DATA_ACTION);
		fileMenu.add(SAVE_DATA_ACTION);
		fileMenu.add(LOAD_SERIES_DATA_ACTION);
		fileMenu.addSeparator();
		fileMenu.add(CLOSE_ACTION);
		menuBar.add(fileMenu);

		JMenu tableMenu = new ResourceMenu("attribute_editor_table");
		tableMenu.add(attributeEditor.GUESS_TYPE_ACTION);
		tableMenu.add(attributeEditor.GUESS_ALL_TYPES_ACTION);
		tableMenu.add(attributeEditor.REMOVE_COLUMN_ACTION);
		tableMenu.add(attributeEditor.REMOVE_ROW_ACTION);
		tableMenu.add(attributeEditor.USE_ROW_AS_NAMES_ACTION);
		tableMenu.add(CLEAR_ACTION);
		menuBar.add(tableMenu);

		setJMenuBar(menuBar);

		// tool bar
		JToolBar toolBar = new ExtendedJToolBar();
		toolBar.add(OPEN_ATTRIBUTE_FILE_ACTION);
		toolBar.add(SAVE_ATTRIBUTE_FILE_ACTION);
		toolBar.add(LOAD_DATA_ACTION);
		toolBar.add(SAVE_DATA_ACTION);
		toolBar.addSeparator();
		toolBar.add(CLEAR_ACTION);
		getContentPane().add(toolBar, BorderLayout.NORTH);

		setSize((int) Math.max(600, super.getOwner().getWidth() * 2.0d / 3.0d),
				(int) Math.max(400, super.getOwner().getHeight() * 2.0d / 3.0d));
		setLocationRelativeTo(super.getOwner());
	}

	public void initActions() {
		this.OPEN_ATTRIBUTE_FILE_ACTION = new OpenAttributeFileAction(attributeEditor);
		this.SAVE_ATTRIBUTE_FILE_ACTION = new SaveAttributeFileAction(attributeEditor);
		this.LOAD_DATA_ACTION = new LoadDataAction(attributeEditor);
		this.LOAD_SERIES_DATA_ACTION = new LoadSeriesDataAction(attributeEditor);
		this.SAVE_DATA_ACTION = new SaveDataAction(attributeEditor);
		this.CLEAR_ACTION = new ClearAction(attributeEditor);
		this.CLOSE_ACTION = new CloseAction(this);
	}

	public File getFile() {
		return attributeEditor.getFile();
	}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		close();
	}

	@Override
	public void close() {
		if (attributeEditor.hasDataChanged()) {
			int selectedOption = JOptionPane.showConfirmDialog(this,
					"It seems that you have changed the data without saving it afterwards." + Tools.getLineSeparator()
							+ "Do you still want to proceed and close the editor (changes will be lost)?",
					"Save data file?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (selectedOption == JOptionPane.YES_OPTION) {
				dispose();
			}
		} else if (attributeEditor.hasMetaDataChanged()) {
			int selectedOption = JOptionPane.showConfirmDialog(this,
					"It seems that you have changed the attribute descriptions without saving an attribute description file (.aml) afterwards."
							+ Tools.getLineSeparator()
							+ "Do you still want to proceed and close the editor (changes will be lost)?",
					"Save attribute description file?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (selectedOption == JOptionPane.YES_OPTION) {
				dispose();
			}
		} else {
			dispose();
		}
	}
}
