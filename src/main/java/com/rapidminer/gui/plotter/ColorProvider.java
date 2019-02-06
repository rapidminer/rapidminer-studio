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
package com.rapidminer.gui.plotter;

import java.awt.Color;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;


/**
 * This class delivers colors according to the user settings.
 *
 * @author Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class ColorProvider {

	private static Color minColor;
	private static Color maxColor;

	/** used as default before Studio 7.5 */
	private static final Color MIN_DEFAULT_COLOR_PRE_75 = new Color(58, 58, 255);
	/** used as default before Studio 7.5 */
	private static final Color MAX_DEFAULT_COLOR_PRE_75 = new Color(255, 48, 48);

	public static final Color MIN_DEFAULT_COLOR = new Color(73, 144, 226);
	public static final Color MAX_DEFAULT_COLOR = new Color(232, 76, 61);

	static {
		// update colors when user changes them in the settings
		ParameterService.registerParameterChangeListener(new ParameterChangeListener() {

			@Override
			public void informParameterSaved() {
				// ignore
			}

			@Override
			public void informParameterChanged(String key, String value) {
				if (MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR.equals(key)) {
					minColor = getColorFromProperty(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR,
							MIN_DEFAULT_COLOR);
				}
				if (MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR.equals(key)) {
					maxColor = getColorFromProperty(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR,
							MAX_DEFAULT_COLOR);
				}
			}
		});

		minColor = getColorFromProperty(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR, MIN_DEFAULT_COLOR);
		boolean fixed = false;
		// this was the previous default and we cannot detect if the user has ever changed it so we
		// force the new scheme here
		if (minColor.equals(Color.BLUE) || minColor.equals(MIN_DEFAULT_COLOR_PRE_75)) {
			ParameterService.setParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MINCOLOR,
					ParameterTypeColor.color2String(MIN_DEFAULT_COLOR));
			fixed = true;
		}
		maxColor = getColorFromProperty(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR, MAX_DEFAULT_COLOR);
		// this was the previous default and we cannot detect if the user has ever changed it so we
		// force the new scheme here
		if (maxColor.equals(Color.RED) || maxColor.equals(MAX_DEFAULT_COLOR_PRE_75)) {
			ParameterService.setParameterValue(MainFrame.PROPERTY_RAPIDMINER_GUI_PLOTTER_LEGEND_MAXCOLOR,
					ParameterTypeColor.color2String(MAX_DEFAULT_COLOR));
			fixed = true;
		}

		if (fixed) {
			ParameterService.saveParameters();
		}
	}

	private boolean reduceBrightness;

	/**
	 * Creates a new {@link ColorProvider}.
	 */
	public ColorProvider() {
		this(false);
	}

	/**
	 * Creates a new {@link ColorProvider} which reduces the brightness of the returned colors.
	 *
	 * @param reduceBrightness
	 *            if <code>true</code>, will reduce brightness of returned colors
	 */
	public ColorProvider(boolean reduceBrightness) {
		this.reduceBrightness = reduceBrightness;
	}

	public Color getMinLegendColor() {
		return minColor;
	}

	public Color getMaxLegendColor() {
		return maxColor;
	}

	/**
	 * Helper methods which can be used to deliver a value for the point color. For nominal values
	 * with two classes, this method tries to search another column with a name xxx(name) and
	 * changes the color a bit to the opponent color if the values are not the same. This might be
	 * nice for example in case of a predicted value and a real value.
	 */
	public double getPointColorValue(DataTable table, DataTableRow row, int column, double min, double max) {
		double colorValue = row.getValue(column);
		if (max == min && table.isNominal(column)) {
			return colorValue / (table.getNumberOfValues(column) - 1);
		} else {
			double normalized = (colorValue - min) / (max - min);
			if (!Double.isNaN(colorValue)) {
				if (table.isNominal(column) && table.getNumberOfValues(column) == 2) {
					String columnName = table.getColumnName(column);
					int startParIndex = columnName.indexOf("(") + 1;
					if (startParIndex >= 0) {
						int endParIndex = columnName.indexOf(")", startParIndex);
						if (endParIndex >= 0) {
							String otherColumnName = columnName.substring(startParIndex, endParIndex);
							int otherColumnIndex = table.getColumnIndex(otherColumnName);
							if (otherColumnIndex >= 0) {
								if (table.isNominal(otherColumnIndex)) {
									double compareValue = row.getValue(otherColumnIndex);
									if (!Double.isNaN(compareValue)) {
										int compareIndex = (int) compareValue;
										String compareString = table.mapIndex(otherColumnIndex, compareIndex);
										compareIndex = table.mapString(column, compareString);
										if (colorValue != compareIndex) {
											// both values are different --> change color
											if (normalized > 0.8) {
												normalized = 0.8;
											} else if (normalized < 0.2) {
												normalized = 0.2;
											}
										}
									}
								}
							}
						}
					}
				}
			}
			return normalized;
		}
	}

	public Color getPointBorderColor(DataTable table, DataTableRow row, int column) {
		Color result = Color.BLACK;
		if (table.isNominal(column)) { // nominal --> try to find compare column
			double colorValue = row.getValue(column);
			if (!Double.isNaN(colorValue)) {
				int colorIndex = (int) colorValue;
				String columnName = table.getColumnName(column);
				int startParIndex = columnName.indexOf("(") + 1;
				if (startParIndex >= 0) {
					int endParIndex = columnName.indexOf(")", startParIndex);
					if (endParIndex >= 0) {
						String otherColumnName = columnName.substring(startParIndex, endParIndex);
						int otherColumnIndex = table.getColumnIndex(otherColumnName);
						if (otherColumnIndex >= 0) {
							if (table.isNominal(otherColumnIndex)) {
								double compareValue = row.getValue(otherColumnIndex);
								if (!Double.isNaN(compareValue)) {
									int compareIndex = (int) compareValue;
									String compareString = table.mapIndex(otherColumnIndex, compareIndex);
									compareIndex = table.mapString(column, compareString);
									if (colorIndex != compareIndex) {
										// both values are different --> change color
										result = Color.RED;
									}
								}
							}
						}
					}
				}
			}
		}
		if (reduceBrightness) {
			return reduceColorBrightness(result);
		} else {
			return result;
		}
	}

	/**
	 * Returns a color for the given value. The value must be normalized, i.e. between zero and one.
	 */
	public Color getPointColor(double value) {
		return getPointColor(value, 255);
	}

	/**
	 * Returns a color for the given value. The value must be normalized, i.e. between zero and one.
	 * Please note that high alpha values are more transparent.
	 */
	public Color getPointColor(double value, int alpha) {
		if (Double.isNaN(value)) {
			return Color.LIGHT_GRAY;
		}
		Color MIN_LEGEND_COLOR = getMinLegendColor();
		Color MAX_LEGEND_COLOR = getMaxLegendColor();
		float[] minCol = Color.RGBtoHSB(MIN_LEGEND_COLOR.getRed(), MIN_LEGEND_COLOR.getGreen(), MIN_LEGEND_COLOR.getBlue(),
				null);
		float[] maxCol = Color.RGBtoHSB(MAX_LEGEND_COLOR.getRed(), MAX_LEGEND_COLOR.getGreen(), MAX_LEGEND_COLOR.getBlue(),
				null);
		// double hColorDiff = 1.0f - 0.68f;
		double hColorDiff = maxCol[0] - minCol[0];
		double sColorDiff = maxCol[1] - minCol[1];
		double bColorDiff = maxCol[2] - minCol[2];

		// lower brightness to 90%
		Color color = new Color(Color.HSBtoRGB((float) (minCol[0] + hColorDiff * value),
				(float) (minCol[1] + value * sColorDiff), (float) (minCol[2] + value * bColorDiff)));

		if (alpha < 255) {
			color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
		}

		if (reduceBrightness) {
			color = reduceColorBrightness(color);
		}
		return color;
	}

	/**
	 * Returns the original color, just slightly less bright.
	 *
	 * @param color
	 * @return
	 */
	public static Color reduceColorBrightness(Color color) {
		// lower brightness to 85% and saturation to 85%
		int r, g, b;
		float[] hsb = new float[3];
		r = color.getRed();
		g = color.getGreen();
		b = color.getBlue();
		Color.RGBtoHSB(r, g, b, hsb);
		// brightness
		hsb[2] *= 0.80f;
		// saturation
		hsb[1] *= 0.85f;
		return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
	}

	private static Color getColorFromProperty(String propertyName, Color errorColor) {
		String propertyString = ParameterService.getParameterValue(propertyName);
		if (propertyString != null) {
			String[] colors = propertyString.split(",");
			if (colors.length != 3) {
				throw new IllegalArgumentException("Color '" + propertyString + "' defined as value for property '"
						+ propertyName + "' is not a vaild color. Colors must be of the form 'r,g,b'.");
			} else {
				Color color = new Color(Integer.parseInt(colors[0].trim()), Integer.parseInt(colors[1].trim()),
						Integer.parseInt(colors[2].trim()));
				return color;
			}
		} else {
			return errorColor;
		}
	}
}
