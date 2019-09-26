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
package com.rapidminer.studio.io.gui.internal.steps;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;

import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.core.io.gui.ImportWizard;
import com.rapidminer.core.io.gui.InvalidConfigurationException;
import com.rapidminer.core.io.gui.WizardDirection;
import com.rapidminer.core.io.gui.WizardStep;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.SwingTools.ResultRunnable;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;
import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.tools.I18N;


/**
 * Abstract {@link WizardStep} that allows to select a repository location as destination of the
 * import. Triggers the actual import on success.
 *
 * @author Michael Knopf, Gisa Schaefer, Marcel Michel
 * @since 7.0.0
 *
 */
public abstract class AbstractToRepositoryStep<T extends RepositoryLocationChooser> extends AbstractWizardStep {

	/** Template for the text below the {@link #animationLabel} */
	private static final String IMPORTING_TEXT_TEMPLATE = "<html><center><span style=\"font-size: 14\">"
			+ I18N.getGUILabel("io.dataimport.step.store_data_to_repository.label") + "<br>%s</span></center></html>";

	/**
	 * SwingWorker which periodically checks if the background job finished.
	 */
	private class ProgressUpdater extends MultiSwingWorker<Void, Void> {

		private static final int IDLE_TIME_MS = 500;

		@Override
		protected Void doInBackground() {
			while (backgroundJob != null || confirmDialog != null) {
				try {
					// we've finished the background job, close the obsolete
					// confirm dialog
					if (backgroundJob == null && confirmDialog != null) {
						confirmDialog.dispose();
					}
					Thread.sleep(IDLE_TIME_MS);
				} catch (InterruptedException e) {
					// ignore this
				}
			}
			return null;
		}
	}

	/** IDs for the {@link CardLayout} */
	private static final String CARD_ID_CHOOSER = "chooser";
	private static final String CARD_ID_PROGRESS = "progress";

	/** Change Listener which is registered to the {@link #chooser} */
	private final ChangeListener changeListener = e -> AbstractToRepositoryStep.this.fireStateChanged();

	/** {@link ResultRunnable} for the {@link #confirmDialog} */
	private final ResultRunnable<Integer> confirmResultRunnable = new ResultRunnable<Integer>() {

		@Override
		public Integer run() {
			confirmDialog = new ConfirmDialog(wizard.getDialog(), "cancel_import", ConfirmDialog.YES_NO_OPTION, false);
			confirmDialog.setVisible(true);
			return confirmDialog.getReturnOption();
		}
	};

	/** In case of a dialog, this listener will be used to control the close event */
	private final WindowAdapter closeListener = new WindowAdapter() {

		@Override
		public void windowClosing(WindowEvent event) {
			if (backgroundJob != null) {
				closeDialog = SwingTools.invokeAndWaitWithResult(confirmResultRunnable) == ConfirmDialog.YES_OPTION;
				confirmDialog = null;
				if (closeDialog && backgroundJob != null) {
					stopButton.doClick();
				}
			}
		}

	};

	/** default close operation of the import wizard dialog, may be -1 */
	private final int defaultCloseOperation;

	/** The chooser for the repository location. */
	private T chooser = null;

	/** The main panel contains two cards */
	private JPanel mainPanel;

	/** The active confirmation dialog */
	private ConfirmDialog confirmDialog;

	/** The import job as {@link ProgressThread} */
	private ProgressThread backgroundJob;

	/** Flag which is set to {@code true} if the {@link #backgroundJob} is cancelled */
	private boolean isImportCancelled;

	/** the label showing the data storing animation and the store location */
	private JLabel animationLabel;

	/** the button to stop the data storing */
	private JButton stopButton;

	/** Flag which is set to {@code true} if the user wants to close the dialog */
	private boolean closeDialog;

	/** Flag used to set the default file name once */
	private boolean defaultFileNameInitialized;

	/** the import wizard which holds this step */
	protected final ImportWizard wizard;

	public AbstractToRepositoryStep(final ImportWizard wizard) {
		this.wizard = wizard;
		JDialog dialog = wizard.getDialog();
		if (dialog != null) {
			defaultCloseOperation = dialog.getDefaultCloseOperation();
		} else {
			defaultCloseOperation = -1;
		}
	}

	@Override
	public synchronized JComponent getView() {
		if (mainPanel == null) {
			// If the user has selected a location in the repository browser, use it as initial
			// location for the chooser.
			String initialLocation = null;
			RepositoryTree tree = RapidMinerGUI.getMainFrame().getRepositoryBrowser().getRepositoryTree();
			Entry entry = tree.getSelectedEntry();
			if (entry == null) {
				// nothing selected
			} else if (entry instanceof Folder && ((Folder) entry).isSpecialConnectionsFolder()) {
				// select repository if it's a connection
				initialLocation = entry.getContainingFolder().getLocation().getAbsoluteLocation();
			} else if (entry.getContainingFolder() != null && entry.getContainingFolder().isSpecialConnectionsFolder()) {
				// select repository if it's a connection
				initialLocation = entry.getContainingFolder().getContainingFolder().getLocation().getAbsoluteLocation();
			} else if (!entry.isReadOnly()) {
				initialLocation = entry.getLocation().getAbsoluteLocation();
			}
			// The validity of the step goes hand in hand with the state of the repository location
			// chooser. Wrap respective events.
			chooser = initializeChooser(initialLocation);
			chooser.addChangeListener(changeListener);
			mainPanel = new JPanel(new CardLayout());
			mainPanel.add(getContentPanel(), CARD_ID_CHOOSER);
			mainPanel.add(createProgressPanel(), CARD_ID_PROGRESS);
		}
		return mainPanel;
	}

	@Override
	public void validate() throws InvalidConfigurationException {
		// Check the location (the descriptor itself) is valid.
		if (!chooser.isEntryValid()) {
			throw new InvalidConfigurationException();
		}
	}

	@Override
	public void viewWillBecomeVisible(WizardDirection direction) throws InvalidConfigurationException {
		wizard.setProgress(100);

		if (!defaultFileNameInitialized) {
			defaultFileNameInitialized = true;
			// try to get a file location
			Path filePath = null;
			try {
				// if there is a location it comes from a FileDataSource
				filePath = wizard.getDataSource(FileDataSource.class).getLocation();
			} catch (InvalidConfigurationException e) {
				// is not a data source with a location
			}

			if (filePath != null) {
				// extract file name without extension and set it
				String fileName = filePath.getFileName().toString();
				int separatorLocation = fileName.lastIndexOf('.');
				if (separatorLocation > -1) {
					fileName = fileName.substring(0, separatorLocation);
				}
				chooser.setRepositoryEntryName(fileName);
			}
		}
	}

	@Override
	public void viewWillBecomeInvisible(WizardDirection direction) throws InvalidConfigurationException {
		if (direction != WizardDirection.NEXT) {
			// This step is the only and last of the custom steps. If the user wants to proceed,
			// trigger the actual import. If they don't ignore the event.
			return;
		}

		try {
			final RepositoryLocation entryLocation = new RepositoryLocation(chooser.getRepositoryLocation());
			final RepositoryLocation folderLocation = entryLocation.parent();
			final Entry entry = folderLocation.locateEntry();
			if (!(entry instanceof Folder)) {
				fireStateChanged();
				throw new InvalidConfigurationException();
			}

			final Folder parent = (Folder) entry;
			final Entry oldEntry = entryLocation.locateEntry();

			if (parent.isSpecialConnectionsFolder()) {
				fireStateChanged();
				throw new InvalidConfigurationException();
			}

			// Ask user whether to override existing file (if any).
			if (oldEntry != null) {
				if (SwingTools.showConfirmDialog(wizard.getDialog(), "overwrite", ConfirmDialog.YES_NO_OPTION,
						oldEntry.getName()) == ConfirmDialog.NO_OPTION) {
					fireStateChanged();
					throw new InvalidConfigurationException();
				}

				boolean retryDelete = true;
				// do not update the wizard during the delete call,
				// otherwise the buttons will not be in the correct state
				chooser.removeChangeListener(changeListener);
				while (retryDelete) {
					try {
						oldEntry.delete();
						retryDelete = false;
					} catch (RepositoryException e) {
						if (SwingTools.showConfirmDialog(wizard.getDialog(), "error_in_delete_entry_with_cause",
								ConfirmDialog.YES_NO_OPTION, oldEntry.getName(), e.getMessage()) == ConfirmDialog.NO_OPTION) {
							fireStateChanged();
							chooser.addChangeListener(changeListener);
							throw new InvalidConfigurationException();
						}
					}
				}
				chooser.addChangeListener(changeListener);
			}

			closeDialog = false;
			isImportCancelled = false;
			backgroundJob = getImportThread(entryLocation, parent);
			SwingTools.invokeLater(() -> {
				askBeforeClosing(true);
				animationLabel.setText(String.format(IMPORTING_TEXT_TEMPLATE, entryLocation));
				stopButton.setEnabled(true);
				showCard(CARD_ID_PROGRESS);
				// this call ensures that the progress bar runs smoothly
				ProgressThreadDialog.getInstance().setVisible(true, false);
			});

			// Import data with background worker.
			backgroundJob.addProgressThreadListener(thread -> backgroundJob = null);
			MultiSwingWorker<Void, Void> progressUpdater = new ProgressUpdater();
			progressUpdater.start();
			backgroundJob.start();
			try {
				// this call will block the EDT and will continue as soon as
				// the backgroundJob is completed
				progressUpdater.get();
			} catch (InterruptedException | ExecutionException e) {
				// do nothing
			}

			askBeforeClosing(false);
			if (closeDialog) {
				return;
			}
			if (!isImportSuccess() || isImportCancelled) {
				showCard(CARD_ID_CHOOSER);
				throw new InvalidConfigurationException();
			}
		} catch (MalformedRepositoryLocationException | RepositoryException e) {
			// This should already be covered in #validate().
			throw new InvalidConfigurationException();
		}
	}

	/**
	 * If the {@link #wizard} is instance of an {@link JDialog} the user will be asked before
	 * quitting the dialog.
	 *
	 * @param askBeforeClosing
	 *            if {@code true} a confirmation dialog will be shown, otherwise the default
	 *            behavior of the dialog will be used.
	 */
	private void askBeforeClosing(final boolean askBeforeClosing) {
		SwingTools.invokeLater(() -> {
			JDialog dialog = wizard.getDialog();
			if (dialog != null) {
				if (askBeforeClosing) {
					dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					dialog.addWindowListener(closeListener);
				} else {
					dialog.setDefaultCloseOperation(defaultCloseOperation);
					dialog.removeWindowListener(closeListener);
				}
			}
		});

	}

	@Override
	public String getNextStepID() {
		return null; // last step
	}

	/**
	 * Creates a file chooser that determines where to store data.
	 *
	 * @param initialDestination
	 *            the initial location the file chooser should show
	 * @return a file chooser
	 */
	protected abstract T initializeChooser(String initialDestination);

	/**
	 * @return a panel that contains the chooser
	 */
	protected abstract JPanel getContentPanel();

	/**
	 * @return the chooser initialized by {@link #initializeChooser(String)}.
	 */
	protected T getChooser() {
		return chooser;
	}

	/**
	 * Indicator if the created {@link ProgressThread} which was created via
	 * {@link #getImportThread(RepositoryLocation, Folder)} has successfully completed the import.
	 *
	 * @return {@code true} if the import successfully completed, otherwise {@code false}
	 */
	protected abstract boolean isImportSuccess();

	/**
	 * Creates a progress thread that stores the data at the entryLocation.
	 *
	 * @param entryLocation
	 *            the location where the data should be stored
	 * @param parent
	 *            the parent folder of the location
	 * @return a progress thread that imports the data to the location
	 * @throws InvalidConfigurationException
	 *             in case the current step is configured wrong
	 */
	protected abstract ProgressThread getImportThread(final RepositoryLocation entryLocation, final Folder parent)
			throws InvalidConfigurationException;

	/**
	 * Shows the card specified by id, e.g. {@link #CARD_ID_CHOOSER} or {@link #CARD_ID_PROGRESS}.
	 *
	 * @param cardId
	 *            the id of the card
	 */
	private void showCard(final String cardId) {
		SwingTools.invokeLater(() -> ((CardLayout) mainPanel.getLayout()).show(mainPanel, cardId));
	}

	/**
	 * Creates a panel which displays the process animation and a stop button.
	 *
	 * @return the created panel
	 */
	private JPanel createProgressPanel() {
		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(0, 5, 5, 5);

		ImageIcon animation = SwingTools
				.createImage(I18N.getGUILabel("io.dataimport.step.store_data_to_repository.animation"));
		animationLabel = new JLabel(animation);
		animationLabel.setHorizontalTextPosition(JLabel.CENTER);
		animationLabel.setVerticalTextPosition(JLabel.BOTTOM);

		gbc.gridy += 1;
		panel.add(animationLabel, gbc);

		stopButton = new JButton(
				new ResourceAction(true, "io.dataimport.step.store_data_to_repository.stop_data_import_progress") {

					private static final long serialVersionUID = 1L;

					@Override
					public void loggedActionPerformed(ActionEvent e) {
						if (backgroundJob != null) {
							setEnabled(false);
							backgroundJob.cancel();
							isImportCancelled = true;
						}
					}
				});

		gbc.insets = new Insets(40, 5, 5, 5);
		gbc.gridy += 1;
		panel.add(stopButton, gbc);

		return panel;
	}
}
