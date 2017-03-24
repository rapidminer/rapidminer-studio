/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.gui.attributeeditor.AttributeEditorDialog;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class CloseAction extends ResourceAction {

	private static final long serialVersionUID = -3571057663841304137L;

	private final AttributeEditorDialog attributeEditorDialog;

	public CloseAction(AttributeEditorDialog attributeEditorDialog) {
		super("attribute_editor.close");
		this.attributeEditorDialog = attributeEditorDialog;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.attributeEditorDialog.close();
	}
}
