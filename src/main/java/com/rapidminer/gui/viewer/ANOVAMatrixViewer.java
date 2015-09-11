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

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.visualization.dependencies.ANOVAMatrix;
import com.rapidminer.tools.Tools;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;


/**
 * This viewer class can be used to display the significant differences of numerical columns
 * depending on the groups defined by nominal columns. The result will be a type of
 * {@link ANOVAMatrix} which will be displayed here. The cells indicating probably significant
 * differences between the groups will be printed with a darker background color.
 * 
 * @author Ingo Mierswa
 */
public class ANOVAMatrixViewer extends JPanel {

	private static final long serialVersionUID = 2307394762389768556L;

	public ANOVAMatrixViewer(ANOVAMatrix matrix) {
		super(new BorderLayout());

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		// info string
		JTextPane infoText = new JTextPane();
		infoText.setEditable(false);
		infoText.setBackground(infoPanel.getBackground());
		infoText.setFont(infoText.getFont().deriveFont(Font.BOLD));
		infoText.setText("A dark background indicates that the probability for non-difference between the groups is less than "
				+ Tools.formatNumber(matrix.getSignificanceLevel()));
		infoPanel.add(infoText);
		infoPanel.setBorder(BorderFactory.createEtchedBorder());
		add(infoPanel, BorderLayout.NORTH);

		// table
		ANOVAMatrixViewerTable table = new ANOVAMatrixViewerTable(matrix);
		table.setBorder(BorderFactory.createEtchedBorder());
		add(new ExtendedJScrollPane(table), BorderLayout.CENTER);
	}
}
