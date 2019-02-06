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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.QuickFixDialog;
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
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.error.AttributeNotFoundError;
import com.rapidminer.operator.error.ParameterError;
import com.rapidminer.operator.error.ProcessExecutionUserErrorError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.quickfix.ConnectLastOperatorToOutputPortsQuickFix;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.CombinedParameterType;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.container.Pair;


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
			this.bubble = new WeakReference<>(bubble);
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
			Port port = userError.getPort();
			int errorCode = error.getCode();
			if (errorCode == 149) {
				// for "no data" errors we display an error bubble instead of a dialog
				return displayInputPortNoDataInformation(port);
			}
			if (errorCode == 156) {
				return displayInputPortWrongTypeInformation(port, userError.getExpectedType(), userError.getActualType());
			}
			return displayGenericPortError(userError);
		}
		if (error instanceof ParameterError) {
			ParameterError userError = (ParameterError) error;
			Operator op = userError.getOperator();
			String errorKey = userError.getKey();
			ParameterType param = op != null ? op.getParameterType(errorKey) : null;
			if (op != null && param != null) {
				if (userError.getClass() == UndefinedParameterError.class) {
					return displayMissingMandatoryParameterInformation(op, param);
				}
				if (userError instanceof AttributeNotFoundError) {
					return displayAttributeNotFoundParameterInformation((AttributeNotFoundError) userError);
				}
				return displayGenericParameterError(userError);
			}
		} else if (error instanceof ProcessExecutionUserErrorError) {
			ProcessExecutionUserErrorError userError = (ProcessExecutionUserErrorError) error;
			if (userError.getUserError() != null && userError.getUserError().getOperator() != null) {
				return displayUserErrorInExecutedProcess(userError);
			}
		}
		return displayGenericUserError(error);
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

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		Port firstResultPort = process.getRootOperator().getSubprocess(0).getInnerSinks().getPortByIndex(0);

		int enabledOperatorsSize = process.getRootOperator().getSubprocess(0).getEnabledOperators().size();
		Operator lastOperator = process.getRootOperator().getSubprocess(0).getEnabledOperators().get(enabledOperatorsSize - 1);

		final JCheckBox dismissForeverCheckBox = new JCheckBox(
				I18N.getGUIMessage("gui.bubble.process_unconnected_result_port.button_dismiss.label"));
		dismissForeverCheckBox
				.setToolTipText(I18N.getGUIMessage("gui.bubble.process_unconnected_result_port.button_dismiss.tip"));

		JButton button = createFixOrAckButton("process_unconnected_result_port", Collections.singletonList(
				new ConnectLastOperatorToOutputPortsQuickFix(lastOperator)), bubbleDelegator);
		button.addActionListener(e -> {

			if (dismissForeverCheckBox.isSelected()) {
				// store setting
				ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_SHOW_NO_RESULT_WARNING,
						String.valueOf(Boolean.FALSE));
				ParameterService.saveParameters();
			}
		});

		ResourceAction runAnywayAction = new ResourceAction("process_unconnected_result_port.button_run_anyway", "F11") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
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
		actionPanel.add(button, gbc);

		gbc.gridx += 1;
		actionPanel.add(runAnywayButton, gbc);

		final PortInfoBubble noResultConnectionBubble = builder.setHideOnConnection(true).setAlignment(AlignedSide.LEFT)
				.setStyle(BubbleStyle.WARNING).setHideOnProcessRun(false).setEnsureVisible(true).hideCloseButton()
				.setAdditionalComponents(new JComponent[] { actionPanel }).build();
		bubbleDelegator.setBubbleWindow(noResultConnectionBubble);

		noResultConnectionBubble.setVisible(true);
		return noResultConnectionBubble;
	}

	/**
	 * Displays a warning bubble that alerts the user that a mandatory parameter of an operator
	 * needs a different value because the provided value does not work. The bubble is located at the operator and the
	 * process view will change to said operator. This is a bubble which occurs during the check for
	 * errors before process execution so it contains a link button to run the process anyway.
	 *
	 * @param op
	 * 		the operator for which to display the warning
	 * @param parameterKey
	 * 		the parameter key for which to display the warning
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 * @since 8.2
	 */
	public static OperatorInfoBubble displayPrecheckBrokenMandatoryParameterWarning(final Operator op, final String parameterKey) {
		return displayPrecheckBrokenMandatoryParameterWarning(op, parameterKey, true);
	}

	/**
	 * DDisplays a warning bubble that alerts the user that a mandatory parameter of an operator
	 * needs a different value because the provided value does not work. The bubble is located at the operator and the
	 * process view will change to said operator. This is a bubble which occurs during the check for
	 * errors before process execution so it contains a link button to run the process anyway.
	 *
	 * @param op
	 * 		the operator for which to display the warning
	 * @param parameterKey
	 * 		the parameter key for which to display the warning
	 * @param showRunAnyway
	 * 		whether the run anyway button should be shown
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 * @since 8.2
	 */
	public static OperatorInfoBubble displayPrecheckBrokenMandatoryParameterWarning(final Operator op,
																					 final String parameterKey, boolean showRunAnyway) {
		if (op == null) {
			throw new IllegalArgumentException("op must not be null!");
		}
		if (parameterKey == null) {
			throw new IllegalArgumentException("parameterKey must not be null!");
		}

		// not the user entered name because that could be god knows how long
		String opName = op.getOperatorDescription().getName();
		return displayPrecheckMissingOrBrokenParameterWarning(op, op.getParameterType(parameterKey), false, showRunAnyway,
				"process_precheck_mandatory_parameter_broken", opName, parameterKey);
	}

	/**
	 * Displays a warning bubble that alerts the user that a mandatory parameter of an operator
	 * needs a value because it has no default value. The bubble is located at the operator and the
	 * process view will change to said operator. This is a bubble which occurs during the check for
	 * errors before process execution so it contains a link button to run the process anyway.
	 *
	 * @param op
	 *            the operator for which to display the warning
	 * @param param
	 *            the parameter for which to display the warning
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayPrecheckMissingMandatoryParameterWarning(final Operator op,
			final ParameterType param) {
		return displayPrecheckMissingMandatoryParameterWarning(op, param, true);
	}

	/**
	 * Displays a warning bubble that alerts the user that a mandatory parameter of an operator
	 * needs a value because it has no default value. The bubble is located at the operator and the
	 * process view will change to said operator. This is a bubble which occurs during the check for
	 * errors before process execution so it contains a link button to run the process anyway.
	 *
	 * @param op
	 *            the operator for which to display the warning
	 * @param param
	 *            the parameter for which to display the warning
	 * @param showRunAnyway
	 *            whether the run anyway button should be shown
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayPrecheckMissingMandatoryParameterWarning(final Operator op,
			final ParameterType param, boolean showRunAnyway) {
		if (op == null) {
			throw new IllegalArgumentException("op must not be null!");
		}
		if (param == null) {
			throw new IllegalArgumentException("param must not be null!");
		}

		// not the user entered name because that could be god knows how long
		String opName = op.getOperatorDescription().getName();
		return displayPrecheckMissingOrBrokenParameterWarning(op, param, false, showRunAnyway,
				"process_precheck_mandatory_parameter_unset", opName, param.getKey());
	}

	/**
	 * Displays a warning bubble that alerts the user that an input port of an operator expects
	 * input but is not connected. The bubble is located at the port and the process view will
	 * change to said port. This is a bubble which occurs during the check for errors before process
	 * execution so it contains a link button to run the process anyway.
	 *
	 * @param portAndError
	 *            the port and the meta data error for which to display the error
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	public static PortInfoBubble displayPrecheckInputPortDisconnectedWarning(final Pair<Port, ProcessSetupError> portAndError) {
		return displayPrecheckInputPortDisconnectedWarning(portAndError, true);
	}

	/**
	 * Displays a warning bubble that alerts the user that an input port of an operator expects
	 * input but is not connected. The bubble is located at the port and the process view will
	 * change to said port. This is a bubble which occurs during the check for errors before process
	 * execution so it contains a link button to run the process anyway if showRunAnyway is
	 * {@code true}.
	 *
	 * @param portAndError
	 *            the port and the meta data error for which to display the error
	 * @param showRunAnyway
	 *            whether the run anyway button should be shown
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	public static PortInfoBubble displayPrecheckInputPortDisconnectedWarning(final Pair<Port, ProcessSetupError> portAndError, boolean showRunAnyway) {
		if (portAndError == null) {
			throw new IllegalArgumentException("portAndError must not be null!");
		}

		// PortOwner is an interface only implemented from anonymous inner classes
		// so check enclosing class and differentiate operator input ports and subprocess
		// (result) input ports
		String key;
		if (ExecutionUnit.class.isAssignableFrom(portAndError.getFirst().getPorts().getOwner().getClass().getEnclosingClass())) {
			key = "process_precheck_mandatory_input_port_unconnected_inner";
		} else {
			key = "process_precheck_mandatory_input_port_unconnected";
		}
		return displayPrecheckMissingInputPortWarning(portAndError, true, false, showRunAnyway, key);
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
	 * @param showRunAnyway
	 *            whether the run anyway button is shown
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	private static OperatorInfoBubble displayPrecheckMissingOrBrokenParameterWarning(final Operator op, final ParameterType param,
			final boolean isError, final boolean showRunAnyway, final String i18nKey, final Object... arguments) {
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();

		List<ProcessSetupError> errorList = op.getErrorList();
		List<? extends QuickFix> quickFixes = null;
		if (!errorList.isEmpty()) {
			quickFixes = errorList.get(0).getQuickFixes();
		}
		JComponent[] additionalComponents = new JComponent[2];
		additionalComponents[0] = createFixOrAckButton(i18nKey, showRunAnyway ? quickFixes : null, bubbleDelegator);
		additionalComponents[1] = showRunAnyway ? createRunAnyWayButton(i18nKey, bubbleDelegator) : createQuickFixButton(quickFixes, bubbleDelegator);
		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(), op, param,
				"mandatory_parameter_decoration", i18nKey, arguments);

		final OperatorInfoBubble missingParameterBubble = builder.setHideOnDisable(true).setAlignment(AlignedSide.BOTTOM)
				.setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING).setEnsureVisible(true).hideCloseButton()
				.setHideOnProcessRun(true)
				.setAdditionalComponents(additionalComponents)
				.build();
		bubbleDelegator.setBubbleWindow(missingParameterBubble);

		missingParameterBubble.setVisible(true);
		return missingParameterBubble;
	}

	/**
	 * Displays a warning bubble that alerts the user that an input port of an operator expected
	 * input but that there was a problem. The bubble is located at the port and the process view
	 * will change to said port. execution.
	 *
	 * @param portAndError
	 *            the port and the meta data error for which to display the warning
	 * @param i18nKey
	 *            the i18n key which defines the title, text and button label for the bubble. Format
	 *            is "gui.bubble.{i18nKey}.title", "gui.bubble.{i18nKey}.body" and
	 *            "gui.bubble.{i18nKey}.button.label".
	 * @param hideOnConnection
	 *            if {@code true}, the bubble will be hidden once the port is connected
	 * @param isError
	 *            if {@code true}, an error bubble will be shown; otherwise a warning bubble is
	 *            displayed
	 * @param showRunAnyway
	 *            whether the run anyway button is shown
	 * @param arguments
	 *            optional i18n arguments
	 * @return the {@link PortInfoBubble} instance, never {@code null}
	 */
	private static PortInfoBubble displayPrecheckMissingInputPortWarning(final Pair<Port, ProcessSetupError> portAndError, final boolean hideOnConnection,
			final boolean isError, final boolean showRunAnyway, final String i18nKey, final Object... arguments) {
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		JComponent[] additionalComponents = new JComponent[2];
		List<? extends QuickFix> quickFixes = portAndError.getSecond().getQuickFixes();
		additionalComponents[0] = createFixOrAckButton(i18nKey, showRunAnyway ? quickFixes : null, bubbleDelegator);
		additionalComponents[1] = showRunAnyway ? createRunAnyWayButton(i18nKey, bubbleDelegator) : createQuickFixButton(quickFixes, bubbleDelegator);
		PortBubbleBuilder builder = new PortBubbleBuilder(RapidMinerGUI.getMainFrame(), portAndError.getFirst(), i18nKey, arguments);
		final PortInfoBubble missingInputBubble = builder.setHideOnConnection(hideOnConnection).setHideOnDisable(true)
				.setAlignment(AlignedSide.LEFT).setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING)
				.setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(additionalComponents)
				.build();
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
		BubbleDelegator bubbleDelegator = new BubbleDelegator();

		String decoratorKey = param instanceof CombinedParameterType || param instanceof ParameterTypeAttributes
				? "attributes_not_found_decoration" : "attribute_not_found_decoration";
		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(), op, param,
				decoratorKey, i18nKey, arguments);
		final ParameterErrorInfoBubble attributeNotFoundParameterBubble = builder.setHideOnDisable(true)
				.setAlignment(AlignedSide.BOTTOM).setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING)
				.setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] {createAckButton(i18nKey, bubbleDelegator, arguments)}).build();
		bubbleDelegator.setBubbleWindow(attributeNotFoundParameterBubble);
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

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();

		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(), op, param,
				"mandatory_parameter_decoration", i18nKey, arguments);
		final OperatorInfoBubble missingParameterBubble = builder.setHideOnDisable(true).setAlignment(AlignedSide.BOTTOM)
				.setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING).setEnsureVisible(true).hideCloseButton()
				.setHideOnProcessRun(true).setAdditionalComponents(new JComponent[] {
						createAckButton(i18nKey, bubbleDelegator),
						createQuickFixButton(Collections.singletonList(new ParameterSettingQuickFix(op, param.getKey())), bubbleDelegator)})
				.build();
		bubbleDelegator.setBubbleWindow(missingParameterBubble);

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
		BubbleDelegator bubbleDelegator = new BubbleDelegator();
		PortBubbleBuilder builder = new PortBubbleBuilder(RapidMinerGUI.getMainFrame(), port, i18nKey, arguments);
		final PortInfoBubble missingInputBubble = builder.setHideOnConnection(hideOnConnection).setHideOnDisable(true)
				.setAlignment(AlignedSide.LEFT).setStyle(isError ? BubbleStyle.ERROR : BubbleStyle.WARNING)
				.setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] {createAckButton(i18nKey, bubbleDelegator, arguments)})
				.build();
		bubbleDelegator.setBubbleWindow(missingInputBubble);
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
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(error.getMessage());

		ParameterErrorBubbleBuilder builder = new ParameterErrorBubbleBuilder(RapidMinerGUI.getMainFrame(),
				error.getOperator(), param, "generic_parameter_decoration", i18nKey, message, "");
		// if no operator or root operator, show in middle, otherwise below
		AlignedSide prefSide = error.getOperator() == null || error.getOperator() instanceof ProcessRootOperator
				? AlignedSide.MIDDLE : AlignedSide.BOTTOM;
		final OperatorInfoBubble userErrorBubble = builder.setHideOnDisable(true).setAlignment(prefSide)
				.setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] {
						createAckButton(i18nKey, bubbleDelegator),
						createShowDetailsButton(error, i18nKey, bubbleDelegator, message)})
				.build();
		bubbleDelegator.setBubbleWindow(userErrorBubble);

		if (error.getErrorName() != null && !error.getErrorName().trim().isEmpty()) {
			userErrorBubble.setHeadline(error.getErrorName());
		}
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
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(error.getMessage());

		OperatorBubbleBuilder builder = new OperatorBubbleBuilder(RapidMinerGUI.getMainFrame(), error.getOperator(), i18nKey,
				message, "");
		// if no operator, root operator or orphan, e.g. because operator is used internally by
		// another operator, show in middle, otherwise below
		AlignedSide prefSide = error.getOperator() == null || error.getOperator().getParent() == null ? AlignedSide.MIDDLE
				: AlignedSide.BOTTOM;
		final OperatorInfoBubble userErrorBubble = builder.setHideOnDisable(true).setAlignment(prefSide)
				.setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] {
						createAckButton(i18nKey, bubbleDelegator),
						createShowDetailsButton(error, i18nKey, bubbleDelegator, message)})
				.build();
		bubbleDelegator.setBubbleWindow(userErrorBubble);

		if (error.getErrorName() != null && !error.getErrorName().trim().isEmpty()) {
			userErrorBubble.setHeadline(error.getErrorName());
		}
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
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(error.getMessage());

		// input ports (located left) show the "hook" of the bubble on the left and vice versa
		AlignedSide prefSide = error.getPort() instanceof InputPort ? AlignedSide.LEFT : AlignedSide.RIGHT;
		PortBubbleBuilder builder = new PortBubbleBuilder(
				RapidMinerGUI.getMainFrame(), error.getPort(), i18nKey, message, "");
		final PortInfoBubble portErrorBubble = builder.setHideOnDisable(true).setAlignment(prefSide)
				.setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] {
						createAckButton(i18nKey, bubbleDelegator),
						createShowDetailsButton(error, i18nKey, bubbleDelegator, message)})
				.build();
		bubbleDelegator.setBubbleWindow(portErrorBubble);

		if (error.getErrorName() != null && !error.getErrorName().trim().isEmpty()) {
			portErrorBubble.setHeadline(error.getErrorName());
		}
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
		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		LinkLocalButton showDetailsButton = new LinkLocalButton(new ResourceAction(i18nKey + ".button_show_details") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				if (RapidMinerGUI.getMainFrame().close(true)) {
					// kill bubble
					BubbleWindow bubble = bubbleDelegator.getBubble();
					if (bubble != null) {
						bubble.killBubble(true);
					}

					// open process which caused the error
					Operator op = error.getUserError().getOperator();
					final Process causingProcess = op.getProcess();
					RapidMinerGUI.getMainFrame().setOpenedProcess(causingProcess);

					// show new error bubble in the newly opened process
					displayBubbleForUserError(error.getUserError());
				}
			}
		});

		OperatorBubbleBuilder builder = new OperatorBubbleBuilder(RapidMinerGUI.getMainFrame(), error.getOperator(), i18nKey,
				arguments);
		final OperatorInfoBubble userErrorBubble = builder.setHideOnDisable(true).setHideOnProcessRun(true)
				.setAlignment(AlignedSide.BOTTOM).setStyle(BubbleStyle.ERROR).setEnsureVisible(true).hideCloseButton()
				.setHideOnProcessRun(true).setAdditionalComponents(
						new JComponent[] {createAckButton(i18nKey, bubbleDelegator, arguments), showDetailsButton })
				.build();
		bubbleDelegator.setBubbleWindow(userErrorBubble);

		userErrorBubble.setVisible(true);
		return userErrorBubble;
	}

	/**
	 * Displays an information bubble pointing to an operator to indicate a
	 * {@link ProcessSetupError} for that operator.
	 *
	 * @param processSetupError
	 *            the {@link ProcessSetupError} to display
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 */
	public static OperatorInfoBubble displayProcessSetupError(final ProcessSetupError processSetupError) {
		final String i18nKey = "process_setup_error";

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(processSetupError.getMessage());

		OperatorBubbleBuilder builder = new OperatorBubbleBuilder(RapidMinerGUI.getMainFrame(),
				processSetupError.getOwner().getOperator(), i18nKey, message, "");

		BubbleStyle style = processSetupError.getSeverity() == Severity.INFORMATION ? BubbleStyle.INFORMATION
				: BubbleStyle.WARNING;
		final OperatorInfoBubble processSetupBubble = builder.setHideOnDisable(true).setAlignment(AlignedSide.BOTTOM)
				.setStyle(style).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[]{
						createAckButton(i18nKey, bubbleDelegator),
						createQuickFixButton(processSetupError.getQuickFixes(), bubbleDelegator)})
				.build();

		bubbleDelegator.setBubbleWindow(processSetupBubble);

		processSetupBubble.setVisible(true);
		return processSetupBubble;
	}

	/**
	 * Displays an information bubble pointing to an operator to indicate that the process needs to be saved for that operator settings.
	 *
	 * @param op
	 * 		the {@link Operator} that caused the error
	 * @param processSetupError
	 * 		the error that caused this bubble
	 * @return the {@link OperatorInfoBubble} instance, never {@code null}
	 * @since 8.2
	 */
	public static OperatorInfoBubble displayPrecheckProcessNotSavedWarning(final Operator op, final ProcessSetupError processSetupError) {
		final String i18nKey = "process_not_saved";

		final BubbleDelegator bubbleDelegator = new BubbleDelegator();
		final String message = removeTerminationCharacters(processSetupError.getMessage());

		List<ProcessSetupError> errorList = op.getErrorList();
		List<? extends QuickFix> quickFixes = processSetupError.getQuickFixes();
		// try to use quickfixes from given process setup error, otherwise use quickfixes from operator
		if ((quickFixes == null || quickFixes.isEmpty()) && !errorList.isEmpty()) {
			quickFixes = errorList.get(0).getQuickFixes();
		}
		OperatorBubbleBuilder builder = new OperatorBubbleBuilder(RapidMinerGUI.getMainFrame(),
				processSetupError.getOwner().getOperator(), i18nKey, message, "");

		BubbleStyle style = processSetupError.getSeverity() == Severity.INFORMATION ? BubbleStyle.INFORMATION
				: BubbleStyle.WARNING;
		final OperatorInfoBubble processNotSavedBubble = builder.setHideOnDisable(true).setAlignment(AlignedSide.BOTTOM)
				.setStyle(style).setEnsureVisible(true).hideCloseButton().setHideOnProcessRun(true)
				.setAdditionalComponents(new JComponent[] {
						createAckButton(i18nKey, bubbleDelegator),
						createQuickFixButton(quickFixes, bubbleDelegator)})
				.build();

		bubbleDelegator.setBubbleWindow(processNotSavedBubble);

		processNotSavedBubble.setVisible(true);
		return processNotSavedBubble;
	}

	/**
	 * Creates a {@link JButton} with the given i18n key and arguments to construct the label text and tool tip.
	 * Sets the action to kill the bubble provided by the {@link BubbleDelegator}.
	 *
	 * @param i18nKey
	 * 		the i18n key
	 * @param bubbleDelegator
	 * 		the bubble delegator
	 * @param arguments
	 * 		the i18n arguments
	 * @since 8.2
	 */
	private static JButton createAckButton(String i18nKey, BubbleDelegator bubbleDelegator, Object... arguments) {
		String completeKey = "gui.bubble." + i18nKey + ".button.";
		final JButton ackButton = new JButton(I18N.getGUIMessage(completeKey + "label", arguments));
		ackButton.setToolTipText(I18N.getGUIMessage(completeKey + "tip"));
		ackButton.addActionListener(e -> {
			BubbleWindow bubble = bubbleDelegator.getBubble();
			if (bubble != null) {
				bubble.killBubble(true);
			}
		});
		return ackButton;
	}

	/**
	 * Creates either the "Got it" or "Fix now" button, depending on if the {@link QuickFix} list
	 * is empty (or {@code null}) or not.
	 *
	 * @param i18nKey
	 * 		the i18n key
	 * @param quickFixes
	 * 		the quickfixes for fixing the problem, can be empty or {@code null}
	 * @param bubbleDelegator
	 * 		the bubble delegator
	 * @return the button
	 * @since 8.2
	 */
	private static JButton createFixOrAckButton(String i18nKey, List<? extends QuickFix> quickFixes, BubbleDelegator bubbleDelegator) {
		if (quickFixes == null || quickFixes.isEmpty()) {
			return createAckButton(i18nKey, bubbleDelegator);
		}
		String completeKey = "gui.bubble." + i18nKey + ".fix.button.";
		final JButton fixButton = new JButton(I18N.getGUIMessage(completeKey + "label"));
		fixButton.setToolTipText(I18N.getGUIMessage(completeKey + "tip"));
		fixButton.addActionListener(e -> showOrApplyQuickFixes(bubbleDelegator, quickFixes));
		return fixButton;
	}

	/**
	 * Creates a {@link LinkLocalButton} for the list of {@link QuickFix QuickFixes} or an empty {@link JLabel} if the given list is empty.
	 *
	 * @param quickFixes
	 * 		the list of quickfixes; can be {@code null} or empty
	 * @param bubbleDelegator
	 * 		the delegator to the {@link BubbleWindow}
	 * @return a button to show the quickfixes or an empty label
	 * @since 8.2
	 */
	private static JComponent createQuickFixButton(List<? extends QuickFix> quickFixes, BubbleDelegator bubbleDelegator) {
		if (quickFixes == null || quickFixes.isEmpty()) {
			return new JLabel();
		}
		return new LinkLocalButton(new ResourceAction("process_setup_error.button_show_quickfixes") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				// kill bubble when quick fix dialog is shown
				showOrApplyQuickFixes(bubbleDelegator, quickFixes);
			}
		});
	}

	/**
	 * Shows a dialog with the list of {@link QuickFix QuickFixes} or just applies a single {@link QuickFix}.
	 * Will also kill the bubble provided by the {@link BubbleDelegator}.
	 *
	 * @param bubbleDelegator
	 * 		the bubble delegator
	 * @param quickFixes
	 * 		the list of quick fixes; must not be non-empty
	 * @since 8.2
	 */
	private static void showOrApplyQuickFixes(BubbleDelegator bubbleDelegator, List<? extends QuickFix> quickFixes) {
		BubbleWindow bubble = bubbleDelegator.getBubble();
		if (bubble != null) {
			bubble.killBubble(true);
		}
		if (quickFixes.size() == 1) {
			quickFixes.get(0).apply();
			return;
		}
		new QuickFixDialog(quickFixes).setVisible(true);
	}

	/**
	 * Creates a {@link LinkLocalButton} for the "run anyway" action.
	 *
	 * @param i18nKey
	 * 		the i18n key
	 * @param bubbleDelegator
	 * 		the delegator to the {@link BubbleWindow}
	 * @return a button to show the "run anyway" action or an empty label
	 * @since 8.2
	 */
	private static LinkLocalButton createRunAnyWayButton(String i18nKey, BubbleDelegator bubbleDelegator) {
		ResourceAction runAnywayAction = new ResourceAction(i18nKey + ".button_run_anyway", "F11") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				BubbleWindow bubble = bubbleDelegator.getBubble();
				if (bubble != null) {
					bubble.killBubble(true);
				}
				// run process without checking for problems
				RapidMinerGUI.getMainFrame().runProcess(false);
			}
		};
		LinkLocalButton button = new LinkLocalButton(runAnywayAction);
		button.registerKeyboardAction(runAnywayAction, KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		return button;
	}

	/**
	 * Creates the "show details" link button in a {@link JPanel} for more error message details.
	 *
	 * @param error
	 * 		the error
	 * @param i18nKey
	 * 		the i18n key
	 * @param bubbleDelegator
	 * 		the bubble for which the button is
	 * @param message
	 * 		the error message
	 * @return the link button panel
	 * @since 8.2
	 */
	private static JPanel createShowDetailsButton(UserError error, String i18nKey, BubbleDelegator bubbleDelegator, String message) {
		JPanel linkPanel = new JPanel();
		LinkLocalButton button = new LinkLocalButton(new ResourceAction(i18nKey + ".button_show_details") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
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
		linkPanel.add(button);
		return linkPanel;
	}

}
