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
package com.rapidminer.gui.new_plotter.gui.cellrenderer;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;


/**
 * This class renders ComboBox items that contain enumeration values as a JLabel with label, tooltip
 * and icon set by a resource identifier.
 * <p>
 * The label settings are taken from a .properties file being part of the GUI Resource bundles of
 * RapidMiner. These might be accessed using the I18N class.
 * <p>
 * A resource action needs a key specifier, which will be used to build the complete keys of the
 * form:
 * <ul>
 * <li>gui.label.-key-.ENUM_VALUE.label as label</li>
 * <li>gui.label.-key-.ENUM_VALUE.tip as tooltip</li>
 * <li>gui.label.-key-.ENUM_VALUE.icon as icon</li>
 * </ul>
 *
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class EnumComboBoxCellRenderer<E> implements ListCellRenderer<E> {

	private final String i18nKeyPrefix;
	private final Map<Object, String> textCache = new HashMap<Object, String>();
	private final Map<Object, ImageIcon> iconCache = new HashMap<Object, ImageIcon>();
	private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

	/**
	 * Creates a Enumeration ComboBox cell renderer.
	 * 
	 * @param key
	 *            the gui resource key. Example: gui.label.foo_plotter.series_type.LINES.label with
	 *            key 'foo_plotter.series_type'
	 */
	public EnumComboBoxCellRenderer(String key) {
		this.i18nKeyPrefix = key;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel listCellRendererComponent = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
				isSelected, cellHasFocus);

		String text = textCache.get(value);
		ImageIcon icon = iconCache.get(value);
		if (text == null) {

			// get enum text
			text = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKeyPrefix + "." + value + ".label");
			if (text != null) {
				textCache.put(value, text);
			} else {
				text = i18nKeyPrefix + "." + value;
			}

			// create label icon
			String iconId = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.label." + i18nKeyPrefix + "." + value + ".icon");
			if (iconId != null) {
				icon = SwingTools.createIcon("16/" + iconId);
				iconCache.put(value, icon);
			}
		}

		// set text and icon
		listCellRendererComponent.setText(text);
		listCellRendererComponent.setIcon(icon);

		return listCellRendererComponent;
	}

}
