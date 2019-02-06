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
package com.rapidminer.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.tools.Observable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Observer;


/**
 * A {@link ResourceMenu} which displays the all {@link Perspective}s of the
 * {@link PerspectiveController}. Uses the {@link PerspectiveModel} to fetch updates, like adding of
 * new perspectives or perspective changes.
 *
 * @author Marcel Michel
 * @since 7.0.0
 */
public class PerspectiveMenu extends ResourceMenu {

	private static final long serialVersionUID = 1L;

	private final PerspectiveController perspectiveController;

	private final Map<String, JMenuItem> perspectiveMap = new HashMap<>();

	private ButtonGroup workspaceMenuGroup = new ButtonGroup();

	private String perspectiveName;

	private final Observer<List<Perspective>> perspectiveObserver = new Observer<List<Perspective>>() {

		@Override
		public void update(Observable<List<Perspective>> observable, List<Perspective> perspectives) {
			updatePerspectives(perspectives);
		}

	};

	private final PerspectiveChangeListener perspectiveChangeListener = new PerspectiveChangeListener() {

		@Override
		public void perspectiveChangedTo(Perspective perspective) {
			perspectiveName = perspective.getName();
			if (perspectiveMap.containsKey(perspectiveName)) {
				SwingTools.invokeLater(() -> {
					JMenuItem item = perspectiveMap.get(perspectiveName);
					if (item != null) {
						item.setSelected(true);
					}
				});
			}
		}
	};

	/**
	 * Creates a new {@link PerspectiveMenu} and uses the {@link PerspectiveController} to get the
	 * registrered {@link Perspective}s.
	 *
	 * @param perspectiveController
	 *            the controller which should be used the fetch the perspectives
	 */
	public PerspectiveMenu(PerspectiveController perspectiveController) {
		super("perspectives");
		this.perspectiveController = perspectiveController;
		this.perspectiveController.getModel().addObserver(perspectiveObserver, true);
		this.perspectiveController.getModel().addPerspectiveChangeListener(perspectiveChangeListener);
		updatePerspectives(this.perspectiveController.getModel().getAllPerspectives());
	}

	private void updatePerspectives(List<Perspective> perspectives) {
		removeAll();
		perspectiveMap.clear();
		workspaceMenuGroup = new ButtonGroup();
		for (Perspective p : perspectives) {
			ResourceAction action = perspectiveController.createPerspectiveAction(p);

			JMenuItem menuItem = new JRadioButtonMenuItem(action);
			add(menuItem);
			perspectiveMap.put(p.getName(), menuItem);
			workspaceMenuGroup.add(menuItem);
		}
		if (perspectiveMap.containsKey(perspectiveName)) {
			perspectiveMap.get(perspectiveName).setSelected(true);
		}
	}

}
