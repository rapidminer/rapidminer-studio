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
package com.rapidminer.gui.renderer.performance;

import com.rapidminer.gui.renderer.AbstractRenderer;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.validation.significance.TTestSignificanceTestOperator.TTestSignificanceTestResult;
import com.rapidminer.report.Reportable;
import com.rapidminer.tools.Tools;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JEditorPane;
import javax.swing.JLabel;


/**
 * 
 * @author Sebastian Land
 */
public class TTestSignificanceTestRenderer extends AbstractRenderer {

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		// TODO: can do anything else than text output?
		return (com.rapidminer.report.Readable) renderable;
	}

	@Override
	public String getName() {
		return "T-Test Significance";
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		TTestSignificanceTestResult result = (TTestSignificanceTestResult) renderable;

		PerformanceVector[] allVectors = result.getAllVectors();
		StringBuffer buffer = new StringBuffer();
		Color bgColor = SwingTools.LIGHTEST_YELLOW;
		String bgColorString = Integer.toHexString(bgColor.getRed()) + Integer.toHexString(bgColor.getGreen())
				+ Integer.toHexString(bgColor.getBlue());

		buffer.append("<table bgcolor=\"" + bgColorString + "\" border=\"1\">");
		buffer.append("<tr><td></td>");
		for (int i = 0; i < result.getAllVectors().length; i++) {
			buffer.append("<td>" + Tools.formatNumber(allVectors[i].getMainCriterion().getAverage()) + " +/- "
					+ Tools.formatNumber(Math.sqrt(allVectors[i].getMainCriterion().getVariance())) + "</td>");
		}
		buffer.append("</tr>");
		for (int i = 0; i < allVectors.length; i++) {
			buffer.append("<tr><td>" + Tools.formatNumber(allVectors[i].getMainCriterion().getAverage()) + " +/- "
					+ Tools.formatNumber(Math.sqrt(allVectors[i].getMainCriterion().getVariance())) + "</td>");
			for (int j = 0; j < allVectors.length; j++) {
				buffer.append("<td>");
				if (!Double.isNaN(result.getProbMatrix()[i][j])) {
					double prob = result.getProbMatrix()[i][j];
					if (prob < result.getAlpha()) {
						buffer.append("<b>");
					}
					buffer.append(Tools.formatNumber(prob));
					if (prob < result.getAlpha()) {
						buffer.append("</b>");
					}
				}
				buffer.append("</td>");
			}
			buffer.append("</tr>");
		}
		buffer.append("</table>");
		buffer.append("<br>Probabilities for random values with the same result.<br>Bold values are smaller than alpha="
				+ Tools.formatNumber(result.getAlpha())
				+ " which indicates a probably significant difference between the actual mean values!");

		JEditorPane textPane = new ExtendedHTMLJEditorPane("text/html", "<html><h1>" + getName() + "</h1>"
				+ buffer.toString() + "</html>");
		textPane.setBackground((new JLabel()).getBackground());
		textPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(11, 11, 11, 11));
		textPane.setEditable(false);
		return new ExtendedJScrollPane(textPane);
	}

}
