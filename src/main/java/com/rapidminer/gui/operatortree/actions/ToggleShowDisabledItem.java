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
package com.rapidminer.gui.operatortree.actions;

import com.rapidminer.gui.operatortree.OperatorTree;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class ToggleShowDisabledItem extends JCheckBoxMenuItem implements ActionListener {

	private static final long serialVersionUID = 570766967933245379L;

	private OperatorTree operatorTree;

	public ToggleShowDisabledItem(OperatorTree operatorTree, boolean state) {
		super("Show Disabled Operators", state);
		setToolTipText("Toggles if disabled operators should be displayed or not");
		addActionListener(this);
		this.operatorTree = operatorTree;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		this.operatorTree.toggleShowDisabledOperators();
	}
}
