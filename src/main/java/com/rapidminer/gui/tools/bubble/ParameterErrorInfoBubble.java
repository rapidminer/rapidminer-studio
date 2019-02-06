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
package com.rapidminer.gui.tools.bubble;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.PropertyPanel.PropertyEditorDecorator;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * This class creates a speech bubble-shaped JDialog, which can be attached to an {@link Operator}.
 * See {@link OperatorInfoBubble} for more details. If the missing parameter is inserted, this
 * bubble vanishes.
 * <p>
 * If the perspective is incorrect, the dockable not shown or the subprocess currently viewed is
 * wrong, automatically corrects everything to ensure the bubble is shown if the
 * {@code ensureVisibility} parameter is set.
 * </p>
 *
 * @author Nils Woehler
 * @since 6.5.0
 *
 */
public class ParameterErrorInfoBubble extends OperatorInfoBubble {

	private static final Icon WARNING_ICON = SwingTools.createIcon("16/sign_warning.png");
	private static final Icon ERROR_ICON = SwingTools.createIcon("16/error.png");

	/**
	 * Builder for {@link ParameterErrorInfoBubble}s. After calling all relevant setters, call
	 * {@link #build()} to create the actual dialog instance.
	 *
	 * @author Nils Woehler
	 * @since 6.5.0
	 *
	 */
	public static class ParameterErrorBubbleBuilder
			extends BubbleWindowBuilder<ParameterErrorInfoBubble, ParameterErrorBubbleBuilder> {

		private final Operator attachTo;
		private final ParameterType parameter;
		private final String decorateI18N;
		private boolean hideOnDisable;
		private boolean hideOnRun;
		private boolean ensureVisible;

		public ParameterErrorBubbleBuilder(final Window owner, final Operator attachTo, final ParameterType parameter,
				final String decorateI18N, final String i18nKey, final Object... arguments) {
			super(owner, i18nKey, arguments);
			this.attachTo = attachTo;
			this.parameter = parameter;
			this.decorateI18N = decorateI18N;
		}

		/**
		 * Sets whether to hide the bubble when the operator is disabled. Defaults to {@code false}.
		 *
		 * @param hideOnDisable
		 *            {@code true} if the bubble should be hidden upon disable; {@code false}
		 *            otherwise
		 * @return the builder instance
		 */
		public ParameterErrorBubbleBuilder setHideOnDisable(final boolean hideOnDisable) {
			this.hideOnDisable = hideOnDisable;
			return this;
		}

		/**
		 * Sets whether to hide the bubble when the process is run. Defaults to {@code false}.
		 *
		 * @param hideOnRun
		 *            {@code true} if the bubble should be hidden upon running a process;
		 *            {@code false} otherwise
		 * @return the builder instance
		 */
		public ParameterErrorBubbleBuilder setHideOnProcessRun(final boolean hideOnRun) {
			this.hideOnRun = hideOnRun;
			return this;
		}

		/**
		 * Sets whether to make sure the bubble is visible by automatically switching perspective,
		 * opening/showing the process dockable and changing the subprocess. Defaults to
		 * {@code false}.
		 *
		 * @param ensureVisible
		 *            {@code true} if the bubble should be hidden upon disable; {@code false}
		 *            otherwise
		 * @return the builder instance
		 */
		public ParameterErrorBubbleBuilder setEnsureVisible(final boolean ensureVisible) {
			this.ensureVisible = ensureVisible;
			return this;
		}

		@Override
		public ParameterErrorInfoBubble build() {
			return new ParameterErrorInfoBubble(owner, style, alignment, i18nKey, attachTo, parameter, decorateI18N,
					componentsToAdd, hideOnDisable, hideOnRun, ensureVisible, moveable, showCloseButton, arguments);
		}

		@Override
		public ParameterErrorBubbleBuilder getThis() {
			return this;
		}

	}

	private static final long serialVersionUID = 1L;

	private final String decorateI18N;
	private final PropertyEditorDecorator decorator;
	private final ParameterType parameter;
	private Observer<String> parameterObserver;

	/**
	 * Creates a MissingParameterOperatorInfoBubble which points to an {@link Operator}.
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param toAttach
	 *            the operator the bubble should be attached to
	 * @param parameter
	 *            the parameter with the missing value
	 * @param style
	 *            the bubble style
	 * @param decorateI18N
	 *            the I18N key used for the parameter panel decorator
	 * @param componentsToAdd
	 *            array of JComponents which will be added to the Bubble or {@code null}
	 * @param hideOnDisable
	 *            if {@code true}, the bubble will be removed once the operator becomes disabled
	 * @param hideOnRun
	 *            if {@code true}, the bubble will be removed once the process is executed
	 * @param ensureVisible
	 *            if {@code true}, will automatically make sure the bubble will be visible by
	 *            manipulating the GUI
	 * @param moveable
	 *            if {@code true} the user can drag the bubble around on screen
	 * @param showCloseButton
	 *            if {@code true} the user can close the bubble via an "x" button in the top right
	 *            corner
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	private ParameterErrorInfoBubble(Window owner, BubbleStyle style, AlignedSide preferredAlignment, String i18nKey,
			Operator toAttach, final ParameterType parameter, String decorateI18N, JComponent[] componentsToAdd,
			boolean hideOnDisable, boolean hideOnRun, boolean ensureVisible, boolean moveable, boolean showCloseButton,
			Object... arguments) {
		super(owner, style, preferredAlignment, i18nKey, toAttach, componentsToAdd, hideOnDisable, hideOnRun, ensureVisible,
				moveable, showCloseButton, true, arguments);
		if (parameter == null) {
			throw new IllegalArgumentException("parameter must not be null");
		}
		if (decorateI18N == null) {
			throw new IllegalArgumentException("decorator I18N key must not be null");
		}
		this.parameter = parameter;
		this.decorateI18N = decorateI18N;

		// Decorator to highlight parameter in operator property panel
		this.decorator = new PropertyEditorDecorator() {

			@Override
			public JPanel decorate(JPanel parameterEditor, ParameterType type, Operator typesOperator) {
				if (typesOperator.equals(getOperator()) && type.getKey().equals(parameter.getKey())) {
					return decorateParameterPanel(parameterEditor);
				}
				return parameterEditor;
			}
		};

	}

	@Override
	protected void registerSpecificListener() {
		super.registerSpecificListener();

		// Parameter observer that checks whether the missing parameter value has been added
		this.parameterObserver = new Observer<String>() {

			@Override
			public void update(Observable<String> observable, String key) {
				try {
					if (parameter != null && parameter.getKey().equals(key)) {
						// try to fetch parameter value
						String value = getOperator().getParameter(key);

						// check if value is empty
						if (value != null && !value.trim().isEmpty()) {
							// kill bubble in case the parameter is not missing anymore
							killBubble(true);
						}
					}
				} catch (UndefinedParameterError e) {
					// parameter is still missing
				}
			}

		};
		getOperator().getParameters().addObserver(parameterObserver, false);
	}

	@Override
	protected void unregisterSpecificListeners() {
		super.unregisterSpecificListeners();

		// remove parameter highlighter again
		RapidMinerGUI.getMainFrame().getPropertyPanel().removePropertyEditorDecorator(decorator);
		RapidMinerGUI.getMainFrame().getPropertyPanel().setupComponents();

		// remove parameter observer
		getOperator().getParameters().removeObserver(parameterObserver);
	}

	/**
	 * Decorates the given parameter editor panel with a "mandatory parameter" hint.
	 *
	 * @param parameterEditor
	 *            the parameter editor panel
	 * @return the decorated panel
	 */
	private JPanel decorateParameterPanel(final JPanel parameterEditor) {
		JPanel parentPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		parentPanel.add(parameterEditor, gbc);

		JLabel warningLabel = new JLabel(I18N.getGUIMessage("gui.bubble." + decorateI18N + ".label"));
		boolean isError = getStyle() == BubbleStyle.ERROR;
		warningLabel.setIcon(isError ? ERROR_ICON : WARNING_ICON);
		warningLabel.setBackground(isError ? BubbleStyle.ERROR.getColor() : BubbleStyle.WARNING.getColor());
		warningLabel.setOpaque(true);
		warningLabel.setBorder(new EmptyBorder(1, 1, 1, 0));
		gbc.gridy += 1;
		gbc.weighty = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(2, 0, 0, 0);
		parentPanel.add(warningLabel, gbc);

		return parentPanel;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			// only add decorator if not yet visible
			if (!isVisible()) {
				RapidMinerGUI.getMainFrame().getPropertyPanel().addPropertyEditorDecorator(decorator);
				RapidMinerGUI.getMainFrame().getPropertyPanel().setupComponents();
			}
		} else {
			RapidMinerGUI.getMainFrame().getPropertyPanel().removePropertyEditorDecorator(decorator);
			RapidMinerGUI.getMainFrame().getPropertyPanel().setupComponents();
		}
		super.setVisible(visible);
	}
}
