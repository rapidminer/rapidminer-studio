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
package com.rapidminer.gui.docking;

import com.vlsolutions.swing.docking.FloatingDialog;

import java.awt.Dialog;
import java.awt.Frame;


/**
 * Detached dialog.
 * 
 * @author Tobias Malbrecht
 */
public class RapidFloatingDialog extends FloatingDialog {

	private static final long serialVersionUID = -1623376048447949302L;

	public RapidFloatingDialog(Dialog parent) {
		super(parent);
	}

	public RapidFloatingDialog(Frame parent) {
		super(parent);
	}

	// @Override
	// public void init() {}
	//
	// @Override
	// public void installDecoration() {}
	//
	// @Override
	// public void installBorders() {}
	//
	// @Override
	// public void installResizers() {}
}
