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
package com.rapidminer.tools.config.gui.renderer;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import com.rapidminer.tools.config.AbstractConfigurator;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationManager;


/**
 * The renderer for the {@link JList} displaying {@link Configurable}s.
 *
 * @author Marco Boeck
 *
 */
public class ConfigurableRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 1L;

	/** color for type name */
	private static final String COLOR_GRAY = "#808080";

	/** color for type name on selected entries */
	private static final String COLOR_GRAY_HIGHLIGHT = "#888888";

	@Override
	public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		Configurable configurable = (Configurable) value;
		AbstractConfigurator<? extends Configurable> configurator = ConfigurationManager.getInstance()
				.getAbstractConfigurator(configurable.getTypeId());
		String text = "<html>" + configurable.getName() + "<br/><font color=\""
				+ (isSelected ? COLOR_GRAY_HIGHLIGHT : COLOR_GRAY) + "\">"
				+ (configurator == null ? configurable.getTypeId() : configurator.getName()) + "</font></html>";

		renderer.setText(text);
		renderer.setIcon(ConfigurationRenderer.getIconForType(configurator));
		renderer.setBorder(BorderFactory.createCompoundBorder(renderer.getBorder(),
				BorderFactory.createEmptyBorder(0, 5, 0, 0)));

		return renderer;
	}

}
