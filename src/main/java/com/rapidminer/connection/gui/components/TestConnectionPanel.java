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
package com.rapidminer.connection.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionStatistics;
import com.rapidminer.connection.gui.actions.TestConnectionAction;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.gui.actions.CopyStringToClipboardAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;


/**
 * A panel that allows to test a {@link ConnectionInformation} and show the result.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class TestConnectionPanel extends JPanel {

	private static final Color TEST_BACKGROUND = Colors.PANEL_BACKGROUND;
	private static final Color TEST_BORDER = Colors.TAB_BORDER;
	private static final int MESSAGE_WIDTH = 300;

	private static final ImageIcon RUNNING_ICON = SwingTools.createIcon("16/" + ConnectionI18N.getConnectionGUIMessage("test.running.icon"));
	private static final ImageIcon EMPTY_ICON = SwingTools.createIconFromColor(TEST_BACKGROUND, TEST_BACKGROUND, 16, 16, new Rectangle2D.Double(0, 0, 16, 16));
	private static final ImageIcon NOT_SUPPORTED_ICON = SwingTools.createIcon("16/" + ConnectionI18N.getConnectionGUIMessage("test.not_supported.icon"));
	private static final ImageIcon WARNING_ICON = SwingTools.createIcon("16/" + ConnectionI18N.getConnectionGUIMessage("test.warning.icon"));
	private static final ImageIcon SUCCESS_ICON = SwingTools.createIcon("16/" + ConnectionI18N.getConnectionGUIMessage("test.success.icon"));
	private static final Color COLOR_SUCCESS = new Color(75, 120, 0);
	private static final String NEWLINE_HTML = "<br/>";
	private static final String NEWLINE_TEXT = "\n";

	private final JLabel resultIconLabel;
	private final FixedWidthLabel resultDisplay;
	private final transient ConnectionStatistics statistics;
	private final String connectionType;


	/**
	 * Creates a new test connection panel
	 *
	 * @param type the connection type
	 * @param connectionSupplier
	 * 		the supplier for the connection
	 * @param testResultConsumer
	 * 		the test result consumer that should be told about test results
	 * @param statistics
	 * 		the statistics that get updated with test results
	 */
	public TestConnectionPanel(String type, Supplier<ConnectionInformation> connectionSupplier, Consumer<TestResult> testResultConsumer, ConnectionStatistics statistics) {
		super(new GridBagLayout());
		this.statistics = statistics;
		this.connectionType = type;
		boolean isTypeKnown = ConnectionHandlerRegistry.getInstance().isTypeKnown(type);
		setPreferredSize(new Dimension(431, 60));
		GridBagConstraints gbc = new GridBagConstraints();
		setBackground(TEST_BACKGROUND);
		setBorder(BorderFactory.createLineBorder(TEST_BORDER));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(10, 10, 10, 10);
		TestConnectionAction testAction = new TestConnectionAction(connectionSupplier, testResultConsumer);
		testAction.setEnabled(isTypeKnown);
		testAction.addPropertyChangeListener(evt -> repaint());
		JButton testButton = new JButton(testAction);
		add(testButton, gbc);

		gbc.gridx = 1;
		gbc.insets = new Insets(0, 0, 0, 10);
		resultIconLabel = new JLabel();
		resultIconLabel.setIcon(EMPTY_ICON);
		add(resultIconLabel, gbc);

		gbc.gridx = 2;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		resultDisplay = new FixedWidthLabel(MESSAGE_WIDTH, "");
		resultDisplay.setBackground(TEST_BACKGROUND);
		JPopupMenu copyMenu = new JPopupMenu();
		copyMenu.add(new CopyStringToClipboardAction(true, "connection.test.copy_result", TestConnectionPanel.this::getTestResultText));
		resultDisplay.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				handlePopup(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				handlePopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				handlePopup(e);
			}

			private void handlePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					copyMenu.show(resultDisplay, e.getX(), e.getY());
				}
			}
		});
		JScrollPane scrollPane = new ExtendedJScrollPane(resultDisplay);

		if (!isTypeKnown) {
			setTestResult(new TestResult(TestResult.ResultType.FAILURE, "unknown_provider_warning.label", null));
		}


		scrollPane.getViewport().setBackground(TEST_BACKGROUND);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, gbc);
	}

	/**
	 * Updates the test result
	 *
	 * @param testResult
	 * 		the test result
	 */
	public void setTestResult(TestResult testResult) {
		if (testResult == null) {
			resultDisplay.setText("");
			resultIconLabel.setIcon(EMPTY_ICON);
			return;
		}

		// prepare proper i18n message
		String i18nMessage;
		if (!testResult.getParameterErrorMessages().isEmpty()) {
			StringBuilder sb = new StringBuilder();
			testResult.getParameterErrorMessages().forEach((key, i18nKey) -> {
				// we can rely on this split because it's enforced in the val result builder
				String[] splittedGroupKey = key.split("\\.", 2);
				String groupKey = splittedGroupKey[0];
				String parameterKey = splittedGroupKey[1];
				sb.append(ConnectionI18N.getValidationErrorMessage(i18nKey, connectionType, groupKey, parameterKey)).append(NEWLINE_HTML);
			});
			i18nMessage = sb.toString();
		} else {
			i18nMessage = ConnectionI18N.getConnectionGUIMessage(testResult.getMessageKey(), testResult.getArguments());
		}

		switch (testResult.getType()) {
			case SUCCESS:
				statistics.updateSuccess();
				resultDisplay.setForeground(COLOR_SUCCESS);
				resultIconLabel.setIcon(SUCCESS_ICON);
				break;
			case FAILURE:
				statistics.updateError(i18nMessage);
				resultDisplay.setForeground(Color.RED);
				resultIconLabel.setIcon(WARNING_ICON);
				break;
			case NOT_SUPPORTED:
				resultDisplay.setForeground(Color.BLACK);
				resultIconLabel.setIcon(NOT_SUPPORTED_ICON);
				break;
			case NONE:
			default:
				resultDisplay.setForeground(Color.BLACK);
				resultIconLabel.setIcon(RUNNING_ICON);
		}

		resultDisplay.setText(i18nMessage);
		int maxWidth = Math.min(resultDisplay.getFontMetrics(resultDisplay.getFont()).stringWidth(i18nMessage), MESSAGE_WIDTH * 2);
		resultDisplay.setToolTipText("<html><body><div style=\"width:" + maxWidth + "pt\">" + i18nMessage + "</div></body></html>");
	}

	private String getTestResultText() {
		return resultDisplay.getPlaintext().replaceAll(NEWLINE_HTML, NEWLINE_TEXT);
	}
}
