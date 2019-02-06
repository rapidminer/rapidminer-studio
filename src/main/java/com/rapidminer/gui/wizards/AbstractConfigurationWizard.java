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
package com.rapidminer.gui.wizards;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;


/**
 * This class is the creator for wizard dialogs defining the configuration for
 * {@link com.rapidminer.operator.io.ExampleSource ExampleSource} operators.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractConfigurationWizard extends JDialog {

	private static final Action DUMMY_NEXT_ACTION = new ResourceActionAdapter("next");

	private static final Action DUMMY_FINISH_ACTION = new ResourceActionAdapter("finish");

	private static final long serialVersionUID = -2633062859175838003L;

	private JButton previous = new JButton(new ResourceAction("previous") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			step(-1);
			next.updateContent();
		}
	});

	private NextButton next = new NextButton();

	private class NextButton extends JButton {

		private static final long serialVersionUID = 1L;

		public NextButton() {
			super(DUMMY_NEXT_ACTION);
			addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					step();
				}
			});
		}

		private void step() {
			AbstractConfigurationWizard.this.step(1);
			updateContent();
		}

		private void updateContent() {
			if (currentStep == numberOfSteps - 1) {
				configurePropertiesFromAction(DUMMY_FINISH_ACTION);
			} else {
				configurePropertiesFromAction(DUMMY_NEXT_ACTION);
			}
		}
	};

	private CardLayout cardLayout = new CardLayout();

	private JPanel mainPanel = new JPanel(cardLayout);

	private GridBagLayout layout = new GridBagLayout();

	private GridBagConstraints c = new GridBagConstraints();

	private JPanel contentPanel = new JPanel(layout);

	private int currentStep = 0;

	private int numberOfSteps = 0;

	private ConfigurationListener listener;

	/** Creates a new wizard. */
	public AbstractConfigurationWizard(String name, ConfigurationListener listener) {
		super(RapidMinerGUI.getMainFrame(), name, true);

		this.listener = listener;

		// button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(previous);
		buttonPanel.add(next);

		buttonPanel.add(Box.createHorizontalStrut(11));
		JButton cancel = new JButton(new ResourceAction("cancel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				cancel();
			}
		});
		buttonPanel.add(cancel);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		// main panel
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(11, 11, 11, 11);
		this.contentPanel = new JPanel(layout);
		layout.setConstraints(mainPanel, c);
		contentPanel.add(mainPanel);

		getContentPane().add(contentPanel, BorderLayout.CENTER);

		setSize(Math.max(640, (int) (0.66d * getOwner().getWidth())), Math.max(480, (int) (0.66d * getOwner().getHeight())));

		setLocationRelativeTo(getOwner());
	}

	/** The default implementation returns true. */
	public boolean validateCurrentStep(int currentStep, int newStep) {
		return true;
	}

	/**
	 * This method is invoked in the method step. Subclasses might perform some additional stuff
	 * here. If false is returned, the step change is not performed.
	 */
	protected abstract void performStepAction(int currentStep, int oldStep);

	/**
	 * This method is invoked at the end of the configuration process. Subclasses should generate
	 * the parameters object and pass it to the listener.
	 */
	protected abstract void finish(ConfigurationListener listener);

	/**
	 * Subclasses might add an additional component here which is seen during all steps, e.g. a data
	 * view table.
	 */
	protected void addBottomComponent(Component bottomComponent) {
		c.weighty = 2;
		layout.setConstraints(bottomComponent, c);
		contentPanel.add(bottomComponent);
	}

	protected int getNumberOfSteps() {
		return numberOfSteps;
	}

	protected void addStep(Component c) {
		mainPanel.add(c, numberOfSteps + "");
		numberOfSteps++;
	}

	private void step(int dir) {
		int oldStep = currentStep;
		currentStep += dir;

		if (validateCurrentStep(oldStep, currentStep)) {
			if (currentStep < 0) {
				currentStep = 0;
			}
			if (currentStep == 0) {
				previous.setEnabled(false);
			} else {
				previous.setEnabled(true);
			}

			if (currentStep >= numberOfSteps) {
				currentStep = numberOfSteps - 1;
				finish(listener);
			}

			cardLayout.show(mainPanel, currentStep + "");

			performStepAction(currentStep, oldStep);
		} else {
			currentStep = oldStep;
		}
	}

	protected void cancel() {
		dispose();
	}
}
