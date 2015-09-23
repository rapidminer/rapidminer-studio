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
package com.rapidminer.gui.new_plotter.templates;

/**
 * This is a wrapper for {@link PlotterTemplate}s.
 * 
 * @author Marco Boeck
 * 
 */
public class PlotterTemplateWrapper {

	/** the wrapped template */
	private PlotterTemplate template;

	/**
	 * Creates a new {@link PlotterTemplateWrapper} around the given {@link PlotterTemplate}.
	 * <p>
	 * The template can later be switched via {@link #setPlotterTemplate(PlotterTemplate)}.
	 * 
	 * @param template
	 */
	public PlotterTemplateWrapper(PlotterTemplate template) {
		this.template = template;
	}

	/**
	 * Sets the new {@link PlotterTemplate} for this wrapper.
	 * 
	 * @param template
	 */
	public void setPlotterTemplate(PlotterTemplate template) {
		this.template = template;
	}

	/**
	 * Returns the currently wrapped {@link PlotterTemplate}.
	 * 
	 * @return
	 */
	public PlotterTemplate getPlotterTemplate() {
		return template;
	}

}
