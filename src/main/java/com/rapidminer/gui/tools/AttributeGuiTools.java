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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import com.rapidminer.example.Attributes;
import com.rapidminer.gui.viewer.metadata.MetaDataStatisticsViewer;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Ontology;


/**
 * This class contains helper methods for various GUI representations related to {@link Attributes}
 * which should look the same throughout RapidMiner. Examples are getting the appropriate icon for a
 * {@link Ontology#ATTRIBUTE_VALUE_TYPE} or getting the color which is used by the
 * {@link MetaDataStatisticsViewer} to color the {@link Attributes#KNOWN_ATTRIBUTE_TYPES}.
 *
 * @author Marco Boeck
 *
 */
public class AttributeGuiTools {

	/** contains a mapping between each existing value type and a symbol for it */
	private static final Map<Integer, String> mapOfValueTypeIcons = new HashMap<>();

	/** mapping between {@link SpecialAttribute} and colors */
	private static final Map<String, Color> mapAttributeRoleNamesToColors = new HashMap<>();

	/** mapping between value types and a color */
	private static final Map<Integer, Color> mapOfValueTypeColors = new HashMap<>();

	/**
	 * can be used to access the default special attribute role color from
	 * {@link #getColorForAttributeRole(String)}
	 */
	public static final String GENERIC_SPECIAL_ATTRIBUTE_NAME = "special";

	static {
		// fill mapping between value types and icons
		mapOfValueTypeIcons.put(Ontology.ATTRIBUTE_VALUE,
				I18N.getMessage(I18N.getGUIBundle(), "gui.icon.attribute_value_type.attribute_value.icon"));	// fallback
		// only
		mapOfValueTypeIcons.put(Ontology.NUMERICAL,
				I18N.getMessage(I18N.getGUIBundle(), "gui.icon.attribute_value_type.numerical.icon"));
		mapOfValueTypeIcons.put(Ontology.NOMINAL,
				I18N.getMessage(I18N.getGUIBundle(), "gui.icon.attribute_value_type.nominal.icon"));
		mapOfValueTypeIcons.put(Ontology.BINOMINAL,
				I18N.getMessage(I18N.getGUIBundle(), "gui.icon.attribute_value_type.binominal.icon"));
		mapOfValueTypeIcons.put(Ontology.STRING,
				I18N.getMessage(I18N.getGUIBundle(), "gui.icon.attribute_value_type.text.icon"));
		mapOfValueTypeIcons.put(Ontology.DATE_TIME,
				I18N.getMessage(I18N.getGUIBundle(), "gui.icon.attribute_Value_type.date_time.icon"));

		// fill color mapping for attribute roles
		mapAttributeRoleNamesToColors.put(Attributes.WEIGHT_NAME, new Color(255, 220, 238));
		mapAttributeRoleNamesToColors.put(Attributes.LABEL_NAME, new Color(226, 255, 218));
		mapAttributeRoleNamesToColors.put(Attributes.PREDICTION_NAME, new Color(226, 255, 218));
		mapAttributeRoleNamesToColors.put(Attributes.CONFIDENCE_NAME, new Color(243, 255, 235));
		mapAttributeRoleNamesToColors.put(Attributes.ID_NAME, new Color(225, 235, 255));
		mapAttributeRoleNamesToColors.put(GENERIC_SPECIAL_ATTRIBUTE_NAME, new Color(255, 255, 222));	// other
		// special
		// attributes,
		// e.g.
		// user
		// defined
		// ones
		mapAttributeRoleNamesToColors.put(Attributes.ATTRIBUTE_NAME, new Color(245, 245, 245));	// regular
		// attributes

		// fill color mapping for attribute value types
		mapOfValueTypeColors.put(Ontology.ATTRIBUTE_VALUE, new Color(255, 255, 153));	// fallback
		// only
		mapOfValueTypeColors.put(Ontology.NUMERICAL, new Color(127, 201, 127));
		mapOfValueTypeColors.put(Ontology.NOMINAL, new Color(127, 201, 127));
		mapOfValueTypeColors.put(Ontology.DATE_TIME, new Color(127, 201, 127));
	}

	/**
	 * Returns the {@link ImageIcon} used to represent the given
	 * {@link Ontology#ATTRIBUTE_VALUE_TYPE}.
	 *
	 * @param valueType
	 * @param smallIcon
	 * @return
	 */
	public static ImageIcon getIconForValueType(int valueType, boolean smallIcon) {
		String iconName = mapOfValueTypeIcons.get(valueType);
		while (iconName == null) {
			valueType = Ontology.ATTRIBUTE_VALUE_TYPE.getParent(valueType);
			iconName = mapOfValueTypeIcons.get(valueType);
		}

		ImageIcon icon;
		if (smallIcon) {
			icon = SwingTools.createIcon("16/" + iconName);
		} else {
			icon = SwingTools.createIcon("24/" + iconName);
		}

		return icon;
	}

	/**
	 * Returns the color for the specified special attribute name {@link String}. See
	 * {@link Attributes#KNOWN_ATTRIBUTE_TYPES}. If the name is unknown, returns the default color
	 * for special attribute roles.
	 *
	 * @param attributeRoleLabel
	 * @return
	 */
	public static Color getColorForAttributeRole(String attributeRoleLabel) {
		Color color = mapAttributeRoleNamesToColors.get(attributeRoleLabel);
		if (color == null) {
			color = mapAttributeRoleNamesToColors.get(GENERIC_SPECIAL_ATTRIBUTE_NAME);
		}

		return color;
	}

	/**
	 * Returns the {@link Color} used to represent the given {@link Ontology#ATTRIBUTE_VALUE_TYPE}.
	 *
	 * @param valueType
	 * @return
	 */
	public static Color getColorForValueType(int valueType) {
		Color color = mapOfValueTypeColors.get(valueType);
		while (color == null) {
			valueType = Ontology.ATTRIBUTE_VALUE_TYPE.getParent(valueType);
			color = mapOfValueTypeColors.get(valueType);
		}

		return color;
	}
}
