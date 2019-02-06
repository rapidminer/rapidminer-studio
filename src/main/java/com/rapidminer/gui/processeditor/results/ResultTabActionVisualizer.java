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
package com.rapidminer.gui.processeditor.results;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.rapidminer.example.ExampleSet;


/**
 * Helper class to to create swing components visualizing the {@link ResultActionGuiProvider}s.
 *
 * @author Andreas Timm
 * @since 9.1
 */
public final class ResultTabActionVisualizer {

	/**
	 * Maximum size for every component.
	 */
	private static final int MAX_COMPONENT_WIDTH = 250;
	private static final int MAX_COMPONENT_HEIGHT = 50;

	/**
	 * Util class, construction not supported
	 */
	private ResultTabActionVisualizer() {
		throw new UnsupportedOperationException("Do not instantiate thee");
	}

	/**
	 * Creates a swing component that either consists of the only available option or shows a popup list of possible actions.
	 *
	 * @param exampleSetSupplier
	 * 		the current exampleSetSupplier that should be used fur further actions
	 * @return a Button or a PopupMenu
	 */
	public static JComponent createResultActionsComponent(Supplier<ExampleSet> exampleSetSupplier) {
		final List<ResultActionGuiProvider> actions = ResultTabActionRegistry.INSTANCE.getActions();
		if (actions.isEmpty()) {
			return null;
		}

		final JPanel actionsPanel = new JPanel();
		actionsPanel.setLayout(new GridBagLayout());
		actionsPanel.setOpaque(false);

		actionsPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 0));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;

		for (ResultActionGuiProvider action : actions) {
			final JComponent component = action.createComponent(exampleSetSupplier);
			final Dimension preferredSize = component.getPreferredSize();
			if (preferredSize.getWidth() > MAX_COMPONENT_WIDTH || preferredSize.getHeight() > MAX_COMPONENT_HEIGHT) {
				preferredSize.setSize(Math.min(preferredSize.width, MAX_COMPONENT_WIDTH), Math.min(preferredSize.height, MAX_COMPONENT_HEIGHT));
			}
			actionsPanel.add(component, gbc);
			gbc.gridx++;
		}
		return actionsPanel;
	}
}
