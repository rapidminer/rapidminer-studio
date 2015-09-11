/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.actions;

import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.ResourceAction;

import java.awt.event.ActionEvent;


/**
 * Start the corresponding action.
 * 
 * Provided as a patch in bugreport #310.
 * 
 * @author Tobias Schlitt <tobias@schlitt.info>
 */
public class MoveColumnAction extends ResourceAction {

	private static final long serialVersionUID = -8676231093844470601L;

	private ExtendedJTable table;

	private int moveTo;

	public MoveColumnAction(ExtendedJTable table, IconSize size, int moveTo) {
		super("move_column", moveTo + 1);
		this.table = table;
		this.moveTo = moveTo;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.table.moveColumn(table.getSelectedColumn(), moveTo);
	}
}
