/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.actions;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class UndoAction extends ResourceAction {

	private static final long serialVersionUID = 4767902062440337756L;

	private MainFrame mainFrame;

	public UndoAction(MainFrame mainFrame) {
		super("undo");
		this.mainFrame = mainFrame;
		setCondition(EDIT_IN_PROGRESS, DISALLOWED);
		setCondition(PROCESS_RENDERER_IS_VISIBLE, MANDATORY);
		setCondition(PROCESS_RENDERER_HAS_UNDO_STEPS, MANDATORY);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.mainFrame.undo();
	}
}
