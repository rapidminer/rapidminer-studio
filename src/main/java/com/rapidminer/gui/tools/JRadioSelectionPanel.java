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
package com.rapidminer.gui.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 * This Class provides an panel for multiple content, allowing the user to switch between content
 * using a radio selection button group.
 * 
 * @author Sebastian Land
 */
public class JRadioSelectionPanel extends JPanel {

	private static final long serialVersionUID = 1683447823326877486L;

	private JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	private ButtonGroup toggleButtons = new ButtonGroup();

	private HashMap<Object, Component> buttonComponentMap = new HashMap<Object, Component>();

	public JRadioSelectionPanel() {
		super();
		setLayout(new BorderLayout());
		add(togglePanel, BorderLayout.NORTH);
	}

	/**
	 * This method allows to add a component to this container. Hence the container provides radio
	 * buttons to toggle between components, it is needed to specify a name for the components and
	 * toolTips.
	 * 
	 * @param selectionName
	 *            is the name of the component
	 * @param component
	 *            is the component to add
	 * @param toolTip
	 *            is the tool tip of corresponding radio button
	 */
	public void addComponent(String selectionName, Component component, String toolTip) {
		boolean isFirstButton = buttonComponentMap.size() == 0;
		final JRadioButton selectionButton = new JRadioButton(selectionName, isFirstButton);
		buttonComponentMap.put(selectionButton, component);

		selectionButton.setToolTipText(toolTip);
		selectionButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectionButton.isSelected()) {
					remove(1);
					add(buttonComponentMap.get(selectionButton), BorderLayout.CENTER);
					repaint();
				}
			}
		});
		toggleButtons.add(selectionButton);
		togglePanel.add(selectionButton);

		if (isFirstButton) {
			selectionButton.setSelected(true);
			add(component, BorderLayout.CENTER);
		}
	}
}
