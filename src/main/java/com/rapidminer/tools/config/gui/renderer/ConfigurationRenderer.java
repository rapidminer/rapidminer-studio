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
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.config.AbstractConfigurator;


/**
 * The renderer for the {@link JList} displaying {@link AbstractConfigurator}s.
 *
 * @author Marcel Michel
 *
 */
public class ConfigurationRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = -7215198911627278249L;

	/** mapping between configurator types and icons */
	private static final Map<String, Icon> mappingTypeToIcon = new HashMap<>();

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		AbstractConfigurator<?> configurator = (AbstractConfigurator<?>) value;
		String text = configurator == null ? "" : configurator.getName();

		label.setIcon(getIconForType(configurator));
		label.setText(text);

		return label;
	}

	/**
	 * Returns the (cached) {@link Icon} for the given {@link Configurator} type. If no icon was
	 * defined, returns <code>null</code>.
	 *
	 * @param configurator
	 * @return
	 */
	public static Icon getIconForType(AbstractConfigurator<?> configurator) {
		if (configurator == null) {
			return null;
		}
		Icon icon = mappingTypeToIcon.get(configurator.getTypeId());
		if (icon == null) {
			mappingTypeToIcon.put(configurator.getTypeId(), SwingTools.createIcon("24/" + configurator.getIconName()));
			icon = mappingTypeToIcon.get(configurator.getTypeId());
		}

		return icon;
	}
}
