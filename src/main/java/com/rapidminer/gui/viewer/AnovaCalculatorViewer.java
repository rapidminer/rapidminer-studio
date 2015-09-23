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
package com.rapidminer.gui.viewer;

import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Tools;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


/**
 * 
 * @author Sebastian Land
 */
public class AnovaCalculatorViewer extends JPanel {

	private static final long serialVersionUID = -3590704018828402377L;

	// TODO: layout with sophisticated HTML/CSS
	public AnovaCalculatorViewer(String name, double sumSquaresBetween, int degreesOfFreedom1, double meanSquaresBetween,
			double fValue, double prob, double sumSquaresResiduals, int degreesOfFreedom2, double meanSquaresResiduals,
			double alpha) {
		this.setLayout(new GridLayout(1, 1));
		StringBuffer buffer = new StringBuffer();
		Color bgColor = SwingTools.LIGHTEST_YELLOW;
		String bgColorString = "#" + Integer.toHexString(bgColor.getRed()) + Integer.toHexString(bgColor.getGreen())
				+ Integer.toHexString(bgColor.getBlue());
		Color headerColor = SwingTools.LIGHTEST_BLUE;
		String headerColorString = "#" + Integer.toHexString(headerColor.getRed())
				+ Integer.toHexString(headerColor.getGreen()) + Integer.toHexString(headerColor.getBlue());
		buffer.append("<table border=\"1\">");
		buffer.append("<tr bgcolor=\"" + headerColorString
				+ "\"><th>Source</th><th>Square Sums</th><th>DF</th><th>Mean Squares</th><th>F</th><th>Prob</th></tr>");
		buffer.append("<tr bgcolor=\"" + bgColorString + "\"><td>Between</td><td>" + Tools.formatNumber(sumSquaresBetween)
				+ "</td><td>" + degreesOfFreedom1 + "</td><td>" + Tools.formatNumber(meanSquaresBetween) + "</td><td>"
				+ Tools.formatNumber(fValue) + "</td><td>" + Tools.formatNumber(prob) + "</td></tr>");
		buffer.append("<tr bgcolor=\"" + bgColorString + "\"><td>Residuals</td><td>"
				+ Tools.formatNumber(sumSquaresResiduals) + "</td><td>" + degreesOfFreedom2 + "</td><td>"
				+ Tools.formatNumber(meanSquaresResiduals) + "</td><td></td><td></td></tr>");
		buffer.append("<tr bgcolor=\"" + bgColorString + "\"><td>Total</td><td>"
				+ Tools.formatNumber(sumSquaresBetween + sumSquaresResiduals) + "</td><td>"
				+ (degreesOfFreedom1 + degreesOfFreedom2) + "</td><td></td><td></td><td></td></tr>");
		buffer.append("</table>");
		buffer.append("<br>Probability for random values with the same result: " + Tools.formatNumber(prob) + "<br>");
		if (prob < alpha) {
			buffer.append("Difference between actual mean values is probably significant, since " + Tools.formatNumber(prob)
					+ " &lt; alpha = " + Tools.formatNumber(alpha) + "!");
		} else {
			buffer.append("Difference between actual mean values is probably not significant, since "
					+ Tools.formatNumber(prob) + " &gt; alpha = " + Tools.formatNumber(alpha) + "!");
		}

		JEditorPane textPane = new ExtendedHTMLJEditorPane("text/html", "<html><h1>" + name + "</h1>" + buffer.toString()
				+ "</html>");
		textPane.setBackground((new JLabel()).getBackground());
		textPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(11, 11, 11, 11));
		textPane.setEditable(false);
		JScrollPane scrollPane = new ExtendedJScrollPane(textPane);
		scrollPane.setBorder(null);
		this.add(scrollPane);
	}
}
