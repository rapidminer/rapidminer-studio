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
package com.rapidminer.gui.look.ui;

import com.rapidminer.gui.look.InternalFrameTitlePane;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicDesktopIconUI;


/**
 * The UI for desktop icons.
 * 
 * @author Ingo Mierswa
 */
public class DesktopIconUI extends BasicDesktopIconUI {

	private InternalFrameTitlePane iconPane;

	public static ComponentUI createUI(JComponent c) {
		return new DesktopIconUI();
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		c.setOpaque(false);
	}

	@Override
	protected void installComponents() {
		this.iconPane = new InternalFrameTitlePane(this.frame);
		this.desktopIcon.setLayout(new BorderLayout());
		this.desktopIcon.add(this.iconPane, BorderLayout.CENTER);
		this.desktopIcon.setBorder(null);
	}

	@Override
	protected void uninstallComponents() {
		this.desktopIcon.remove(this.iconPane);
		this.desktopIcon.setLayout(null);
		this.iconPane = null;
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		return getMinimumSize(c);
	}

	@Override
	public Dimension getMinimumSize(JComponent c) {
		Dimension dim = new Dimension(140, 26);
		return dim;
	}
}
