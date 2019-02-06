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
package com.rapidminer.studio.io.data.internal.file;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;

import com.lowagie.text.Font;
import com.rapidminer.core.io.data.source.DataSourceFactory;
import com.rapidminer.core.io.data.source.DataSourceFactoryRegistry;
import com.rapidminer.core.io.data.source.FileDataSource;
import com.rapidminer.core.io.data.source.FileDataSourceFactory;
import com.rapidminer.gui.tools.ExtendedJFileChooser;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.studio.io.gui.internal.DataWizardEventType;


/**
 * A view that shows a {@link JFileChooser} to allow file selection for a {@link FileDataSource}s.
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
final class LocalFileLocationChooserView extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final int SELECTED_TYPE_PANEL_WIDTH = 700;
	private static final int SELECTED_TYPE_PANEL_HEIGHT = 41;
	private static final int SELECTED_TYPE_PANEL_INSETS_TOP = 10;
	private static final int SELECTED_TYPE_PANEL_INSETS_LEFT = 3;
	private static final int SELECTED_TYPE_PANEL_INSETS_BOTTOM = 10;
	private static final int SELECTED_TYPE_PANEL_INSETS_RIGHT = 0;

	private final ExtendedJFileChooser fileChooser;
	private final JPanel selectedTypePanel;
	private final JComboBox<FileDataSourceFactory<?>> factoryDropDownComboBox;
	private final List<ChangeListener> changeListeners = new LinkedList<>();

	private FileDataSourceFactory<?> fileDataSourceFactory;
	private boolean showFileDataSourceComboBox = false;

	private final Action changeTypeAction = new ResourceAction(false, "io.dataimport.step.file_selection.change_type") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			// toggle marker variable
			showFileDataSourceComboBox = true;
			updateFileTypePanel();
		}

	};

	private final Action selectTypeAction = new ResourceAction(false, "io.dataimport.step.file_selection.select_type") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			chooseDataSourceFileTypeFromComboBox();
		}

	};

	private final transient ChangeListener fileChangeListener = e -> {
		// Update detected file type each time the users changes the selected file
		final Path selectedLocation = getSelectedLocation();
		if (selectedLocation != null) {
			fileDataSourceFactory = LocalFileDataSourceFactory.lookupFactory(selectedLocation);
		}
		updateFileTypePanel();
	};

	/**
	 * The view constructor.
	 *
	 * @param fileFilters
	 *            the file filters for this file chooser
	 * @param factoryI18NKey
	 *            the i18n key of the factory, that should be the only usable one (optional)
	 */
	LocalFileLocationChooserView(List<FileFilter> fileFilters, String factoryI18NKey) {
		this.fileChooser = new ExtendedJFileChooser("", FileSystemView.getFileSystemView().getDefaultDirectory());
		this.fileChooser.setControlButtonsAreShown(false);
		if (factoryI18NKey == null || factoryI18NKey.trim().isEmpty()) {
			this.fileChooser.setAcceptAllFileFilterUsed(true);
		}

		if (fileFilters != null) {
			for (FileFilter filter : fileFilters) {
				this.fileChooser.addChoosableFileFilter(filter);
			}
		}

		// create combo box for all available factories
		this.factoryDropDownComboBox = new JComboBox<>();
		FileDataSourceFactory<?>[] comboBoxItems;

		if (factoryI18NKey != null && !factoryI18NKey.trim().isEmpty()) {
			this.fileChooser.setAcceptAllFileFilterUsed(true);

			List<FileDataSourceFactory<?>> fileFactories = DataSourceFactoryRegistry.INSTANCE.getFileFactories();
			comboBoxItems = new FileDataSourceFactory<?>[1];
			for (FileDataSourceFactory<?> factory : fileFactories) {
				if (factory.getI18NKey().equals(factoryI18NKey)) {
					comboBoxItems[0] = factory;
					break;
				}
			}
		} else {
			comboBoxItems = DataSourceFactoryRegistry.INSTANCE.getFileFactories().toArray(new FileDataSourceFactory[0]);
		}

		ComboBoxModel<FileDataSourceFactory<?>> comboBoxModel = new DefaultComboBoxModel<>(comboBoxItems);
		factoryDropDownComboBox.setModel(comboBoxModel);
		factoryDropDownComboBox.setRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(DataImportWizardUtils.getFactoryLabel((DataSourceFactory<?>) value));
				return comp;
			}

		});

		setLayout(new BorderLayout());

		// add file chooser to panel
		add(fileChooser, BorderLayout.CENTER);

		this.selectedTypePanel = new JPanel(new GridBagLayout());
		add(selectedTypePanel, BorderLayout.SOUTH);

		if (factoryI18NKey == null || factoryI18NKey.trim().isEmpty()) {
			registerChangeListener(fileChangeListener);
			fileChangeListener.stateChanged(null);
		}
	}

	/**
	 * Applies the file type selection made by the user in the file type combobox.
	 */
	protected void chooseDataSourceFileTypeFromComboBox() {

		// toggle marker variable
		this.showFileDataSourceComboBox = false;

		String oldFileType = fileDataSourceFactory != null ? fileDataSourceFactory.getI18NKey() : "unknown";

		// update selected factory
		this.fileDataSourceFactory = (FileDataSourceFactory<?>) factoryDropDownComboBox.getSelectedItem();

		// log file type change
		String newFileType = fileDataSourceFactory.getI18NKey();
		DataImportWizardUtils.logStats(DataWizardEventType.FILE_TYPE_CHANGED, oldFileType + "->" + newFileType);

		fireChangeEvent();
		updateFileTypePanel();
	}

	/**
	 * @return the selected file location. Might be {@code null} in case no file has been selected.
	 */
	Path getSelectedLocation() {
		File selectedFile = fileChooser.getSelectedFile();
		return selectedFile != null ? selectedFile.toPath() : null;
	}

	/**
	 * @return the data source factory for the selected file. Might be {@code null} in case no file
	 *         has been selected.
	 */
	FileDataSourceFactory<?> getFileDataSourceFactory() {
		return fileDataSourceFactory;
	}

	/**
	 * Registers a new {@link ChangeListener} for the file chooser.
	 *
	 * @param listener
	 *            the {@link ChangeListener} to register
	 */
	void registerChangeListener(ChangeListener listener) {
		this.fileChooser.addChangeListener(listener);
		this.changeListeners.add(listener);
	}

	/**
	 * Fires a state changed event for registered {@link ChangeListener}. Skips the
	 * {@link #fileChangeListener} to avoid new file type lookup when choosing a different file
	 * import type.
	 */
	private void fireChangeEvent() {
		for (ChangeListener listener : changeListeners) {
			if (listener == fileChangeListener) {
				// skip local file change listener to avoid new file type lookup
				continue;
			}
			try {
				listener.stateChanged(new ChangeEvent(fileChooser));
			} catch (RuntimeException e) {
				// ignore
			}
		}
	}

	/**
	 * Sets the selected file and updates the {@link JFileChooser} accordingly.
	 *
	 * @param selectedFile
	 *            the selected file. Must not be <code>null</code>.
	 */
	void setSelectedFile(Path selectedFile) {
		this.fileChooser.setSelectedFile(selectedFile.toFile());
	}

	/**
	 * @param fileDataSourceFactory
	 *            updates the file data source factory for this view
	 */
	public void setFileDataSourceFactory(FileDataSourceFactory<?> fileDataSourceFactory) {
		this.fileDataSourceFactory = fileDataSourceFactory;
	}

	private void updateFileTypePanel() {
		SwingTools.invokeLater(() -> {
			selectedTypePanel.removeAll();

			GridBagConstraints constraint = new GridBagConstraints();
			constraint.fill = GridBagConstraints.NONE;
			constraint.anchor = GridBagConstraints.WEST;
			constraint.insets = new Insets(SELECTED_TYPE_PANEL_INSETS_TOP, SELECTED_TYPE_PANEL_INSETS_LEFT, SELECTED_TYPE_PANEL_INSETS_BOTTOM, SELECTED_TYPE_PANEL_INSETS_RIGHT);

			constraint.fill = GridBagConstraints.BOTH;
			constraint.weightx = 1.0;
			JPanel fillerPanel = new JPanel();
			selectedTypePanel.add(fillerPanel, constraint);

			// set constraints for other component
			constraint.fill = GridBagConstraints.NONE;
			constraint.weightx = 0.0;

			if (getSelectedLocation() != null) {

				boolean showComboBox = showFileDataSourceComboBox;

				// add first label
				if (fileDataSourceFactory != null) {
					if (showComboBox) {
						JLabel typeLabel = new ResourceLabel("io.dataimport.step.file_selection.select_type");
						selectedTypePanel.add(typeLabel, constraint);
					} else {
						JLabel typeLabel = new ResourceLabel("io.dataimport.step.file_selection.type_detected");
						selectedTypePanel.add(typeLabel, constraint);

						String selectedTypeName = DataImportWizardUtils.getFactoryLabel(fileDataSourceFactory);
						JLabel selectedTypeLabel = new JLabel(selectedTypeName);
						selectedTypeLabel.setFont(selectedTypeLabel.getFont().deriveFont(Font.BOLD));
						selectedTypePanel.add(selectedTypeLabel, constraint);
					}
				} else {
					JLabel unknownTypeLabel = new ResourceLabel("io.dataimport.step.file_selection.unknown_type");
					selectedTypePanel.add(unknownTypeLabel, constraint);
					showComboBox = true;
				}

				if (showComboBox) {
					selectedTypePanel.add(factoryDropDownComboBox, constraint);

					LinkLocalButton changeTypeButton = new LinkLocalButton(selectTypeAction);
					changeTypeButton.setAlignmentX(SwingConstants.CENTER);
					selectedTypePanel.add(changeTypeButton, constraint);
				} else {
					LinkLocalButton changeTypeButton = new LinkLocalButton(changeTypeAction);
					changeTypeButton.setAlignmentX(SwingConstants.CENTER);
					selectedTypePanel.add(changeTypeButton, constraint);
				}

			} else {
				// add a no file selected label
				selectedTypePanel.add(new ResourceLabel("io.dataimport.step.file_selection.no_file_selected"),
						constraint);
			}

			// ensure same height for every panel content to avoid flickering
			selectedTypePanel.setPreferredSize(new Dimension(SELECTED_TYPE_PANEL_WIDTH, SELECTED_TYPE_PANEL_HEIGHT));
			selectedTypePanel.setMinimumSize(new Dimension(SELECTED_TYPE_PANEL_WIDTH, SELECTED_TYPE_PANEL_HEIGHT));

			selectedTypePanel.revalidate();
			selectedTypePanel.repaint();
		});
	}

}
