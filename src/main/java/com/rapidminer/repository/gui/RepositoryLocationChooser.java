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
package com.rapidminer.repository.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.RepositoryEntryTextField;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ParameterService;


/**
 * A dialog that shows the repository tree. The static method {@link #selectLocation(RepositoryLocation, Component)})} shows a
 * dialog and returns the location selected by the user.
 *
 * @author Simon Fischer, Tobias Malbrecht
 *
 */
public class RepositoryLocationChooser extends JPanel implements Observer<Boolean> {

	/** Removes the Connections folder */
	public static final Predicate<Entry> NO_CONNECTIONS = e -> !(e instanceof Folder && ((Folder) e).isSpecialConnectionsFolder());

	/** Only Connections and Repositories which support Connections */
	public static final Predicate<Entry> ONLY_CONNECTIONS = e ->
			(e instanceof Repository && ((Repository) e).supportsConnections())
					|| (e instanceof Folder && ((Folder) e).isSpecialConnectionsFolder())
					|| e instanceof ConnectionEntry;

	/** Shows only folder, can be combined with {@code NO_CONNECTIONS.and(ONLY_FOLDERS)}*/
	public static final Predicate<Entry> ONLY_FOLDERS = Folder.class::isInstance;

	/** Shows only processes, and hides the connection folder */
	public static final Predicate<Entry> ONLY_PROCESSES = e -> ((e instanceof Folder && !((Folder) e).isSpecialConnectionsFolder())
			|| e instanceof ProcessEntry);

	/** Only show example sets, hides the connection folder */
	public static final Predicate<Entry> ONLY_EXAMPLESETS = e -> {
		if (e instanceof Folder && !((Folder) e).isSpecialConnectionsFolder()) {
			return true;
		}
		if (!(e instanceof IOObjectEntry)) {
			return false;
		}
		Class<?> clazz = ((IOObjectEntry) e).getObjectClass();
		if (clazz == null) {
			return false;
		}
		return ExampleSet.class.isAssignableFrom(clazz) || IOTable.class.isAssignableFrom(clazz);
	};


	private static final long serialVersionUID = 1L;

	private final RepositoryTree tree;

	JLabel locationLabel;
	private final JTextField locationField = new JTextField(30);
	private final RepositoryEntryTextField locationFieldRepositoryEntry = new RepositoryEntryTextField();
	private JLabel selectionErrorTextLabel;
	private JLabel selectionErrorIconLabel;
	private Icon standardIcon;
	private Icon errorIcon;

	private boolean enforceValidRepositoryEntryName;
	private volatile boolean currentEntryValid;

	private final RepositoryLocation resolveRelativeTo;

	private JCheckBox resolveBox;

	private final List<ChangeListener> listeners = new LinkedList<>();

	private final JLabel resultLabel = new JLabel();

	/**
	 * The entry the user last clicked on. (Not the selected entry, this is also influenced by the
	 * text field.)
	 */
	private Entry currentEntry;

	private boolean folderSelected;

	private static class RepositoryLocationChooserDialog extends ButtonDialog {

		private static final long serialVersionUID = -726540444296013310L;

		private RepositoryLocationChooser chooser;

		private final JButton okButton;
		private final JButton cancelButton;

		public RepositoryLocationChooserDialog(Window owner, RepositoryLocation resolveRelativeTo, String initialValue,
				final boolean allowEntries, final boolean allowFolders, final boolean onlyWriteableRepositories) {
			this(owner, resolveRelativeTo, initialValue, allowEntries, allowFolders, onlyWriteableRepositories, null);
		}

		/**
		 * Create a Dialog to choose a {@link RepositoryLocation} with the following configuration settings.
		 *
		 * @param owner
		 * 		Parent dialog to be blocked by this modal dialog
		 * @param resolveRelativeTo
		 * 		if a relative path is requested, this is the base path to be relative to
		 * @param initialValue
		 * 		preselected value
		 * @param allowEntries
		 * 		if true entries are shown, else only folders will be shown
		 * @param allowFolders
		 * 		if true folders are shown, else no folders will be shown
		 * @param onlyWriteableRepositories
		 * 		if true show only those repositories that are writable by the current user
		 * @param entryPredicate
		 * 		if set it will filter the shown entries based on its logic
		 * @since 9.4
		 */
		public RepositoryLocationChooserDialog(Window owner, RepositoryLocation resolveRelativeTo, String initialValue,
											   final boolean allowEntries, final boolean allowFolders, final boolean onlyWriteableRepositories,
											   Predicate<Entry> entryPredicate) {
			super(owner, "repository_chooser", ModalityType.APPLICATION_MODAL);
			okButton = makeOkButton();
			chooser = new RepositoryLocationChooser(this, resolveRelativeTo, initialValue, allowEntries, allowFolders, false,
					onlyWriteableRepositories, Colors.WHITE, entryPredicate);
			chooser.tree.addRepositorySelectionListener(e -> {
				// called on double click
				Entry entry = e.getEntry();
				if (allowEntries && entry instanceof DataEntry) {
					wasConfirmed = true;
					dispose();
				}
			});
			chooser.addChangeListener(e -> okButton.setEnabled(chooser.hasSelection(allowFolders) && (allowFolders || !chooser.folderSelected)));
			okButton.setEnabled(chooser.hasSelection(allowFolders) && (allowFolders || !chooser.folderSelected));
			cancelButton = makeCancelButton();
			layoutDefault(chooser, NORMAL, okButton, cancelButton);
		}

		@Override
		protected void ok() {
			try {
				chooser.getRepositoryLocation();
				super.ok();
			} catch (MalformedRepositoryLocationException e) {
				SwingTools.showSimpleErrorMessage(this, "malformed_repository_location", e, e.getMessage());
			}
		}
	}

	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue) {
		this(owner, resolveRelativeTo, initialValue, true, false);
	}

	/** @since 8.2 */
	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue, Color backgroundColor) {
		this(owner, resolveRelativeTo, initialValue, true, false, false, false, backgroundColor);
	}

	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue,
			final boolean allowEntries, final boolean allowFolders) {
		this(owner, resolveRelativeTo, initialValue, allowEntries, allowFolders, false);
	}

	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue,
			boolean allowEntries, boolean allowFolders, boolean enforceValidRepositoryEntryName) {
		this(owner, resolveRelativeTo, initialValue, allowEntries, allowFolders, enforceValidRepositoryEntryName, false);
	}

	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue,
			final boolean allowEntries, final boolean allowFolders, boolean enforceValidRepositoryEntryName,
			final boolean onlyWriteableRepositories) {
		this(owner, resolveRelativeTo, initialValue, allowEntries, allowFolders, enforceValidRepositoryEntryName,
				onlyWriteableRepositories, null);
	}

	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue,
			final boolean allowEntries, final boolean allowFolders, boolean enforceValidRepositoryEntryName,
			final boolean onlyWritableRepositories, Color backgroundColor) {
		this(owner, resolveRelativeTo, initialValue, allowEntries, allowFolders, enforceValidRepositoryEntryName,
				onlyWritableRepositories, backgroundColor, null);
	}

	/**
	 * Show a dialog to select a {@link RepositoryLocation} to be used. Can be configured via the following parameters.
	 *
	 * @param owner
	 * 		parent for the modal dialog
	 * @param resolveRelativeTo
	 * 		if a relative path is requested, this is the base path to be relative to
	 * @param initialValue
	 * 		preselected value
	 * @param allowEntries
	 * 		if true entries are shown, else only folders will be shown
	 * @param allowFolders
	 * 		unused
	 * @param enforceValidRepositoryEntryName
	 * 		if true the result can only be a repository entry, not a repository or a folder
	 * @param onlyWritableRepositories
	 * 		if true show only those repositories that are writable by the current user
	 * @param backgroundColor
	 * 		the {@link Color} for the background of the shown tree
	 * @param predicate
	 * 		if set it will filter the shown entries based on its logic
	 * @since 9.4
	 */
	public RepositoryLocationChooser(Dialog owner, RepositoryLocation resolveRelativeTo, String initialValue,
									 final boolean allowEntries, final boolean allowFolders, boolean enforceValidRepositoryEntryName,
									 final boolean onlyWritableRepositories, Color backgroundColor, Predicate<Entry> predicate) {
		if (initialValue != null) {
			try {
				RepositoryLocation repositoryLocation;
				if (resolveRelativeTo != null) {
					repositoryLocation = new RepositoryLocation(resolveRelativeTo, initialValue);
				} else {
					repositoryLocation = new RepositoryLocation(initialValue);
				}
				locationField.setText(repositoryLocation.getName());
				locationFieldRepositoryEntry.setText(repositoryLocation.getName());
				resultLabel.setText(repositoryLocation.toString());
			} catch (Exception e) {
			}
		}
		this.resolveRelativeTo = resolveRelativeTo;
		this.enforceValidRepositoryEntryName = enforceValidRepositoryEntryName;
		tree = new RepositoryTree(owner, !allowEntries, onlyWritableRepositories, false, backgroundColor, predicate);

		if (initialValue != null) {
			// called twice to fix bug that it only selects parent on first time
			tree.expandIfExists(resolveRelativeTo, initialValue);
			if (tree.expandIfExists(resolveRelativeTo, initialValue)) {
				locationField.setText("");
				locationFieldRepositoryEntry.setText("");
			}
		} else {
			// no initial value, select the first local repository if one exists
			List<Repository> repositories = RepositoryManager.getInstance(null).getRepositories();
			for (Repository r : repositories) {
				if (!r.isReadOnly() && r instanceof LocalRepository) {
					// called twice to fix bug that it only selects parent on first time
					tree.expandIfExists(null, r.getLocation().getAbsoluteLocation());
					if (tree.expandIfExists(null, r.getLocation().getAbsoluteLocation())) {
						locationField.setText("");
						locationFieldRepositoryEntry.setText("");
						break;
					}
				}
			}
		}
		tree.getSelectionModel().addTreeSelectionListener(e -> {
			if (e.getPath() != null && e.getPath().getLastPathComponent() instanceof Entry) {
				currentEntry = (Entry) e.getPath().getLastPathComponent();
				if (!(currentEntry instanceof Folder) && allowEntries) {
					locationField.setText(currentEntry.getLocation().getName());
					locationFieldRepositoryEntry.setText(currentEntry.getLocation().getName());
				} else if (!onlyWritableRepositories && currentEntry instanceof Folder) {
					// we are assuming we are in READ mode, so selecting a folder will clear the name to prevent accidental, wrong selections
					locationField.setText("");
					locationFieldRepositoryEntry.setText("");
				}
				updateResult();
			}
		});
		KeyListener keyListener = new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				update();
			}

			private void update() {
				updateResult();
			}

		};
		// this is a key listener because document listener will also trigger on internally triggered updates
		locationField.addKeyListener(keyListener);
		locationFieldRepositoryEntry.addObserver(this, true);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 0, 0);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridwidth = GridBagConstraints.REMAINDER;

		JScrollPane treePane = new ExtendedJScrollPane(tree);
		treePane.setBorder(ButtonDialog.createBorder());
		add(treePane, c);

		standardIcon = null;
		errorIcon = SwingTools.createIcon(
				"16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.repository_location.location_invalid.icon"));
		selectionErrorIconLabel = new JLabel();
		selectionErrorIconLabel.setMinimumSize(new Dimension(16, 16));
		selectionErrorIconLabel.setPreferredSize(new Dimension(16, 16));
		selectionErrorTextLabel = new JLabel();
		JPanel selectionErrorPanel = new JPanel();
		selectionErrorPanel.setLayout(new FlowLayout());
		selectionErrorPanel.add(selectionErrorIconLabel);
		selectionErrorPanel.add(selectionErrorTextLabel);
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(0, 0, 0, 0);
		c.fill = GridBagConstraints.NONE;
		add(selectionErrorPanel, c);

		c.insets = new Insets(ButtonDialog.GAP, 0, 0, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0;
		locationLabel = new ResourceLabel("repository_chooser.entry_name");
		locationLabel.setLabelFor(locationField);
		add(locationLabel, c);

		c.weightx = 1;
		c.insets = new Insets(0, 0, 0, 0);
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(locationField, c);
		add(locationFieldRepositoryEntry, c);
		if (enforceValidRepositoryEntryName) {
			locationLabel.setVisible(false);
			locationField.setVisible(false);
		} else {
			locationFieldRepositoryEntry.setVisible(false);
		}

		c.gridwidth = GridBagConstraints.RELATIVE;
		c.weightx = 0;
		c.insets = new Insets(ButtonDialog.GAP, 0, 0, ButtonDialog.GAP);
		add(new ResourceLabel("repository_chooser.location"), c);
		c.weightx = 1;
		c.insets = new Insets(ButtonDialog.GAP, 0, 0, 0);
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(resultLabel, c);

		if (resolveRelativeTo != null) {
			resolveBox = new JCheckBox(
					new ResourceActionAdapter("repository_chooser.resolve", resolveRelativeTo.getAbsoluteLocation()));
			resolveBox.setSelected("true".equals(ParameterService
					.getParameterValue(RapidMinerGUI.PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS)));
			add(resolveBox, c);
			resolveBox.addActionListener(e -> updateResult());
		}
		if (initialValue != null && enforceValidRepositoryEntryName) {
			// check if initial value is valid
			locationFieldRepositoryEntry.triggerCheck();
		}
		updateResult();
	}

	/**
	 * Sets the name of the repository entry for the file chooser.
	 *
	 * @param name
	 *            the new name of the repository entry
	 */
	public void setRepositoryEntryName(final String name) {
		locationFieldRepositoryEntry.setText(name);
		locationField.setText(name);
		updateSelection();
		updateResult();
	}

	public String getRepositoryLocation() throws MalformedRepositoryLocationException {
		final TreePath path = tree.getSelectionPath();
		if (path == null || !(path.getLastPathComponent() instanceof Entry)) {
			return getLocationNameFieldText();
		}
		Entry selectedEntry = (Entry) path.getLastPathComponent();
		RepositoryLocation selectedLocation = selectedEntry.getLocation();
		if (selectedEntry instanceof Folder) {
			selectedLocation = new RepositoryLocation(selectedLocation, getLocationNameFieldText());
		} else if (selectedLocation.parent() != null) {
			selectedLocation = new RepositoryLocation(selectedLocation.parent(), getLocationNameFieldText());
		}
		if (resolveRelativeTo != null && resolveBox.isSelected()) {
			return selectedLocation.makeRelative(resolveRelativeTo);
		} else {
			return selectedLocation.getAbsoluteLocation();
		}
	}

	public boolean isEntryValid() {
		return hasSelection() && !enforceValidRepositoryEntryName || currentEntryValid;
	}

	/** Same as {@link #hasSelection(boolean)} with parameter false. */
	public boolean hasSelection() {
		return hasSelection(false);
	}

	/** Returns true if the user entered a valid, non-empty repository location. */
	public boolean hasSelection(boolean allowFolders) {
		if (!allowFolders
				&& (enforceValidRepositoryEntryName &&
				    !RepositoryLocation.isNameValid(locationFieldRepositoryEntry.getText())
						|| !enforceValidRepositoryEntryName && locationField.getText().isEmpty()
						|| tree.getSelectedEntry() == null)) {
			return false;
		} else {
			try {
				getRepositoryLocation();
				return true;
			} catch (MalformedRepositoryLocationException e) {
				// LogService.getRoot().warning("Malformed repository location: " + e);
				LogService.getRoot().log(Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.gui.RepositoryLocationChooser.malformed_repository_location", e),
						e);
				return false;
			}
		}
	}

	public boolean resolveRelative() {
		return resolveBox.isSelected();
	}

	public void setResolveRelative(boolean resolveRelative) {
		if (resolveBox != null && resolveRelative != resolveBox.isSelected()) {
			resolveBox.doClick();
		}
	}

	public void addChangeListener(ChangeListener l) {
		listeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}

	/**
	 * This will open a window to select a repository entry that is an entry or returns null if the
	 * user aborts the operation. Enforces a valid repository entry name.
	 */
	public static String selectEntry(RepositoryLocation resolveRelativeTo, Component c,
			boolean enforceValidRepositoryEntryName) {
		return selectLocation(resolveRelativeTo, null, c, true, false, false, enforceValidRepositoryEntryName);
	}

	/**
	 * This will open a window to select a repository entry that is an entry or returns null if the
	 * user aborts the operation.
	 */
	public static String selectEntry(RepositoryLocation resolveRelativeTo, Component c) {
		return selectLocation(resolveRelativeTo, null, c, true, false, false, true);
	}

	/**
	 * This will open a window to select a repository entry that is a folder or null if the user
	 * chooses to abort.
	 */
	public static String selectFolder(RepositoryLocation resolveRelativeTo, Component c) {
		return selectLocation(resolveRelativeTo, null, c, false, true);
	}

	public static String selectLocation(RepositoryLocation resolveRelativeTo, Component c) {
		return selectLocation(resolveRelativeTo, null, c, true, true);
	}

	public static String selectLocation(RepositoryLocation resolveRelativeTo, String initialValue, Component c,
			final boolean selectEntries, final boolean selectFolder) {
		return selectLocation(resolveRelativeTo, initialValue, c, selectEntries, selectFolder, false);
	}

	public static String selectLocation(RepositoryLocation resolveRelativeTo, String initialValue, Component c,
			final boolean selectEntries, final boolean selectFolder, final boolean forceDisableRelativeResolve) {
		return selectLocation(resolveRelativeTo, initialValue, c, selectEntries, selectFolder, forceDisableRelativeResolve,
				false);
	}

	public static String selectLocation(RepositoryLocation resolveRelativeTo, String initialValue, Component c,
			final boolean selectEntries, final boolean selectFolder, final boolean forceDisableRelativeResolve,
			final boolean enforceValidRepositoryEntryName) {
		return selectLocation(resolveRelativeTo, initialValue, c, selectEntries, selectFolder, forceDisableRelativeResolve,
				enforceValidRepositoryEntryName, false);
	}

	public static String selectLocation(RepositoryLocation resolveRelativeTo, String initialValue, Component c,
			final boolean selectEntries, final boolean selectFolder, final boolean forceDisableRelativeResolve,
			final boolean enforceValidRepositoryEntryName, final boolean onlyWriteableRepositories) {
		return selectLocation(resolveRelativeTo, initialValue, c, selectEntries, selectFolder, forceDisableRelativeResolve,
				enforceValidRepositoryEntryName, onlyWriteableRepositories, null);
	}

	/**
	 * Show a dialog to select a {@link RepositoryLocation} to be used. Can be configured via the following parameters.
	 *
	 * @param resolveRelativeTo
	 * 		if a relative path is requested, this is the base path to be relative to
	 * @param initialValue
	 * 		preselected value
	 * @param c
	 * 		base component to find the ancestor window
	 * @param selectEntries
	 * 		if true entries are shown, else only folders will be shown
	 * @param selectFolder
	 * 		if true folders are shown, else no folders will be shown
	 * @param forceDisableRelativeResolve
	 * 		if true relative resolving cannot be activated by the user and returned {@link RepositoryLocation} are absolute
	 * @param enforceValidRepositoryEntryName
	 * 		if true the result can only be a repository entry, not a repository or a folder
	 * @param onlyWriteableRepositories
	 * 		if true show only those repositories that are writable by the current user
	 * @param entryPredicate
	 * 		if set it will filter the shown entries based on its logic
	 * @return the chosen path
	 * @since 9.4
	 */
	public static String selectLocation(RepositoryLocation resolveRelativeTo, String initialValue, Component c,
										final boolean selectEntries, final boolean selectFolder, final boolean forceDisableRelativeResolve,
										final boolean enforceValidRepositoryEntryName, final boolean onlyWriteableRepositories,
										Predicate<Entry> entryPredicate) {
		Window owner = c != null ? SwingUtilities.getWindowAncestor(c) : null;
		AtomicReference<RepositoryLocationChooserDialog> dialogReference = new AtomicReference<>();
		SwingTools.invokeAndWait(() -> {
			dialogReference.set(new RepositoryLocationChooserDialog(
					owner, resolveRelativeTo, initialValue, selectEntries,
					selectFolder, onlyWriteableRepositories, entryPredicate));
			RepositoryLocationChooserDialog dialog = dialogReference.get();
			if (forceDisableRelativeResolve) {
				dialog.chooser.setResolveRelative(false);
				if (dialog.chooser.resolveBox != null) {
					dialog.chooser.resolveBox.setVisible(false);
				}
			}
			dialog.chooser.setEnforceValidRepositoryEntryName(enforceValidRepositoryEntryName);
			dialog.chooser.requestFocusInWindow();
			dialog.setVisible(true);
		});

		RepositoryLocationChooserDialog dialog = dialogReference.get();
		// if user has confirmed dialog with double-click or OK button
		if (dialog.wasConfirmed()) {
			if (resolveRelativeTo != null && !forceDisableRelativeResolve) {
				ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_RESOLVE_RELATIVE_REPOSITORY_LOCATIONS,
						Boolean.toString(dialog.chooser.resolveRelative()));
				ParameterService.saveParameters();
			}
			try {
				String text = dialog.chooser.getRepositoryLocation();
				if (text.length() > 0) {
					return text;
				}
			} catch (MalformedRepositoryLocationException e) {
				// this should not happen since the dialog would not have disposed without an error
				// message.
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	/**
	 * Updates the path selected via the tree.
	 */
	private void updateSelection() {
		TreePath selectionPath = tree.getSelectionPath();
		if (selectionPath != null && selectionPath.getLastPathComponent() instanceof Entry) {
			Entry selectedEntry = (Entry) selectionPath.getLastPathComponent();
			if (!(selectedEntry instanceof Folder)) {
				tree.setSelectionPath(selectionPath.getParentPath());
			}
		}
	}

	private void updateResult() {
		try {
			String repositoryLocation = getRepositoryLocation();
			resultLabel.setText(repositoryLocation);
			// check if a repository folder is selected, if not, show warning
			if (tree.getSelectedEntry() == null) {
				selectionErrorTextLabel.setText(I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.repository_location.location_invalid_no_selection.label"));
				selectionErrorIconLabel.setIcon(errorIcon);
			} else {
				selectionErrorTextLabel.setText("");
				selectionErrorIconLabel.setIcon(standardIcon);
			}
		} catch (MalformedRepositoryLocationException e) {
			// LogService.getRoot().log(Level.WARNING, "Malformed location: " + e, e);
			LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
					"com.rapidminer.repository.gui.RepositoryLocationChooser.malformed_location", e), e);
		}
		this.folderSelected = currentEntry instanceof Folder && getLocationNameFieldText().isEmpty();
		for (ChangeListener l : listeners) {
			l.stateChanged(new ChangeEvent(this));
		}
	}

	public boolean isEnforceValidRepositoryEntryName() {
		return enforceValidRepositoryEntryName;
	}

	public void setEnforceValidRepositoryEntryName(final boolean enforceValidRepositoryEntryName) {
		SwingTools.invokeLater(() -> {
			this.enforceValidRepositoryEntryName = enforceValidRepositoryEntryName;
			this.locationLabel.setVisible(!enforceValidRepositoryEntryName);
			this.locationField.setVisible(!enforceValidRepositoryEntryName);
			this.locationFieldRepositoryEntry.setVisible(enforceValidRepositoryEntryName);
		});
	}

	@Override
	public void update(Observable<Boolean> observable, Boolean arg) {
		this.currentEntryValid = arg;
		updateResult();
	}

	@Override
	public boolean requestFocusInWindow() {
		// this bit allows for easy name entering when the dialog is used for saving
		// instantly allows typing text and pressing Enter afterwards
		if (locationFieldRepositoryEntry.isVisible()) {
			return locationFieldRepositoryEntry.requestFocusInWindow();
		} else {
			return tree.requestFocusInWindow();
		}
	}

	/**
	 * @return the text value of the visible location field
	 */
	private String getLocationNameFieldText() {
		return enforceValidRepositoryEntryName ? locationFieldRepositoryEntry.getText() : locationField.getText();
	}
}
