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
package com.rapidminer.gui.graphs.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import com.rapidminer.gui.graphs.GraphViewer;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;

import edu.uci.ics.jung.visualization.control.ModalGraphMouse;


/**
 * Start the corresponding action.
 *
 * @author Ingo Mierswa
 */
public class PickingModeAction<V, E> extends AbstractAction {

	private static final long serialVersionUID = 6410209216842324428L;

	private static final String ICON_NAME = "hand_point_up.png";

	private static final Icon[] ICONS = new Icon[IconSize.values().length];

	static {
		int counter = 0;
		for (IconSize size : IconSize.values()) {
			ICONS[counter++] = SwingTools.createIcon(size.getSize() + "/" + ICON_NAME);
		}
	}

	private GraphViewer<V, E> graphViewer;

	public PickingModeAction(GraphViewer<V, E> graphViewer, IconSize size) {
		super("Pick", ICONS[size.ordinal()]);
		putValue(SHORT_DESCRIPTION, "Changes into picking mode.");
		putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_P));
		this.graphViewer = graphViewer;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		graphViewer.changeMode(ModalGraphMouse.Mode.PICKING);
	}
}
