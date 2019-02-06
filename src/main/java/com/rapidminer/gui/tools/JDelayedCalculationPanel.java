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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;


/**
 * A panel for delayed calculations.
 * 
 * @author Sebastian Land
 */
public class JDelayedCalculationPanel extends JPanel {

	private static final long serialVersionUID = -6010071394984207389L;

	private final GridBagLayout layout = new GridBagLayout();
	private GridBagConstraints constraints = new GridBagConstraints();
	private JButton startButton = new JButton("Start calculation");
	private JLabel calculationLabel = new JLabel("Calculation started...");
	private Thread calculationThread = null;

	public JDelayedCalculationPanel() {

		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = new Insets(4, 4, 4, 4);
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.gridwidth = GridBagConstraints.REMAINDER;
		this.setLayout(layout);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		layout.setConstraints(startButton, constraints);
		buttonPanel.add(startButton, constraints);

		super.add(buttonPanel);

		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// removing current Content
				removeAll();
				JPanel panel = new JPanel();
				GridBagLayout localLayout = new GridBagLayout();
				panel.setLayout(localLayout);
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.insets = new Insets(4, 4, 4, 4);
				c.weightx = 0;
				c.weighty = 0;
				c.gridwidth = GridBagConstraints.REMAINDER;
				localLayout.setConstraints(calculationLabel, c);
				panel.add(calculationLabel);

				JProgressBar bar = new JProgressBar();
				bar.setIndeterminate(true);
				localLayout.setConstraints(bar, c);
				panel.add(bar);

				constraints.weightx = 1;
				constraints.weighty = 1;
				layout.setConstraints(panel, constraints);
				add(panel);
				revalidate();
				repaint();

				// performing thread
				getCalculationThread().start();
			}
		});
	}

	public Thread getCalculationThread() {
		return calculationThread;
	}

	@Override
	public Component add(Component comp) {
		constraints.weightx = 1;
		constraints.weighty = 1;
		layout.setConstraints(comp, constraints);
		return super.add(comp);
	}

	public void setDelayThread(Thread delayThread) {
		this.calculationThread = delayThread;
	}
}
