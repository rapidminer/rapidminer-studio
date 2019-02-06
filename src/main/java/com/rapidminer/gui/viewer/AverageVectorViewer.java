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
package com.rapidminer.gui.viewer;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.report.Readable;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.AverageVector;


/**
 *
 * @author Sebastian Land
 */
public class AverageVectorViewer extends JPanel implements Readable {

	private static final long serialVersionUID = -5108739438512582933L;

	private AverageVector vector;

	public AverageVectorViewer(AverageVector vector, IOContainer container) {
		setLayout(new GridLayout(1, 1));
		this.vector = vector;

		JPanel mainPanel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		mainPanel.setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(11, 11, 11, 11);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0d;
		c.weighty = 0.0d;

		JLabel mainLabel = new JLabel("<html><h2>" + getName() + " (" + vector.size() + ")</h2></html>");
		layout.setConstraints(mainLabel, c);
		mainPanel.add(mainLabel);

		for (int i = 0; i < vector.size(); i++) {
			Averagable avg = vector.getAveragable(i);
			Component visualizationComponent = ResultDisplayTools.createVisualizationComponent(avg, container, "Averagable");
			layout.setConstraints(visualizationComponent, c);
			mainPanel.add(visualizationComponent);

		}
		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(mainPanel);
		scrollPane.setBorder(null);
		add(scrollPane);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < vector.size(); i++) {
			buffer.append(vector.getAveragable(i).toString());
		}
		return buffer.toString();
	}

	@Override
	public boolean isInTargetEncoding() {
		return false;
	}
}
