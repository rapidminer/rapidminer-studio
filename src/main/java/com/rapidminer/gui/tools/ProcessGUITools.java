/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.renderer.RendererService;
import com.rapidminer.gui.tools.bubble.BubbleWindow;
import com.rapidminer.gui.tools.bubble.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tools.bubble.BubbleWindow.BubbleStyle;
import com.rapidminer.gui.tools.bubble.OperatorInfoBubble;
import com.rapidminer.gui.tools.bubble.OperatorInfoBubble.OperatorBubbleBuilder;
import com.rapidminer.gui.tools.bubble.ParameterErrorInfoBubble;
import com.rapidminer.gui.tools.bubble.ParameterErrorInfoBubble.ParameterErrorBubbleBuilder;
import com.rapidminer.gui.tools.bubble.PortInfoBubble;
import com.rapidminer.gui.tools.bubble.PortInfoBubble.PortBubbleBuilder;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.PortUserError;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.error.ParameterError;
import com.rapidminer.operator.error.ProcessExecutionUserErrorError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.parameter.CombinedParameterType;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;


/**
 * This class contains GUI utility methods related to {@link com.rapidminer.Process}es.
 *
 * @author Marco Boeck
 * @since 6.5.0
 *
 */
public class ProcessGUITools {

	private static class BubbleDelegator {

		private WeakReference<BubbleWindow> bubble;

		private void setBubbleWindow(BubbleWindow bubble) {
			this.bubble = new WeakReference<BubbleWindow>(bubble);
		}

		/**
		 * @return the bubble or {@code null}
		 */
		private BubbleWindow getBubble() {
			return bubble.get();
		}
	}

	/**
	 * Private constructor which throws if called.
	 */
	private ProcessGUITools() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Displays the best machting error bubble depending on the given {@link UserError}. If no
	 * explicit match can be found, displays a generic error bubble.
	 *
	 * @param error
	 *            the error in question, must not be {@code null}
	 * @return the bubble instance, never {@code null}
	 */
	public static BubbleWindow displayBubbleForUserError(final UserError error) {
		if (error == null) {
			throw new IllegalArgumentException("userError must not be null!");
		}

		if (error instanceof PortUserError) {
			PortUserError userError = (PortUserError) error;
			if (userError.getCode() == 149) {
				// for "no data" errors we display an error bubble instead of a dialog
				return displayInputPortNoDataInformation(userError.getPort());
			} else if (userError.getCode() == 156) {
				return displayInputPortWrongTypeInformation(userError.getPort(), userError.getExpectedType(),
						userError.getActualType());
			} else {
				return displayGenericPortError(userError);
			}
		} else if (error.getClass().equals(UndefinedParameterError.class)) {
			UndefinedParameterError userError = (UndefinedParameterError) error;
			if (userError.getCode() == 205 || userError.getCode() == 217) {
				// for "missing mandatory parameter" errors we display an error bubble
				// instead of a dialog
				Operator op = userError.getOperator();
				ParameterType param = op != null ? op.getParameterType(userError.getKey()) : null;
				if (op != null && param != null) {
					return displayMissingMandatoryParameterInformation(op, param);
				} else {
					return displayGenericUserError(error);
				}
			} else {
				Operator op = userError.getOperator();
				ParameterType param = op != null ? op.getParameterType(userError.getKey()) : null;
				if (op != null && param != null) {
					return displayMissingMandatoryParameterInformation(op, param);
				} else {
					return displayGenericUserError(error);
				}
			}
		} else if (error instanceof ParameterError) {
			ParameterError userError = (ParameterError) error;
			Operator op = userError.getOperator();
			ParameterType param = op != null ? op.getParameterType(userError.getKey()) : null;
			if (op != null && param != null) {
				return displayGenericParameterError(userError);
			} else {
				return displayGenericUserError(error);
			}
		} else if (error instanceof AttributeNotFoundError) {
			AttributeNotFoundError userError = (AttributeNotFoundError) error;
			Operator op = userError.getOperator();
			ParameterType param = op != null ? op.getParameterType(userError.getKey()) : null;
			if (op != null && param != null) {
				return displayAttributeNotFoundParameterInformation(userError);
			} else {
				return displayGenericUserError(error);
			}
		} else if (error instanceof ProcessExecutionUserErrorError) {
			ProcessExecutionUserErrorError userError = (ProcessExecutionUserErrorError) error;
			if (userError.getUserError() != null && userError.getUserError().getOperator() != null) {
				return displayUserErrorInExecutedProcess(userError);
			} else {
				return displayGenericUserError(error);
			}
		} else {
			return displayGenericUserError(error);
		}
	}

	/**
	 * Displays a warning bubble that alerts the user that he failed to connect any of the process
	 * result ports.
	 *
	 * The bubble is located at the first result port of the outermost process and the process view
	 * will change to said port. The {@link ResultWarningPreventionRegistry} contains a list with
	 * Operators which can suppress this warning.
	 *
	 * @param process
	 *            the process in question
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	public static PortInfoBubble displayPrecheckNoResultPortInformation(final Process process) {
		if (process == null) {
			throw new IllegalArgumentException("port must not be null!");
		}

		Port firstResultPort = process.getRootOperator().getSubprocess(0).getInnerSinks().getPortByIndex(0);
		JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble.process_unconnected_result_port.button.label"));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble.process_unconnected_result_port.button.tip"));

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final JCheckBox dismissForeverCheckBox = new JCheckBox(
				I18N.getGUIMessage("gui.bubble.process_unconnected_result_port.button_dismiss.label"));
		dismissForeverCheckBox
				.setToolTipText(I18N.getGUIMessage("gui.bubble.process_unconnected_result_port.button_dismiss.tip"));
		ResourceAction runAnywayAction = new ResourceAction("process_unconnected_result_port.button_run_anyway", "F11") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				BubbleWindow bubble = bubbleDelegator.getBubble();
				if (bubble != null) {
					bubble.killBubble(true);
				}

				if (dismissForeverCheckBox.isSelected()) {
					// store setting
					ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_SHOW_NO_RESULT_WARNING,
							String.valueOf(Boolean.FALSE));
					ParameterService.saveParameters();
				}

				// run process without checking for problems
				RapidMinerGUI.getMainFrame().runProcess(false);
			}
		};

		LinkLocalButton runAnywayButton = new LinkLocalButton(runAnywayAction);
		runAnywayButton
				.setToolTipText(I18N.getGUIMessage("gui.bubble.process_unconnected_result_port.button_run_anyway.tip"));
		runAnywayButton.registerKeyboardAction(runAnywayAction, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		PortBubbleBuilder builder = new PortBubbleBuilder(RapidMinerGUI.getMainFrame(), firstResultPort,
				"process_unconnected_result_port");

		JPanel actionPanel = new JPanel(new GridBagLayout());
		actionPanel.setOpaque(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;

		gbc.insets = new Insets(0, 0, 5, 0);
		gbc.gridwidth = 2;
		dismissForeverCheckBox.setOpaque(false);
		actionPanel.add(dismissForeverCheckBox, gbc);

		gbc.gridy += 1;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(0, 0, 0, 10);
		actionPanel.add(ackButton, gbc);

		gbc.gridx += 1;
		actionPanel.add(runAnywayButton, gbc);

		final PortInfoBubble noResultConnectionBubble = builder.setHideOnConnection(true).setAlignment(AlignedSide.LEFT)
				.setStyle(BubbleStyle.WARNING).setHideOnProcessRun(false).setEnsureVisible(true).hideCloseButton()
				.setAdditionalComponents(new JComponent[] { actionPanel }).build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				noResultConnectionBubble.killBubble(true);

				if (dismissForeverCheckBox.isSelected()) {
					// store setting
					ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_SHOW_NO_RESULT_WARNING,
							String.valueOf(Boolean.FALSE));
					ParameterService.saveParameters();
				}
			}
		});
		bubbleDelegator.setBubbleWindow(noResultConnectionBubble);

		noResultConnectionBubble.setVisible(true);
		return noResultConnectionBubble;
	}

	/**
	 * Displays a warning bubble that alerts the user that a mandatory parameter of an operator
	 * needs a value because it has no default value. The bubble is located at the operator and the
	 * process view will change to said operator. This is a bubble which occurs during the check for
	 * errors before process execution so it contains a link button to disable pre-execution checks.
	 *
	 * @param op
	 *            the operator for which to display the warning
	 * @param param
	 *            the parameter for which to display the warning
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayPrecheckMissingMandatoryParameterWarning(final Operator op,
			final ParameterType param) {
		if (op == null) {
			throw new IllegalArgumentException("op must not be null!");
		}
		if (param == null) {
			throw new IllegalArgumentException("param must not be null!");
		}

		// not the user entered name because that could be god knows how long
		String opName = op.getOperatorDescription().getName();
		return displayPrecheckMissingParameterWarning(op, param, false, "process_precheck_mandatory_parameter_unset", opName,
				param.getKey());
	}

	/**
	 * Displays a warning bubble that alerts the user that an input port of an operator expects
	 * input but is not connected. The bubble is located at the port and the process view will
	 * change to said port. This is a bubble which occurs during the check for errors before process
	 * execution so it contains a link button to disable pre-execution checks.
	 *
	 * @param port
	 *            the port for which to display the error
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	public static PortInfoBubble displayPrecheckInputPortDisconnectedWarning(final Port port) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}

		// PortOwner is an interface only implemented from anonymous inner classes
		// so check enclosing class and differentiate operator input ports and subprocess
		// (result) input ports
		String key;
		if (ExecutionUnit.class.isAssignableFrom(port.getPorts().getOwner().getClass().getEnclosingClass())) {
			key = "process_precheck_mandatory_input_port_unconnected_inner";
		} else {
			key = "process_precheck_mandatory_input_port_unconnected";
		}
		return displayPrecheckMissingInputPortWarning(port, true, false, key);
	}

	/**
	 * Displays an error bubble that alerts the user that the attribute specified in the parameters
	 * of an operator was not found. The bubble is located at the operator and the process view will
	 * change to said port. This method is used after the error occurred during process execution.
	 *
	 * @param error
	 *            the error containing all the information about the operator, the parameter and the
	 *            name of the attribute which was not found
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayAttributeNotFoundParameterInformation(final AttributeNotFoundError error) {
		if (error == null) {
			throw new IllegalArgumentException("error must not be null!");
		}
		if (error.getOperator() == null) {
			throw new IllegalArgumentException("error operator must not be null!");
		}
		if (error.getKey() == null) {
			throw new IllegalArgumentException("error parameter key must not be null!");
		}

		String key;
		switch (error.getCode()) {
			case AttributeNotFoundError.ATTRIBUTE_NOT_FOUND_IN_REGULAR:
				key = "process_regular_attribute_not_found_parameter";
				break;
			case AttributeNotFoundError.ATTRIBUTE_NOT_FOUND:
			default:
				key = "process_attribute_not_found_parameter";
		}
		return displayAttributeNotFoundParameterInformation(error, true, key, error.getAttributeName(), error.getKey());
	}

	/**
	 * Displays an error bubble that alerts the user that a mandatory parameter of an operator was
	 * not set and has no default value. The bubble is located at the operator and the process view
	 * will change to said port. This method is used after the error occurred during process
	 * execution.
	 *
	 * @param op
	 *            the operator for which to display the error
	 * @param param
	 *            the parameter for which to display the error
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayMissingMandatoryParameterInformation(final Operator op,
			final ParameterType param) {
		if (op == null) {
			throw new IllegalArgumentException("op must not be null!");
		}
		if (param == null) {
			throw new IllegalArgumentException("param must not be null!");
		}

		String key = "process_mandatory_parameter_missing";
		// not the user entered name because that could be god knows how long
		String opName = op.getOperatorDescription().getName();
		return displayMissingMandatoryParameterInformation(op, param, true, key, opName, param.getKey());
	}

	/**
	 * Displays an error bubble that alerts the user that an input port of an operator expected
	 * input but did not receive any. The bubble is located at the port and the process view will
	 * change to said port. This method is used after the error occurred during process execution.
	 *
	 * @param port
	 *            the port for which to display the error
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	public static PortInfoBubble displayInputPortNoDataInformation(final Port port) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}

		String key;
		if (port.isConnected()) {
			key = "process_mandatory_input_port_no_data";
		} else {
			// PortOwner is an interface only implemented from anonymous inner classes
			// so check enclosing class and differentiate operator input ports and subprocess
			// (result) input ports
			if (ExecutionUnit.class.isAssignableFrom(port.getPorts().getOwner().getClass().getEnclosingClass())) {
				key = "process_mandatory_input_port_no_data_unconnected_inner";
			} else {
				key = "process_mandatory_input_port_no_data_unconnected";
			}
		}
		String opName = "";
		if (port instanceof InputPort) {
			InputPort inPort = (InputPort) port;
			OutputPort source = inPort.getSource();
			if (source != null) {
				// not the user entered name because that could be god knows how long
				opName = source.getPorts().getOwner().getOperator().getOperatorDescription().getName();
			}
		}
		return displayMissingInputPortInformation(port, !port.isConnected(), true, key, opName);
	}

	/**
	 * Displays an error bubble that alerts the user that an input port of an operator expected
	 * input of a certain type but received the wrong type. The bubble is located at the port and
	 * the process view will change to said port. This method is used after the error occurred
	 * during process execution.
	 *
	 * @param port
	 *            the port for which to display the error
	 * @param expectedType
	 *            the expected data type for the port
	 * @param actualType
	 *            the actually delivered type for the port
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	public static PortInfoBubble displayInputPortWrongTypeInformation(final Port port, final Class<?> expectedType,
			final Class<?> actualType) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}
		if (expectedType == null) {
			throw new IllegalArgumentException("expectedType must not be null!");
		}
		if (actualType == null) {
			throw new IllegalArgumentException("actualType must not be null!");
		}

		String key = "process_mandatory_input_port_wrong_type";
		return displayMissingInputPortInformation(port, true, true, key, RendererService.getName(expectedType),
				RendererService.getName(actualType));
	}

	/**
	 * Displays an information bubble pointing to an ExecuteProcess operator to indicate a
	 * {@link UserError} occurred inside the process executed by the operator.
	 *
	 * @param userError
	 *            the error instance
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayUserErrorInExecutedProcess(final ProcessExecutionUserErrorError userError) {
		String referencedOperatorName = userError.getUserError().getOperator().getName();
		return displayUserErrorInExecutedProcess(userError, "executed_process_usererror", referencedOperatorName,
				userError.getUserError().getDetails());
	}

	/**
	 * Displays an information bubble pointing to an operator to indicate a {@link UserError} was
	 * thrown by the operator.
	 *
	 * @param userError
	 *            the error instance
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayGenericUserError(final UserError userError) {
		if (userError == null) {
			throw new IllegalArgumentException("userError must not be null!");
		}

		return displayGenericUserError(userError, "generic_usererror");
	}

	/**
	 * Displays an information bubble pointing to a port to indicate a {@link PortUserError} was
	 * thrown.
	 *
	 * @param portError
	 *            the error instance
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	public static PortInfoBubble displayGenericPortError(final PortUserError portError) {
		if (portError == null) {
			throw new IllegalArgumentException("portError must not be null!");
		}

		return displayGenericPortError(portError, "generic_usererror");
	}

	/**
	 * Displays an information bubble pointing to an operator to indicate a {@link UserError} was
	 * thrown by the operator.
	 *
	 * @param userError
	 *            the error instance
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayGenericParameterError(final ParameterError userError) {
		if (userError == null) {
			throw new IllegalArgumentException("userError must not be null!");
		}
		Operator op = userError.getOperator();
		if (op == null) {
			throw new IllegalArgumentException("ParameterError operator must not be null!");
		}
		ParameterType param = op.getParameterType(userError.getKey());
		if (param == null) {
			throw new IllegalArgumentException("ParameterError key must point to a valid ParameterType!");
		}

		return displayGenericParameterError(userError, op, param, "generic_paramerror");
	}

	/**
	 * Displays a warning bubble that alerts the user that a mandatory parameter of an operator has
	 * no value and no default value. The bubble is located at the operator and the process view
	 * will change to said port.
	 *
	 * @param op
	 *            the operator for which to display the warning
	 * @param param
	 *            the parameter for which to display the warning
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @param isError
	 *            if {@code true}, an error bubble will be shown; otherwise a warning bubble is
	 *            displayed
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	private static OperatorInfoBubble displayPrecheckMissingParameterWarning(final Operator op, final ParameterType param,
			final boolean isError, final String i18nKey, final Object... arguments) {
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		ResourceAction runAnywayAction = new ResourceAction(i18nKey + ".button_run_anyway", "F11") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				BubbleWindow bubble = bubbleDelegator.getBubble();
				if (bubble != null) {
					bubble.killBubble(true);
				}

				// run process without checking for problems
				RapidMinerGUI.getMainFrame().runProcess(false);
			}
		};
		LinkLocalButton runAnywayButton = new LinkLocalButton(runAnywayAction);
		runAnywayButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button_run_anyway.tip"));
		runAnywayButton.registerKeyboardAction(runAnywayAction, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label", arguments));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(), op, param,
				"mandatory_parameter_decoration", i18nKey, arguments);
		final OperatorInfoBubble missingParameterBubble = builder.setHideOnDisable(true).setAlignment(AlignedSide.BOTTOM)
				.setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING).setEnsureVisible(true).hideCloseButton()
				.setHideOnProcessRun(true).setAdditionalComponents(new JComponent[] { ackButton, runAnywayButton }).build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				missingParameterBubble.killBubble(true);
			}
		});
		bubbleDelegator.setBubbleWindow(missingParameterBubble);

		missingParameterBubble.setVisible(true);
		return missingParameterBubble;
	}

	/**
	 * Displays a warning bubble that alerts the user that an input port of an operator expected
	 * input but that there was a problem. The bubble is located at the port and the process view
	 * will change to said port. execution.
	 *
	 * @param port
	 *            the port for which to display the warning
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @param hideOnConnection
	 *            if {@code true}, the bubble will be hidden once the port is connected
	 * @param isError
	 *            if {@code true}, an error bubble will be shown; otherwise a warning bubble is
	 *            displayed
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	private static PortInfoBubble displayPrecheckMissingInputPortWarning(final Port port, final boolean hideOnConnection,
			final boolean isError, final String i18nKey, final Object... arguments) {
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		ResourceAction runAnywayAction = new ResourceAction(i18nKey + ".button_run_anyway", "F11") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				BubbleWindow bubble = bubbleDelegator.getBubble();
				if (bubble != null) {
					bubble.killBubble(true);
				}

				// run process without checking for problems
				RapidMinerGUI.getMainFrame().runProcess(false);
			}
		};
		LinkLocalButton runAnywayButton = new LinkLocalButton(runAnywayAction);
		runAnywayButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button_run_anyway.tip"));
		runAnywayButton.registerKeyboardAction(runAnywayAction, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label", arguments));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		PortBubbleBuilder builder = new PortBubbleBuilder(RapidMinerGUI.getMainFrame(), port, i18nKey, arguments);
		final PortInfoBubble missingInputBubble = builder.setHideOnConnection(hideOnConnection).setHideOnDisable(true)
				.setAlignment(AlignedSide.LEFT).setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING)
				.setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] { ackButton, runAnywayButton }).build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				missingInputBubble.killBubble(true);
			}
		});
		bubbleDelegator.setBubbleWindow(missingInputBubble);

		missingInputBubble.setVisible(true);
		return missingInputBubble;
	}

	/**
	 * Displays an information bubble that alerts the user that the attribute specified in the
	 * operator parameters was not found. The bubble is located at the operator and the process view
	 * will change to said operator. This method is used after the error occurred during process
	 * execution.
	 *
	 * @param error
	 *            the error containing all the information about the operator, the parameter and the
	 *            name of the attribute which was not found
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @param isError
	 *            if {@code true}, an error bubble will be shown; otherwise a warning bubble is
	 *            displayed
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	private static OperatorInfoBubble displayAttributeNotFoundParameterInformation(final AttributeNotFoundError error,
			final boolean isError, final String i18nKey, final Object... arguments) {
		final Operator op = error.getOperator();
		final ParameterType param = op.getParameterType(error.getKey());
		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label", arguments));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		String decoratorKey = param instanceof CombinedParameterType || param instanceof ParameterTypeAttributes
				? "attributes_not_found_decoration" : "attribute_not_found_decoration";

		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(), op, param,
				decoratorKey, i18nKey, arguments);
		final ParameterErrorInfoBubble attributeNotFoundParameterBubble = builder.setHideOnDisable(true)
				.setAlignment(AlignedSide.BOTTOM).setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING)
				.setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] { ackButton }).build();

		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				attributeNotFoundParameterBubble.killBubble(true);
			}
		});

		attributeNotFoundParameterBubble.setVisible(true);
		return attributeNotFoundParameterBubble;
	}

	/**
	 * Displays an information bubble that alerts the user that a mandatory operator parameter was
	 * not set and had no default value. The bubble is located at the operator and the process view
	 * will change to said operator. This method is used after the error occurred during process
	 * execution.
	 *
	 * @param op
	 *            the operator for which to display the error
	 * @param param
	 *            the parameter for which to display the error
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @param isError
	 *            if {@code true}, an error bubble will be shown; otherwise a warning bubble is
	 *            displayed
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	private static OperatorInfoBubble displayMissingMandatoryParameterInformation(final Operator op,
			final ParameterType param, final boolean isError, final String i18nKey, final Object... arguments) {
		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label", arguments));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(), op, param,
				"mandatory_parameter_decoration", i18nKey, arguments);
		final OperatorInfoBubble missingParameterBubble = builder.setHideOnDisable(true).setAlignment(AlignedSide.BOTTOM)
				.setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING).setEnsureVisible(true).hideCloseButton()
				.setHideOnProcessRun(true).setAdditionalComponents(new JComponent[] { ackButton }).build();

		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				missingParameterBubble.killBubble(true);
			}
		});

		missingParameterBubble.setVisible(true);
		return missingParameterBubble;
	}

	/**
	 * Displays an information bubble that alerts the user that an input port of an operator
	 * expected input but that there was a problem. The bubble is located at the port and the
	 * process view will change to said port. This method is used after the error occurred during
	 * process execution.
	 *
	 * @param port
	 *            the port for which to display the error
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @param hideOnConnection
	 *            if {@code true}, the bubble will be hidden once the port is connected
	 * @param isError
	 *            if {@code true}, an error bubble will be shown; otherwise a warning bubble is
	 *            displayed
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	private static PortInfoBubble displayMissingInputPortInformation(final Port port, final boolean hideOnConnection,
			final boolean isError, final String i18nKey, final Object... arguments) {
		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label", arguments));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		PortBubbleBuilder builder = new PortBubbleBuilder(RapidMinerGUI.getMainFrame(), port, i18nKey, arguments);
		final PortInfoBubble missingInputBubble = builder.setHideOnConnection(hideOnConnection).setHideOnDisable(true)
				.setAlignment(AlignedSide.LEFT).setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING)
				.setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] { ackButton }).build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				missingInputBubble.killBubble(true);
			}
		});
		missingInputBubble.setVisible(true);
		return missingInputBubble;
	}

	/**
	 * Displays an information bubble pointing to an operator to indicate a {@link ParameterError}
	 * was thrown by that operator. It will also select the operator and highlight the parameter in
	 * question.
	 *
	 * @param error
	 *            the error instance
	 * @param op
	 *            the operator for which to display the error
	 * @param param
	 *            the parameter for which to display the error
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	private static OperatorInfoBubble displayGenericParameterError(final ParameterError error, final Operator op,
			final ParameterType param, final String i18nKey) {
		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label"));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(error.getMessage());
		final JPanel linkPanel = new JPanel();
		LinkLocalButton showDetailsButton = new LinkLocalButton(new ResourceAction(i18nKey + ".button_show_details") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				BubbleWindow bubble = bubbleDelegator.getBubble();
				if (bubble != null) {
					String text = I18N.getMessage(I18N.getGUIBundle(), "gui.bubble." + i18nKey + ".body", message,
							error.getDetails());
					bubble.setMainText(text);

					linkPanel.removeAll();
					bubble.pack();
				}
			}
		});
		showDetailsButton.setToolTipText(I18N.getGUIMessage("gui.action." + i18nKey + ".button_show_details.tip"));
		linkPanel.add(showDetailsButton);

		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(),
				error.getOperator(), param, "generic_parameter_decoration", i18nKey, message, "");
		// if no operator or root operator, show in middle, otherwise below
		AlignedSide prefSide = error.getOperator() == null || error.getOperator() instanceof ProcessRootOperator
				? AlignedSide.MIDDLE : AlignedSide.BOTTOM;
		final OperatorInfoBubble userErrorBubble = builder.setHideOnDisable(true).setAlignment(prefSide)
				.setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] { ackButton, linkPanel }).build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				userErrorBubble.killBubble(true);
			}
		});
		bubbleDelegator.setBubbleWindow(userErrorBubble);

		userErrorBubble.setVisible(true);
		return userErrorBubble;
	}

	/**
	 * Shortens the provided message by removing the trailing termination characters like '.' or a
	 * '!'.
	 *
	 * @param message
	 *            the message to shorten
	 * @return the message without the trailing termination characters
	 */
	private static String removeTerminationCharacters(final String message) {
		if (message == null) {
			throw new IllegalArgumentException("Message must not be null");
		}
		String errMsg = message.trim();
		while (errMsg.endsWith(".") || errMsg.endsWith("!")) {
			errMsg = errMsg.substring(0, errMsg.length() - 1);
		}
		return errMsg;
	}

	/**
	 * Displays an information bubble pointing to an operator to indicate a {@link UserError} was
	 * thrown by that operator.
	 *
	 * @param error
	 *            the error instance
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	private static OperatorInfoBubble displayGenericUserError(final UserError error, final String i18nKey) {
		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label"));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(error.getMessage());
		final JPanel linkPanel = new JPanel();
		LinkLocalButton showDetailsButton = new LinkLocalButton(new ResourceAction(i18nKey + ".button_show_details") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				BubbleWindow bubble = bubbleDelegator.getBubble();
				if (bubble != null) {
					String text = I18N.getMessage(I18N.getGUIBundle(), "gui.bubble." + i18nKey + ".body", message,
							error.getDetails());
					bubble.setMainText(text);

					linkPanel.removeAll();
					bubble.pack();
				}
			}
		});
		showDetailsButton.setToolTipText(I18N.getGUIMessage("gui.action." + i18nKey + ".button_show_details.tip"));
		linkPanel.add(showDetailsButton);

		OperatorBubbleBuilder builder = new OperatorBubbleBuilder(RapidMinerGUI.getMainFrame(), error.getOperator(), i18nKey,
				message, "");
		// if no operator or root operator, show in middle, otherwise below
		AlignedSide prefSide = error.getOperator() == null || error.getOperator() instanceof ProcessRootOperator
				? AlignedSide.MIDDLE : AlignedSide.BOTTOM;
		final OperatorInfoBubble userErrorBubble = builder.setHideOnDisable(true).setAlignment(prefSide)
				.setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] { ackButton, linkPanel }).build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				userErrorBubble.killBubble(true);
			}
		});
		bubbleDelegator.setBubbleWindow(userErrorBubble);

		userErrorBubble.setVisible(true);
		return userErrorBubble;
	}

	/**
	 * Displays an information bubble pointing to a port to indicate a {@link PortUserError} was
	 * thrown.
	 *
	 * @param error
	 *            the error instance
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	private static PortInfoBubble displayGenericPortError(final PortUserError error, final String i18nKey) {
		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label"));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(error.getMessage());
		final JPanel linkPanel = new JPanel();
		LinkLocalButton showDetailsButton = new LinkLocalButton(new ResourceAction(i18nKey + ".button_show_details") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				BubbleWindow bubble = bubbleDelegator.getBubble();
				if (bubble != null) {
					String text = I18N.getMessage(I18N.getGUIBundle(), "gui.bubble." + i18nKey + ".body", message,
							error.getDetails());
					bubble.setMainText(text);

					linkPanel.removeAll();
					bubble.pack();
				}
			}
		});
		showDetailsButton.setToolTipText(I18N.getGUIMessage("gui.action." + i18nKey + ".button_show_details.tip"));
		linkPanel.add(showDetailsButton);

		// input ports (located left) show the "hook" of the bubble on the left and vice versa
		AlignedSide prefSide = error.getPort() instanceof InputPort ? AlignedSide.LEFT : AlignedSide.RIGHT;
		PortBubbleBuilder builder = new PortBubbleBuilder(RapidMinerGUI.getMainFrame(), error.getPort(), i18nKey, message,
				"");
		final PortInfoBubble portErrorBubble = builder.setHideOnDisable(true).setAlignment(prefSide)
				.setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] { ackButton, linkPanel }).build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				portErrorBubble.killBubble(true);
			}
		});
		bubbleDelegator.setBubbleWindow(portErrorBubble);

		portErrorBubble.setVisible(true);
		return portErrorBubble;
	}

	/**
	 * Displays an information bubble pointing to an ExecuteProcess operator to indicate a
	 * {@link UserError} occurred inside the process executed by the operator.
	 *
	 * @param error
	 *            the error instance
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	private static OperatorInfoBubble displayUserErrorInExecutedProcess(final ProcessExecutionUserErrorError error,
			final String i18nKey, final Object... arguments) {
		final JButton ackButton = new JButton(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.label", arguments));
		ackButton.setToolTipText(I18N.getGUIMessage("gui.bubble." + i18nKey + ".button.tip"));

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		LinkLocalButton showDetailsButton = new LinkLocalButton(new ResourceAction(i18nKey + ".button_show_details") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (RapidMinerGUI.getMainFrame().close(true)) {
					// kill bubble
					BubbleWindow bubble = bubbleDelegator.getBubble();
					if (bubble != null) {
						bubble.killBubble(true);
					}

					// open process which caused the error
					Operator op = error.getUserError().getOperator();
					final Process causingProcess = op.getProcess();
					RapidMinerGUI.getMainFrame().setOpenedProcess(causingProcess, false,
							causingProcess.getProcessLocation().toString());

					// show new error bubble in the newly opened process
					displayBubbleForUserError(error.getUserError());
				}
			}
		});
		showDetailsButton.setToolTipText(I18N.getGUIMessage("gui.action." + i18nKey + ".button_show_details.tip"));

		OperatorBubbleBuilder builder = new OperatorBubbleBuilder(RapidMinerGUI.getMainFrame(), error.getOperator(), i18nKey,
				arguments);
		final OperatorInfoBubble userErrorBubble = builder.setHideOnDisable(true).setHideOnProcessRun(true)
				.setAlignment(AlignedSide.BOTTOM).setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton()
				.setHideOnProcessRun(true).setAdditionalComponents(new JComponent[] { ackButton, showDetailsButton })
				.build();
		ackButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				userErrorBubble.killBubble(true);
			}
		});
		bubbleDelegator.setBubbleWindow(userErrorBubble);

		userErrorBubble.setVisible(true);
		return userErrorBubble;
	}

}
