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
package com.rapidminer.io.process.rules;

import com.rapidminer.io.process.XMLImporter;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.container.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This rule adapts the old parameter settings of the plotter to the new one, if they are stored in
 * a list inside a parameter of a operator.
 * 
 * @author Sebastian Land
 */
public class RenamePlotterParametersRule extends AbstractParseRule {

	private static final Collection<Pair<String, String>> REPLACEMENTS = new ArrayList<Pair<String, String>>();

	static {
		REPLACEMENTS.add(new Pair<String, String>("scatterplotter2_", "scatter_"));
		REPLACEMENTS.add(new Pair<String, String>("multiplescatterplotter_", "scatter_multiple_"));
		REPLACEMENTS.add(new Pair<String, String>("scattermatrixplotter_", "scatter_matrix_"));
		REPLACEMENTS.add(new Pair<String, String>("scatterplot3d_", "scatter_3d_"));
		REPLACEMENTS.add(new Pair<String, String>("scatterplot3dcolor_", "scatter_3d_color_"));
		REPLACEMENTS.add(new Pair<String, String>("bubblechartplotter_", "bubble_"));
		REPLACEMENTS.add(new Pair<String, String>("parallelplotter2_", "parallel_"));
		REPLACEMENTS.add(new Pair<String, String>("deviationchartplotter_", "deviation_"));
		REPLACEMENTS.add(new Pair<String, String>("multipleserieschartplotter_", "series_multiple_"));
		REPLACEMENTS.add(new Pair<String, String>("serieschartplotter_", "series_"));
		REPLACEMENTS.add(new Pair<String, String>("surveyplotter_", "survey_"));
		REPLACEMENTS.add(new Pair<String, String>("somplotter_", "som_"));
		REPLACEMENTS.add(new Pair<String, String>("blockchartplotter_", "block_"));
		REPLACEMENTS.add(new Pair<String, String>("densityplotter_", "density_"));
		REPLACEMENTS.add(new Pair<String, String>("piechart2dplotter_", "pie_"));
		REPLACEMENTS.add(new Pair<String, String>("piechart3dplotter_", "pie_3d_"));
		REPLACEMENTS.add(new Pair<String, String>("ringchartplotter_", "ring_"));
		REPLACEMENTS.add(new Pair<String, String>("barchartplotter_", "bars_"));
		REPLACEMENTS.add(new Pair<String, String>("paretochartplotter_", "pareto_"));
		REPLACEMENTS.add(new Pair<String, String>("andrewscurves_", "andrews_curves_"));
		REPLACEMENTS.add(new Pair<String, String>("distributionplotter_", "distribution_"));
		REPLACEMENTS.add(new Pair<String, String>("histogramchart_", "histogram_"));
		REPLACEMENTS.add(new Pair<String, String>("histogramcolorchart_", "histogram_color_"));
		REPLACEMENTS.add(new Pair<String, String>("colorquartileplotter_", "quartile_color_"));
		REPLACEMENTS.add(new Pair<String, String>("colorquartilematrixplotter_", "quartile_color_matrix_"));
		REPLACEMENTS.add(new Pair<String, String>("quartileplotter_", "quartile_"));
		REPLACEMENTS.add(new Pair<String, String>("sticksplot2d_", "sticks_"));
		REPLACEMENTS.add(new Pair<String, String>("sticksplot3d_", "sticks_3d_"));
		REPLACEMENTS.add(new Pair<String, String>("boxplot2d_", "box_"));
		REPLACEMENTS.add(new Pair<String, String>("boxplot3d_", "box_3d_"));
		REPLACEMENTS.add(new Pair<String, String>("radvizplotter_", "scatter_"));
		REPLACEMENTS.add(new Pair<String, String>("gridvizplotter_", "scatter_"));
		REPLACEMENTS.add(new Pair<String, String>("scatterplotter_", "lines_"));
	}

	private String parameter;

	/**
	 * @param operatorTypeName
	 * @param element
	 * @throws XMLException
	 */
	public RenamePlotterParametersRule(String operatorTypeName, Element element) throws XMLException {
		super(operatorTypeName, element);
		assert (element.getTagName().equals("renamePlotterParameters"));
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Element childElem = (Element) child;
				if (childElem.getTagName().equals("parameter")) {
					parameter = childElem.getTextContent();
				}
			}
		}
	}

	@Override
	protected String apply(Operator operator, String operatorTypeName, XMLImporter importer) {
		if (operator.getParameters().isSpecified(parameter)) {
			String value = operator.getParameters().getParameterOrNull(parameter);
			if (value != null) {
				List<String[]> list = ParameterTypeList.transformString2List(value);
				for (String[] pair : list) {
					if (pair[0].equals("plotter")) {
						if (pair[1].equals("RadViz") || pair[1].equals("GridViz")) {
							pair[1] = "Scatter";
						}
					} else {
						for (Pair<String, String> replacement : REPLACEMENTS) {
							pair[0] = pair[0].replace(replacement.getFirst(), replacement.getSecond());
						}
					}
				}
				operator.getParameters().setParameter(parameter, ParameterTypeList.transformList2String(list));
				return "Corrected plotter setting names in parameter <code>" + parameter + "</code> in <var>"
						+ operator.getName() + "</var>.";
			}
		}
		return null;
	}
}
