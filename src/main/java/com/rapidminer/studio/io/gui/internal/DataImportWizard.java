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
package com.rapidminer.studio.io.gui.internal;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.source.DataSource;
import com.rapidminer.core.io.data.source.DataSourceFactory;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.core.io.gui.WizardStep.ButtonState;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;


/**
 * The {@link DataImportWizard} is the wizard dialog shown to import any kind of data into
 * RapidMiner Studio. Use the {@link DataImportWizardBuilder} to construct a new instance.
 *
 * @author Nils Woehler
 * @since 7.0.0
 */
final class DataImportWizard extends ButtonDialog implements ImportWizard {

	private static final long serialVersionUID = 1L;

	/**
	 * A loading icon
	 */
	private static final ImageIcon LOADING_ICON = SwingTools.createIcon("16/loading.gif");

	/**
	 * A template for the header which includes a HTML progress bar.
	 */
	private static final String INFO_LABEL_TEXT_TEMPLATE = "<div style='text-align:center;'><h2>%s</h2>"
			+ "<div style='background-color: #BBBBBB; width: 100%%;height: 4px;'><div style='width: %s; background-color: #34AD65;height: 4px;'>"
			+ "</div></div>";

	private int progress = 0;

	private final JButton previousButton = new JButton(new ResourceAction("previous") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			previousStep();
		}

	});

	private final JButton nextButton = new JButton(new ResourceAction("next") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			nextStep();
		}

	});

	private final Action finishAction = new ResourceAction("finish") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			// only execute the action in case the finish button is visible and enabled
			if (finishButton.isVisible() && finishButton.isEnabled()) {
				disableButtons();
				new Thread(() -> {
					try {
						getCurrentStep().viewWillBecomeInvisible(WizardDirection.NEXT);
						SwingTools.invokeLater(() -> accept(true));
					} catch (InvalidConfigurationException e1) {
						updateButtons();
					}
				}).start();
			}
		}
	};

	private final JButton finishButton = new JButton(finishAction);

	private final JButton cancelButton;

	/**
	 * Listens for changes and updates the button panel and info header.
	 */
	private final ChangeListener stepChangeListener = e -> {
		updateButtons();
		updateInfoHeader();
	};

	private final JPanel cardPanel;
	private final CardLayout cardLayout;
	private final List<WizardStep> steps;
	private final Icon previousIcon;
	private final Icon nextIcon;
	private DataSource dataSource;

	private String currentStepID;
	private List<String> previousStepIDs;

	/**
	 * Constructs a new instance of the {@link DataImportWizard}.
	 *
	 * @param owner
	 * 		the dialog owner
	 * @param modalityType
	 * 		the modality type
	 * @param graphicsConfig
	 * 		the graphics config. Might be <code>null</code> if no special config should be
	 * 		used.
	 */
	DataImportWizard(Window owner, ModalityType modalityType, GraphicsConfiguration graphicsConfig) {
		super(owner, "io.dataimport.import_wizard", modalityType, graphicsConfig);
		this.cardLayout = new CardLayout();
		this.cardPanel = new JPanel(cardLayout);
		this.steps = new LinkedList<>();
		this.previousStepIDs = new LinkedList<>();
		this.cancelButton = makeCancelButton();
		this.previousIcon = previousButton.getIcon();
		this.nextIcon = nextButton.getIcon();
		setResizable(false);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(WindowEvent e) {
				if (wasConfirmed()) {
					DataSourceFactory<?> factory = DataSourceFactoryRegistry.INSTANCE.lookUp(getDataSource().getClass());
					DataImportWizardUtils.logStats(DataWizardEventType.CLOSED, "finished - " + factory.getI18NKey());
				} else {
					if (getDataSource() != null) {
						DataSourceFactory<?> factory = DataSourceFactoryRegistry.INSTANCE.lookUp(getDataSource().getClass());
						DataImportWizardUtils.logStats(DataWizardEventType.CLOSED, "aborted - " + factory.getI18NKey());
					} else {
						DataImportWizardUtils.logStats(DataWizardEventType.CLOSED, "aborted - no datasource");
					}
				}

				closeDataSource();
			}
		});
	}

	/**
	 * Updates the dialog button, title, header and shows the {@link WizardStep} referenced by
	 * the provided stepId.
	 */
	void showStep(final String stepId, WizardDirection direction) {

		// log step change
		switch (direction) {
			case NEXT:
				DataImportWizardUtils.logStats(DataWizardEventType.NEXT_STEP, stepId);
				break;
			case PREVIOUS:
				DataImportWizardUtils.logStats(DataWizardEventType.PREVIOUS_STEP, stepId);
				break;
			case STARTING:
				DataImportWizardUtils.logStats(DataWizardEventType.STARTING, stepId);
				break;
			default:
				// ignore
				break;
		}

		// lookup step
		WizardStep importWizardStep = getStep(stepId);

		// and notify the step that it will become visible now
		try {
			importWizardStep.viewWillBecomeVisible(direction);
		} catch (InvalidConfigurationException e) {
			return;
		}

		// update current and previous step ID
		if (currentStepID != null && direction != WizardDirection.PREVIOUS) {
			this.previousStepIDs.add(currentStepID);
		}
		this.currentStepID = stepId;

		SwingTools.invokeLater(() -> {
			updateButtons();
			updateTitle();
			updateInfoHeader();
			// show step
			cardLayout.show(cardPanel, stepId);
		});

	}

	private void updateTitle() {
		setTitle(getDialogTitle() + " - " + getStepTitle());
	}

	private void updateInfoHeader() {
		if (getCurrentStep() == null) {
			return;
		}
		if (infoTextLabel != null) {
			SwingTools.disableClearType(infoTextLabel);
			infoTextLabel.setText(String.format(INFO_LABEL_TEXT_TEMPLATE, getStepTitle(), progress + "%"));
		}
		try {
			getCurrentStep().validate();
		} catch (InvalidConfigurationException e) {
			// ignore
		}

	}

	private String getStepTitle() {
		return I18N.getGUIMessage("gui.dialog.io.dataimport.step." + getCurrentStep().getI18NKey() + ".title");
	}

	/**
	 * Updates the button status within the EDT by calling {@link SwingTools#invokeLater(Runnable)}.
	 */
	private void updateButtons() {
		SwingTools.invokeLater(() -> {
			// adapt previous button
			WizardStep currentStep = getCurrentStep();
			if (currentStep == null) {
				return;
			}

			previousButton.setIcon(previousIcon);
			previousButton.setEnabled(
					currentStep.getPreviousButtonState() == ButtonState.ENABLED && !previousStepIDs.isEmpty());
			previousButton.setVisible(currentStep.getPreviousButtonState() != ButtonState.HIDDEN);

			// adapt next and finish buttons
			nextButton.setIcon(nextIcon);
			nextButton.setEnabled(currentStep.getNextButtonState() == ButtonState.ENABLED);
			boolean isLastStep = isLastStep(currentStep);
			nextButton.setVisible(!isLastStep && currentStep.getNextButtonState() != ButtonState.HIDDEN);

			finishButton.setEnabled(currentStep.getNextButtonState() == ButtonState.ENABLED);
			finishButton.setVisible(isLastStep && currentStep.getNextButtonState() != ButtonState.HIDDEN);

			cancelButton.setEnabled(true);
		});
	}

	private void disableButtons() {
		previousButton.setEnabled(false);
		nextButton.setEnabled(false);
		finishButton.setEnabled(false);
		cancelButton.setEnabled(false);
	}

	/**
	 * Check to see if the given step is the last step or if there is at least one more step.
	 *
	 * @param step
	 * 		a {@link WizardStep}
	 * @return true if the given step has no following step
	 */
	private boolean isLastStep(WizardStep step) {
		return step != null && step.getNextStepID() == null;
	}

	/**
	 * Looks up the {@link WizardStep} for the provided step ID.
	 *
	 * @param stepId
	 * 		the step ID used to lookup the requested {@link WizardStep}
	 * @return either the {@link WizardStep} for the provided ID or <code>null</code> if no
	 * step could be found
	 */
	private WizardStep getStep(String stepId) {
		for (WizardStep step : steps) {
			if (step.getI18NKey().equals(stepId)) {
				return step;
			}
		}
		return null;
	}

	/**
	 * @return the current step. Might return {@code null} in case there is no step yet.
	 */
	private WizardStep getCurrentStep() {
		return getStep(currentStepID);
	}

	/**
	 * Layouts the dialog by adding the dialog main content and updating the current shown step.
	 * Should only be called once after the dialog instance has been created.
	 *
	 * @param size
	 * 		the dialog size.
	 */
	void layoutDefault(int size, String startingStepId) {
		super.layoutDefault(cardPanel, size, previousButton, nextButton, finishButton, cancelButton);

		// fix button size such that it is not affected by icon change
		final Dimension nextSize = new Dimension(nextButton.getWidth(), nextButton.getHeight());
		nextButton.setMinimumSize(nextSize);
		nextButton.setPreferredSize(nextSize);
		final Dimension previousSize = new Dimension(previousButton.getWidth(), previousButton.getHeight());
		previousButton.setMinimumSize(previousSize);
		previousButton.setPreferredSize(previousSize);

		showStep(startingStepId, WizardDirection.STARTING);
	}

	@Override
	public void addStep(WizardStep newStep) {

		// check if step with same key was registered before and remove if true
		WizardStep oldStep = getStep(newStep.getI18NKey());
		if (oldStep != null) {
			removeStep(oldStep);
		}

		// add new step
		this.steps.add(newStep);
		this.cardPanel.add(newStep.getView(), newStep.getI18NKey());
		newStep.addChangeListener(stepChangeListener);
	}

	private void removeStep(WizardStep step) {
		step.removeChangeListener(stepChangeListener);
		this.steps.remove(step);
		this.cardPanel.remove(step.getView());
	}

	@Override
	public void previousStep() {
		SwingTools.invokeLater(() -> {
			previousButton.setIcon(LOADING_ICON);
			disableButtons();
		});
		new Thread(() -> {
			try {
				getCurrentStep().viewWillBecomeInvisible(WizardDirection.PREVIOUS);

				// remove step ID from list and show step
				String previousStepID = previousStepIDs.remove(previousStepIDs.size() - 1);
				showStep(previousStepID, WizardDirection.PREVIOUS);
			} catch (InvalidConfigurationException e) {
				SwingTools.invokeLater(this::updateButtons);
			}
		}).start();
	}

	@Override
	public void nextStep(final String stepId) {
		SwingTools.invokeLater(() -> {
			nextButton.setIcon(LOADING_ICON);
			disableButtons();
		});
		new Thread(() -> {
			try {
				getCurrentStep().viewWillBecomeInvisible(WizardDirection.NEXT);
				showStep(stepId, WizardDirection.NEXT);
			} catch (InvalidConfigurationException e) {
				SwingTools.invokeLater(this::updateButtons);
			}
		}).start();
	}

	@Override
	public void nextStep() {
		SwingTools.invokeLater(() -> {
			nextButton.setIcon(LOADING_ICON);
			disableButtons();
		});
		new Thread(() -> {
			try {
				/*
				 * Implementation almost equal to nextStep(String) but we need to call
				 * viewWillBecomeInvisible() before calling getNextStepID() so we cannot call
				 * nextStep(String) here.
				 */
				getCurrentStep().viewWillBecomeInvisible(WizardDirection.NEXT);

				String nextStepID = getCurrentStep().getNextStepID();
				showStep(nextStepID, WizardDirection.NEXT);
			} catch (InvalidConfigurationException e) {
				SwingTools.invokeLater(this::updateButtons);
			}
		}).start();
	}

	@Override
	public <D extends DataSource> void setDataSource(final D dataSource, final DataSourceFactory<D> factory) {

		// close data source if data source was already specified
		closeDataSource();

		// update the data source
		setDataSource(dataSource);

		// log data source selection
		DataImportWizardUtils.logStats(DataWizardEventType.DATASOURCE_SELECTED, factory.getI18NKey());

		// add data source custom steps right after the current steps but before the concluding
		// steps
		SwingTools.invokeAndWait(() -> {
			List<WizardStep> customSteps = factory.createCustomSteps(DataImportWizard.this, dataSource);
			for (WizardStep step : customSteps) {
				addStep(step);
			}
		});

	}

	private void closeDataSource() {
		if (getDataSource() != null) {
			try {
				getDataSource().close();
			} catch (DataSetException e) {
				// ignore, can't do anything here anyway
			}
		}
	}

	@Override
	public <D> D getDataSource(Class<? extends D> dsClass) throws InvalidConfigurationException {
		DataSource ds = getDataSource();
		if (ds == null) {
			return null;
		} else if (dsClass.isAssignableFrom(ds.getClass())) {
			return dsClass.cast(ds);
		} else {
			throw new InvalidConfigurationException();
		}
	}

	private DataSource getDataSource() {
		return dataSource;
	}

	private void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void setProgress(int progress) {
		this.progress = Math.min(Math.max(progress, 0), 100);
	}

	@Override
	public JDialog getDialog() {
		return this;
	}

}
