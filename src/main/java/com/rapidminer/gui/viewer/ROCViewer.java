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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.operator.performance.AreaUnderCurve;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.I18N;


/**
 * This viewer can be used to show the ROC curve for the given ROC data. It is also able to display
 * the average values of averaged ROC curves together with their standard deviations.
 *
 * @author Ingo Mierswa, Nils Woehler
 */
public class ROCViewer extends JPanel implements Renderable, PrintableComponent {

	private static final long serialVersionUID = -5441366103559588567L;

	private ROCChartPlotter plotter;

	private String criterionName;

	public ROCViewer(AreaUnderCurve auc) {
		setLayout(new BorderLayout());

		String message = auc.toString();

		criterionName = auc.getName();

		// info string
		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		infoPanel.setOpaque(true);
		infoPanel.setBackground(Colors.WHITE);
		JTextPane infoText = new JTextPane();
		infoText.setEditable(false);
		infoText.setBackground(infoPanel.getBackground());
		infoText.setFont(infoText.getFont().deriveFont(Font.BOLD));
		infoText.setText(message);
		infoPanel.add(infoText);
		add(infoPanel, BorderLayout.NORTH);

		// plot panel
		plotter = new ROCChartPlotter();
		plotter.addROCData("ROC", auc.getRocData());
		JPanel innerPanel = new JPanel(new BorderLayout());
		innerPanel.add(plotter, BorderLayout.CENTER);
		innerPanel.setBorder(BorderFactory.createMatteBorder(5, 0, 10, 10, Colors.WHITE));
		add(innerPanel, BorderLayout.CENTER);
	}

	@Override
	public void prepareRendering() {
		plotter.prepareRendering();
	}

	@Override
	public void finishRendering() {
		plotter.finishRendering();
	}

	@Override
	public int getRenderHeight(int preferredHeight) {
		return plotter.getRenderHeight(preferredHeight);
	}

	@Override
	public int getRenderWidth(int preferredWidth) {
		return plotter.getRenderWidth(preferredWidth);
	}

	@Override
	public void render(Graphics graphics, int width, int height) {
		plotter.render(graphics, width, height);
	}

	@Override
	public Component getExportComponent() {
		return plotter;
	}

	@Override
	public String getExportName() {
		return I18N.getGUIMessage("gui.cards.result_view.roc_curve.title");
	}

	@Override
	public String getIdentifier() {
		return criterionName;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.roc_curve.icon");
	}
}
