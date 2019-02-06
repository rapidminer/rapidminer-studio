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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.LinkedList;
import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.PerspectiveModel;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.operator.UserError;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;


/**
 * The error message dialog. Several buttons are provided in addition to the error message. Details
 * about the exception can be shown and an edit button can jump to the source code if an editor was
 * defined in the properties / settings. In case of a non-expected error (i.e. all non-user errors)
 * a button for sending a bug report is also provided.
 *
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht, Marco Boeck
 */
public class ExtendedErrorDialog extends ButtonDialog {

	private static final long serialVersionUID = -8136329951869702133L;

	private static final int SIZE = ButtonDialog.DEFAULT_SIZE;

	private final JButton editButton = new JButton("Edit");

	private JButton sendReport;

	private Throwable error;

	private final JComponent mainComponent = new JPanel(new BorderLayout());

	/**
	 * Creates a dialog with the internationalized I18n-message from the given key and a panel for
	 * detailed stack trace.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param error
	 *            the exception associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @deprecated use {@link #ExtendedErrorDialog(Window, String, Throwable, Object...)} instead
	 */
	@Deprecated
	public ExtendedErrorDialog(String key, Throwable error, Object... arguments) {
		this(key, error, false, arguments);
	}

	/**
	 * Creates a dialog with the internationalized I18n-message from the given key and a panel for
	 * detailed stack trace.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param error
	 *            the exception associated to this message
	 * @param displayExceptionMessage
	 *            indicates if the exception message can be shown using the details button.
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @deprecated use {@link #ExtendedErrorDialog(Window, String, Throwable, boolean, Object...)}
	 *             instead
	 */
	@Deprecated
	public ExtendedErrorDialog(String key, Throwable error, boolean displayExceptionMessage, Object... arguments) {
		this(ApplicationFrame.getApplicationFrame(), key, error, displayExceptionMessage, arguments);
	}

	/**
	 * Creates a dialog with the internationalized I18n-message from the given key.
	 *
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param errorMessage
	 *            the error message associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @deprecated use {@link #ExtendedErrorDialog(Window, String, String, Object...)} instead
	 */
	@Deprecated
	public ExtendedErrorDialog(String key, String errorMessage, Object... arguments) {
		this(ApplicationFrame.getApplicationFrame(), key, errorMessage, arguments);
	}

	/**
	 * Creates a dialog with the internationalized I18n-message from the given key and a panel for
	 * detailed stack trace.
	 *
	 * @param owner
	 *            the owner window in which the dialog is displayed
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param error
	 *            the exception associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @since 6.5.0
	 */
	public ExtendedErrorDialog(Window owner, String key, Throwable error, Object... arguments) {
		this(owner, key, error, false, arguments);
	}

	/**
	 * Creates a dialog with the internationalized I18n-message from the given key and a panel for
	 * detailed stack trace.
	 *
	 * @param owner
	 *            the owner window in which the dialog is displayed
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param error
	 *            the exception associated to this message
	 * @param displayExceptionMessage
	 *            indicates if the exception message can be shown using the details button.
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @since 6.5.0
	 */
	public ExtendedErrorDialog(Window owner, String key, Throwable error, boolean displayExceptionMessage,
			Object... arguments) {
		super(owner, "error." + key, ModalityType.APPLICATION_MODAL, arguments);
		this.error = error;

		boolean hasError = error != null;
		JComponent detailedPane = hasError ? createDetailPanel(error) : null;

		if (error != null && error instanceof UserError && ((UserError) error).getOperator() != null) {
			final String opName = ((UserError) error).getOperator().getName();
			mainComponent.add(new LinkLocalButton(new ResourceAction("show_offending_operator", opName) {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					MainFrame mainFrame = RapidMinerGUI.getMainFrame();
					mainFrame.getPerspectiveController().showPerspective(PerspectiveModel.DESIGN);
					mainFrame.selectAndShowOperator(mainFrame.getProcess().getOperator(opName), true);
				}
			}), BorderLayout.NORTH);
		}

		layoutDefault(mainComponent, SIZE,
				getButtons(hasError && displayExceptionMessage, isBugReportException(error), detailedPane, error));
	}

	/**
	 * Creates a dialog with the internationalized I18n-message from the given key.
	 *
	 * @param owner
	 *            the owner window in which the dialog is displayed
	 * @param key
	 *            the I18n-key which will be used to display the internationalized message
	 * @param errorMessage
	 *            the error message associated to this message
	 * @param arguments
	 *            additional arguments for the internationalized message, which replace
	 *            <code>{0}</code>, <code>{1}</code>, etcpp.
	 * @since 6.5.0
	 */
	public ExtendedErrorDialog(Window owner, String key, String errorMessage, Object... arguments) {
		super(owner, "error." + key, ModalityType.APPLICATION_MODAL, arguments);

		boolean hasError = errorMessage != null && !errorMessage.isEmpty();
		JScrollPane detailedPane = hasError ? createDetailPanel(errorMessage) : null;

		layoutDefault(mainComponent, SIZE, getButtons(hasError, false, detailedPane, null));
	}

	@Override
	protected Icon getInfoIcon() {
		String configuredIcon = I18N.getMessageOrNull(I18N.getGUIBundle(), getKey() + ".icon");
		if (configuredIcon != null) {
			return super.getInfoIcon();
		}
		return SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.icon"));
	}

	/**
	 * Creates a Panel for the error details and attaches the exception to it, but doesn't add the
	 * Panel to the dialog.
	 *
	 * @param error
	 * @return
	 */
	private JComponent createDetailPanel(Throwable error) {
		StackTraceList stl = new StackTraceList(error);
		JScrollPane detailPane = new ExtendedJScrollPane(stl);
		detailPane.setPreferredSize(new Dimension(getWidth(), 200));
		detailPane.setBorder(null);
		return detailPane;
	}

	/**
	 * Creates a Panel for the error details and attaches the error message to it, but doesn't add
	 * the Panel to the dialog.
	 *
	 * @param errorMessage
	 * @return
	 */
	private JScrollPane createDetailPanel(String errorMessage) {

		JTextArea textArea = new JTextArea(errorMessage);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		JScrollPane detailPane = new ExtendedJScrollPane(textArea);
		detailPane.setPreferredSize(new Dimension(getWidth(), 200));
		return detailPane;
	}

	/**
	 * Adds all necessary buttons to the dialog.
	 *
	 * @param hasError
	 * @param isBug
	 * @param detailedPane
	 *            the Panel which will be shown, if the user clicks on the 'Show Details' Button
	 * @param error
	 *            The error occurred
	 * @return
	 */
	@SuppressWarnings("unused") // Contains Bugzilla code for later usage
	private Collection<AbstractButton> getButtons(boolean hasError, boolean isBug, final JComponent detailedPane,
			final Throwable error) {
		Collection<AbstractButton> buttons = new LinkedList<>();
		if (hasError && !(error instanceof RepositoryException)) {
			final JToggleButton showDetailsButton = new JToggleButton(
					I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.show_details.label"), SwingTools
							.createIcon("24/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.error.show_details.icon")));
			showDetailsButton.setSelected(false);
			showDetailsButton.addActionListener(new ActionListener() {

				private boolean detailsShown = false;

				@Override
				public void actionPerformed(ActionEvent e) {
					if (detailsShown) {
						int width2 = ExtendedErrorDialog.this.getWidth();
						mainComponent.remove(detailedPane);

						ExtendedErrorDialog.this
								.setPreferredSize(new Dimension(width2, ExtendedErrorDialog.this.getHeight() - 150));
						pack();
					} else {
						int width2 = ExtendedErrorDialog.this.getWidth();
						mainComponent.add(detailedPane, BorderLayout.CENTER);

						ExtendedErrorDialog.this
								.setPreferredSize(new Dimension(width2, ExtendedErrorDialog.this.getHeight() + 150));
						pack();
					}
					detailsShown = !detailsShown;
				}
			});
			buttons.add(showDetailsButton);

		}

		/*
		 * Link to the RapidMiner community, can be removed when JIRA issue collection is ready
		 */
		if (isBug) {
			sendReport = new JButton(new ResourceAction("report_bug") {

				private static final long serialVersionUID = 1L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					RMUrlHandler.openInBrowser(I18N.getMessage(I18N.getGUIBundle(), "gui.action.report_bug.url"));
				}
			});
			// don't show "Report Bug" button if this dialog is shown when RM is only embedded
			if (!RapidMiner.getExecutionMode().equals(ExecutionMode.EMBEDDED_WITH_UI)) {
				buttons.add(sendReport);
			}
		}

		buttons.add(makeCloseButton());
		return buttons;
	}

	/**
	 * Returns <code>true</code> if this is a "real" bug, <code>false</code> otherwise.
	 *
	 * @param t
	 * @return
	 */
	private boolean isBugReportException(Throwable t) {
		return t instanceof RuntimeException || t instanceof Error;
		// return !(t instanceof NoBugError || t instanceof XMLException || t instanceof
		// RepositoryException);
	}

	/**
	 * Overrides the {@link ButtonDialog} method to add the exception message to the
	 * internationalized message
	 */
	@Override
	protected String getInfoText() {
		if (error != null) {
			StringBuilder infoText = new StringBuilder();
			infoText.append("<div>");
			infoText.append(super.getInfoText());
			infoText.append("</div>");

			// if already arguments are given, we can expect already a detailed error message
			if (arguments.length == 0 && error.getMessage() != null && error.getMessage().length() > 0) {
				infoText.append("<br/>");
				infoText.append(Tools.escapeHTML(error.getMessage()));
			}

			return infoText.toString();
		} else {
			return super.getInfoText();
		}

	}

	private static class FormattedStackTraceElement {

		private final StackTraceElement ste;

		private FormattedStackTraceElement(StackTraceElement ste) {
			this.ste = ste;
		}

		@Override
		public String toString() {
			return "  " + ste;
		}
	}

	private class StackTraceList extends JList<Object> {

		private static final long serialVersionUID = -2482220036723949144L;

		public StackTraceList(Throwable t) {
			super(new DefaultListModel<>());
			setFont(getFont().deriveFont(Font.PLAIN));
			setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			appendAllStackTraces(t);
			addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (getSelectedIndex() >= 0) {
						if (!(getSelectedValue() instanceof FormattedStackTraceElement)) {
							editButton.setEnabled(false);
						} else {
							editButton.setEnabled(true);
						}
					} else {
						editButton.setEnabled(true);
					}
				}
			});
		}

		private DefaultListModel<Object> model() {
			return (DefaultListModel<Object>) getModel();
		}

		private void appendAllStackTraces(Throwable throwable) {
			while (throwable != null) {
				appendStackTrace(throwable);
				throwable = throwable.getCause();
				if (throwable != null) {
					model().addElement("");
					model().addElement("Cause");
				}
			}
		}

		private void appendStackTrace(Throwable throwable) {
			model().addElement("Exception: " + throwable.getClass().getName());
			model().addElement("Message: " + throwable.getMessage());
			model().addElement("Stack trace:" + Tools.getLineSeparator());
			for (int i = 0; i < throwable.getStackTrace().length; i++) {
				model().addElement(new FormattedStackTraceElement(throwable.getStackTrace()[i]));
			}
		}
	}
}
