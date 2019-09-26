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
package com.rapidminer.connection.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.actions.CancelEditingAction;
import com.rapidminer.connection.gui.actions.OpenEditConnectionAction;
import com.rapidminer.connection.gui.actions.SaveConnectionAction;
import com.rapidminer.connection.gui.components.TestConnectionPanel;
import com.rapidminer.connection.gui.dto.ConnectionInformationHolder;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;


/**
 * The actual dialog to view/edit connections in. To provide the necessary custom UI to edit your connection type, register it at {@link ConnectionGUIRegistry}.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class ConnectionEditDialog extends JDialog {

	private static final Dimension DEFAULT_SIZE = new Dimension(790, 500);

	private final transient ConnectionInformationHolder holder;
	private final transient ConnectionGUI gui;

	private final boolean editMode;
	private final TestConnectionPanel testConnectionPanel;

	private final boolean isTypeKnown;

	// the mainGUI will be the ConnectionGUI implementation
	private JComponent mainGUI;

	/**
	 * Opens the dialog in edit mode
	 *
	 * @param holder
	 * 		the connection holder
	 */
	public ConnectionEditDialog(ConnectionInformationHolder holder) {
		this(holder, true);
	}

	/**
	 * Opens the dialog in view or edit mode
	 *
	 * @param holder
	 * 		the connection holder
	 * @param openInEditMode
	 *        {@code true} for edit mode, {@code false} for view mode
	 */
	public ConnectionEditDialog(ConnectionInformationHolder holder, boolean openInEditMode) {
		this(ApplicationFrame.getApplicationFrame(), holder, openInEditMode);
	}

	/**
	 * Opens the dialog in view or edit mode
	 *
	 * @param owner
	 * 		the owner
	 * @param holder
	 * 		the connection holder
	 * @param openInEditMode
	 *        {@code true} for edit mode, {@code false} for view mode
	 */
	public ConnectionEditDialog(Window owner, ConnectionInformationHolder holder, boolean openInEditMode) {
		super(owner, getTitle(holder, openInEditMode), ModalityType.APPLICATION_MODAL);
		this.isTypeKnown = ConnectionHandlerRegistry.getInstance().isTypeKnown(holder.getConnectionType());
		this.editMode = openInEditMode && holder.isEditable() && isTypeKnown;
		this.holder = holder;
		this.gui = getEditGui(holder);
		this.setLayout(new GridBagLayout());
		setMinimumSize(DEFAULT_SIZE);
		setPreferredSize(DEFAULT_SIZE);


		GridBagConstraints gbc = new GridBagConstraints();
		//----------BIG PANEL------------
		// Set injected Fields
		// [Test connection]   [Create] [Cancel]
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(10, 10, 0, 10);
		mainGUI = createMainGUI();
		add(mainGUI, gbc);

		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(18, 10, 10, 74);
		testConnectionPanel = new TestConnectionPanel(holder.getConnectionType(), gui::getConnection, this::processTestResult, holder.getConnectionInformation().getStatistics());
		add(testConnectionPanel, gbc);

		gbc.insets = new Insets(0, 0, 10, 10);
		// Give save full with to make alignable to the east
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.SOUTHEAST;
		if (!openInEditMode) {
			JButton editButton = new JButton(new OpenEditConnectionAction(this, holder));
			editButton.setEnabled(holder.isEditable() && isTypeKnown);
			add(editButton, gbc);
		} else if (editMode) {
			add(new JButton(new SaveConnectionAction(this, gui, holder::getLocation, this::processTestResult, "save_connection")), gbc);
		}
		gbc.insets = new Insets(0, 0, 10, 10);
		CancelEditingAction cancelAction = new CancelEditingAction(this, editMode ? "cancel_connection_edit" : "close_connection_edit", () -> editMode && holder.hasChanged(gui.getConnection()));
		add(new JButton(cancelAction), gbc);

		// check changes on close
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancelAction.actionPerformed(null);
			}

		});
		// close dialog with ESC
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CLOSE");
		getRootPane().getActionMap().put("CLOSE", cancelAction);

		setLocationRelativeTo(getOwner());
		SwingUtilities.invokeLater(() -> {
			pack();
			revalidate();
			repaint();
		});
	}

	/**
	 * Show a specific tab, to be found via the title. The title is already present resolved from I18N so the result
	 * from {@link ConnectionI18N#getConnectionGUILabel(String, Object...)} is required here. Fails quietly.
	 *
	 * @param name
	 * 		value of the I18N key for connection GUI label
	 */
	public void showTab(String name) {
		if (name == null || !(mainGUI instanceof JTabbedPane)) {
			return;
		}

		JTabbedPane tabPane = (JTabbedPane) mainGUI;
		for (int i = 0; i < tabPane.getTabCount(); i++) {
			if (name.equals(tabPane.getTitleAt(i))) {
				tabPane.setSelectedIndex(i);
				break;
			}
		}
	}

	/**
	 * Show a specific tab, to be found via the index. Fails quietly.
	 *
	 * @param index
	 * 		the index of the tab to be selected
	 */
	public void showTab(int index) {
		JTabbedPane tabPane = (JTabbedPane) mainGUI;
		if (index < 0 || index >= tabPane.getTabCount()) {
			return;
		}
		tabPane.setSelectedIndex(index);
	}

	/**
	 * Get the name of the currently displayed tab or {@code null}
	 *
	 * @return the localized title of the currently displayed tab
	 */
	public String getCurrentTabTitle() {
		if (mainGUI instanceof JTabbedPane) {
			return ((JTabbedPane) mainGUI).getTitleAt(((JTabbedPane) mainGUI).getSelectedIndex());
		}
		return null;
	}

	/**
	 * @return {@code true} if the dialog is in edit mode and the connection is editable
	 */
	protected boolean isEditable() {
		return editMode && holder.isEditable() && isTypeKnown;
	}

	private void processTestResult(TestResult testResult) {
		testConnectionPanel.setTestResult(testResult);
		if (testResult != null && testResult.getType() != TestResult.ResultType.NONE) {
			gui.validationResult(testResult.getParameterErrorMessages());
		}
	}

	/**
	 * Creates the main GUI panel.
	 */
	private JComponent createMainGUI() {
		return gui.getConnectionEditComponent();
	}

	/**
	 * Gets the edit view for the given connection
	 *
	 * @param connectionHolder
	 * 		the connection
	 * @return the edit view
	 */
	private ConnectionGUI getEditGui(ConnectionInformationHolder connectionHolder) {

		ConnectionInformation connection = connectionHolder.getConnectionInformation();
		RepositoryLocation location = connectionHolder.getLocation();
		ConnectionGUI editGui = null;
		boolean editable = isEditable();

		if (!isTypeKnown) {
			return new UnknownConnectionTypeGUIProvider().edit(this, connection, location, false);
		}

		ConnectionGUIProvider guiProvider = AccessController.doPrivileged((PrivilegedAction<ConnectionGUIProvider>)
				() -> ConnectionGUIRegistry.INSTANCE.getGUIProvider(connection.getConfiguration().getType()));

		if (guiProvider != null) {
			editGui = guiProvider.edit(this, connection, location, editable);
		}

		// fallback for when connection BEs are developed, and there is no frontend yet. This allows to at least have label - field pairs for each parameter
		if (editGui == null) {
			LogService.getRoot().log(Level.SEVERE, "com.rapidminer.connection.gui.ConnectionEditDialog.fallback_gui_used", ConnectionI18N.getTypeName(connection.getConfiguration().getType()));
			editGui = new DefaultConnectionGUIProvider().edit(this, connection, location, editable);
		}
		return editGui;
	}

	/**
	 * Returns the title for the given connection information
	 *
	 * @param holder
	 * 		the connection information
	 * @param editMode
	 * 		if the dialog is opened in edit mode
	 * @return the dialog title
	 */
	private static String getTitle(ConnectionInformationHolder holder, boolean editMode) {
		final String i18n;
		if (holder.isEditable() && editMode && ConnectionHandlerRegistry.getInstance().isTypeKnown(holder.getConnectionType())) {
			i18n = "edit_connection";
		} else {
			i18n = "view_connection";
		}
		return ConnectionI18N.getConnectionGUILabel(i18n, holder.getConnectionInformation().getConfiguration().getName());
	}

}
