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
package com.rapidminer.gui.new_plotter.templates.style;

import java.awt.Font;
import java.util.Observable;

import javax.swing.JPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Abstract class which all style providers for the new plotter templates have to extend.
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class PlotterStyleProvider extends Observable {

	public static final String STYLE_ELEMENT = "style";

	/**
	 * Return the {@link JPanel} where the user can change the color/font settings.
	 * 
	 * @return
	 */
	public abstract JPanel getStyleProviderPanel();

	/**
	 * Returns the {@link String} which the user chose as the chart title.
	 * 
	 * @return
	 */
	public abstract String getTitleText();

	/**
	 * Returns <code>true</code> when the legend should be shown; <code>false</code> otherwise.
	 * 
	 * @return
	 */
	public abstract boolean isShowLegend();

	/**
	 * Returns the {@link Font} which the user chose for the axes.
	 * 
	 * @return
	 */
	public abstract Font getAxesFont();

	/**
	 * Returns the {@link Font} which the user chose for the legend.
	 * 
	 * @return
	 */
	public abstract Font getLegendFont();

	/**
	 * Returns the {@link Font} which the user chose for the title.
	 * 
	 * @return
	 */
	public abstract Font getTitleFont();

	/**
	 * Returns the {@link ColorRGB} of the background frame of the chart.
	 * 
	 * @return
	 */
	public abstract ColorRGB getFrameBackgroundColor();

	/**
	 * Returns the {@link ColorRGB} of the plot background.
	 * 
	 * @return
	 */
	public abstract ColorRGB getPlotBackgroundColor();

	/**
	 * Returns a {@link ColorScheme} instance which will be used to color the plot(s). Each plot
	 * will use one of the colors in the order provided, if more plots than colors exist, it will
	 * start from the beginning.
	 * 
	 * @return
	 */
	public abstract ColorScheme getColorScheme();

	/**
	 * Creates an {@link Element} which contains all {@link PlotterStyleProvider} settings.
	 * 
	 * @param document
	 *            the {@link Document} where the {@link Element} will be written to
	 * @return the styleElement {@link Element}
	 */
	public abstract Element createXML(Document document);

	/**
	 * Loads all {@link PlotterStyleProvider} settings from the given {@link Element}.
	 * 
	 * @param styleElement
	 *            the {@link Element} where all settings are loaded from
	 */
	public abstract void loadFromXML(Element styleElement);

	/**
	 * Uses all settings from the given {@link PlotterStyleProvider} to overwrite his own settings.
	 * Does not overwrite the title text.
	 * 
	 * @param provider
	 */
	public abstract void copySettingsFromPlotterStyleProvider(PlotterStyleProvider provider);
}
