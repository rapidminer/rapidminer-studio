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
package com.rapidminer.gui.attributeeditor.actions;

import com.rapidminer.gui.attributeeditor.AttributeEditor;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JOptionPane;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class LoadDataAction extends ResourceAction {

	private static final long serialVersionUID = -2111723479390457757L;

	private final AttributeEditor attributeEditor;

	public LoadDataAction(AttributeEditor attributeEditor) {
		super("attribute_editor.load_data");
		this.attributeEditor = attributeEditor;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		File file = SwingTools.chooseFile(this.attributeEditor, null, true, null, null);
		if (file != null) {
			try {
				this.attributeEditor.readData(file, AttributeEditor.LOAD_DATA);
			} catch (java.io.IOException ex) {
				JOptionPane.showMessageDialog(this.attributeEditor, e.toString(), "Error loading " + file,
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
