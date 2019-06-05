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
package com.rapidminer.tools;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;

import com.rapidminer.gui.RapidMinerGUI;


/**
 * This class provides a method to create a fallback {@link Font} with respect to
 * {@link RapidMinerGUI#PROPERTY_FONT_CONFIG}. It is meant to be extended with mechanisms for e.g.
 * scaling.
 *
 * @author Marcel Seifert
 * @since 7.5.2
 */
public class FontTools {

	/**
	 * The position of standard fonts in the selectableFonts array.
	 */
	public static final int OPTION_INDEX_STANDARD_FONTS = 0;

	/**
	 * The position of system fonts in the selectableFonts array.
	 */
	public static final int OPTION_INDEX_SYSTEM_FONTS = 1;

	private static final String OPTION_STANDARD_FONTS = "Standard fonts";

	private static final String OPTION_SYSTEM_FONTS = "System fonts";

	/**
	 * The {@link Font} that should be used when system fonts is selected.
	 */
	private static final String SYSTEM_FALLBACK_FONT = Font.DIALOG;

	/**
	 * Java's logical font families.
	 */
	private static final Set<String> LOGICAL_FONT_FAMILIES = new HashSet<>(5, 1F);
	static {
		LOGICAL_FONT_FAMILIES.add(Font.DIALOG);
		LOGICAL_FONT_FAMILIES.add(Font.DIALOG_INPUT);
		LOGICAL_FONT_FAMILIES.add(Font.SANS_SERIF);
		LOGICAL_FONT_FAMILIES.add(Font.SERIF);
		LOGICAL_FONT_FAMILIES.add(Font.MONOSPACED);
	}

	/**
	 * This should be used everywhere as a replacement for {@link Font#Font(String, int, int)}.
	 * Returns a {@link Font} which can be one of the following depending on the value of
	 * {@link RapidMinerGUI#PROPERTY_FONT_CONFIG}:
	 *
	 * 1. Standard fonts: {@link StyleContext#getDefaultStyleContext()#getFont(String, int, int)}
	 * will be returned, which is a composite Font of the delivered with a fallback for unknown
	 * glyphs, which works well in the most cases.
	 *
	 * 2. System fonts: Replaces all fonts expect {@link Font#DIALOG}, {@link Font#DIALOG_INPUT},
	 * {@link Font#SANS_SERIF}, {@link Font#SERIF}, {@link Font#MONOSPACED} with
	 * {@link Font#DIALOG}.
	 *
	 * 3. Custom fonts: The family will be replaced by the user selection
	 * {@link RapidMinerGUI#PROPERTY_FONT_CONFIG}.
	 *
	 * @param family
	 *            font family string
	 * @param style
	 *            style int
	 * @param size
	 *            size int
	 * @return A {@link Font} with respect to {@link RapidMinerGUI#PROPERTY_FONT_CONFIG}.
	 */
	public static Font getFont(String family, int style, int size) {
		String fontConfig = getFontConfig();
		Font font;
		switch (fontConfig) {
			case OPTION_STANDARD_FONTS:
				font = StyleContext.getDefaultStyleContext().getFont(family, style, size);
				break;
			case OPTION_SYSTEM_FONTS:
				String systemFamily = family;
				if (!LOGICAL_FONT_FAMILIES.contains(family)) {
					systemFamily = SYSTEM_FALLBACK_FONT;
				}
				font = new Font(systemFamily, style, size);
				break;
			default:
				font = StyleContext.getDefaultStyleContext().getFont(fontConfig, style, size);
				break;
		}
		return font;
	}

	/**
	 * This should be called during the GUI initialization after applying LAF. The UI fonts will be
	 * replaced according to the user's wishes.
	 */
	public static void checkAndSetFallbackUIFont() {
		String fontConfig = getFontConfig();
		if (OPTION_STANDARD_FONTS.equals(fontConfig)) {
			return;
		}

		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof FontUIResource) {
				FontUIResource orig = (FontUIResource) value;
				String family = orig.getFamily();

				if (OPTION_SYSTEM_FONTS.equals(fontConfig)) {
					// replace all non-system fonts with SYSTEM_FALLBACK_FONT
					if (LOGICAL_FONT_FAMILIES.contains(family)) {
						continue;
					}
					family = SYSTEM_FALLBACK_FONT;
				} else {
					// replace all fonts with specified family
					if (family.equals(fontConfig)) {
						continue;
					}
					family = fontConfig;
				}

				Font font = StyleContext.getDefaultStyleContext().getFont(family, orig.getStyle(), orig.getSize());
				UIManager.put(key, new FontUIResource(font));
			}
		}
	}

	/**
	 * Gets the available fonts on this system and returns them as a String array. At the beginning
	 * of the array 2 entries will be inserted, one representing the standard fonts and one
	 * representing the system fonts.
	 *
	 * @return array of available font options
	 */
	public static String[] getAvailableFonts() {
		String[] fonts;
		try {
			fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		} catch (Throwable e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.FontTools.system_font_loading.failed", e);
			fonts = new String[0];
		}
		String[] selectableFonts = new String[fonts.length + 2];
		selectableFonts[OPTION_INDEX_STANDARD_FONTS] = OPTION_STANDARD_FONTS;
		selectableFonts[OPTION_INDEX_SYSTEM_FONTS] = OPTION_SYSTEM_FONTS;
		System.arraycopy(fonts, 0, selectableFonts, 2, fonts.length);
		return selectableFonts;
	}

	private static String getFontConfig() {
		return ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_FONT_CONFIG);
	}

}
