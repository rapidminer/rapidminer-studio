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
package com.rapidminer.gui.properties;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.properties.celleditors.value.AttributeFileValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.AttributeOrderingCellEditor;
import com.rapidminer.gui.properties.celleditors.value.AttributeValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.AttributesValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ConnectionLocationValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ColorValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ConfigurableValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ConfigurationWizardValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.CronExpressionCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DateFormatValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DateValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.DefaultPropertyValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.EnumerationValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ExpressionValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.FilterValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.InnerOperatorValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.LinkButtonValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ListValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.MatrixValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.OAuthValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.OperatorValueValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ParameterTupelCellEditor;
import com.rapidminer.gui.properties.celleditors.value.PreviewValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.ProcessLocationValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.RegexpValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.RemoteFileValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.RepositoryLocationValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.SimpleFileValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.SimpleSuggestionBoxValueCellEditor;
import com.rapidminer.gui.properties.celleditors.value.TextValueCellEditor;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeAttributeFile;
import com.rapidminer.parameter.ParameterTypeAttributeOrderingRules;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeConnectionLocation;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeChar;
import com.rapidminer.parameter.ParameterTypeColor;
import com.rapidminer.parameter.ParameterTypeConfiguration;
import com.rapidminer.parameter.ParameterTypeCronExpression;
import com.rapidminer.parameter.ParameterTypeDate;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeFilter;
import com.rapidminer.parameter.ParameterTypeInnerOperator;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeLinkButton;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeMatrix;
import com.rapidminer.parameter.ParameterTypeOAuth;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypePreview;
import com.rapidminer.parameter.ParameterTypeProcessLocation;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeRemoteFile;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.ParameterTypeSuggestion;
import com.rapidminer.parameter.ParameterTypeText;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.ParameterTypeValue;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.config.ParameterTypeConfigurable;


/**
 * @author Simon Fischer
 *
 */
public abstract class PropertyPanel extends JPanel {

	/**
	 * Can be used to decorate the parameter editor panels. See
	 * {@link PropertyPanel#addPropertyEditorDecorator(PropertyEditorDecorator)}.
	 *
	 * @since 6.3.0
	 */
	public static interface PropertyEditorDecorator {

		/**
		 * Called once each time the operator property panel is created.
		 *
		 * @param parameterEditor
		 *            the original parameter editor panel
		 * @param type
		 *            the parameter for which this editor panel is
		 * @param typesOperator
		 *            the operator for which this editor panel is
		 * @return this panel will be used as the parameter editor panel
		 */
		public JPanel decorate(JPanel parameterEditor, ParameterType type, Operator typesOperator);
	}

	private final List<PropertyEditorDecorator> editorDecorators = new ArrayList<>();

	/**
	 * Add the given decorator.
	 *
	 * @param d
	 *            the decorator; must not be {@code null}
	 */
	public void addPropertyEditorDecorator(PropertyEditorDecorator d) {
		if (d == null) {
			throw new IllegalArgumentException("d must not be null!");
		}
		editorDecorators.add(d);
	}

	/**
	 * Remove the given decorator. If the decorator is already removed, does nothing.
	 *
	 * @param d
	 *            the decorator; must not be {@code null}
	 */
	public void removePropertyEditorDecorator(PropertyEditorDecorator d) {
		if (d == null) {
			throw new IllegalArgumentException("d must not be null!");
		}
		editorDecorators.remove(d);
	}

	private static final long serialVersionUID = -3478661102690417293L;

	private final GridBagLayout layout = new GridBagLayout();

	/** Maps parameter type keys to currently displayed editors. */
	private final Map<String, PropertyValueCellEditor> currentEditors = new LinkedHashMap<>();

	private boolean showHelpButtons = true;

	public static final int VALUE_CELL_EDITOR_HEIGHT = 32;

	/** Color for the lines separating the entries */
	private static final Color SEPARATION_LINE_COLOR = Colors.PANEL_SEPARATOR;

	/** Initial size of the tooltip string builder */
	private static final int TOOLTIP_INITIAL_SIZE = 512;

	/** Closing tags that would stop the Swing HTML parser */
	private static final Pattern CLOSING_TAGS = Pattern.compile("</(body|html)>", Pattern.CASE_INSENSITIVE);

	private static final Border PANEL_BORDER = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, SEPARATION_LINE_COLOR), BorderFactory.createEmptyBorder(0, 0, 3, 0));

	private static Map<Class<? extends ParameterType>, Class<? extends PropertyValueCellEditor>> knownValueEditors = new HashMap<>();

	static {
		registerPropertyValueCellEditor(ParameterTypePassword.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeConfiguration.class, ConfigurationWizardValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypePreview.class, PreviewValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeColor.class, ColorValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeCategory.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeStringCategory.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeBoolean.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeChar.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeInt.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDouble.class, DefaultPropertyValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttributeFile.class, AttributeFileValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeFile.class, SimpleFileValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeRepositoryLocation.class, RepositoryLocationValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeProcessLocation.class, ProcessLocationValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeConnectionLocation.class, ConnectionLocationValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeValue.class, OperatorValueValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeInnerOperator.class, InnerOperatorValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeList.class, ListValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeMatrix.class, MatrixValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeExpression.class, ExpressionValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeText.class, TextValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttribute.class, AttributeValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeTupel.class, ParameterTupelCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeRegexp.class, RegexpValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttributes.class, AttributesValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeEnumeration.class, EnumerationValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDateFormat.class, DateFormatValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeConfigurable.class, ConfigurableValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeAttributeOrderingRules.class, AttributeOrderingCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeCronExpression.class, CronExpressionCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeDate.class, DateValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeFilter.class, FilterValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeSuggestion.class, SimpleSuggestionBoxValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeOAuth.class, OAuthValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeRemoteFile.class, RemoteFileValueCellEditor.class);
		registerPropertyValueCellEditor(ParameterTypeLinkButton.class, LinkButtonValueCellEditor.class);
	}

	/**
	 * This method allows extensions to register own ParameterTypes and their editors. Please keep
	 * in mind, that this method has to be called before any operator creation! That means, it has
	 * to be performed during init of the extension. This method will register this value cell
	 * editor as well in the PropertyTable.
	 *
	 * @param typeClass
	 *            The class of the new ParameterType for which the editor should be used
	 * @param editor
	 *            The class of the PropertyValueCellEditor
	 */
	public static void registerPropertyValueCellEditor(Class<? extends ParameterType> typeClass,
			Class<? extends PropertyValueCellEditor> editor) {
		knownValueEditors.put(typeClass, editor);
		PropertyTable.registerPropertyValueCellEditor(typeClass, editor);
	}

	private PropertyValueCellEditor instantiateValueCellEditor(final ParameterType type) {
		return instantiateValueCellEditor(type, getOperator());
	}

	public PropertyPanel() {
		setLayout(layout);
	}

	public void setupComponents() {
		SwingTools.invokeLater(this::setupComponentsNow);
	}

	public void fireEditingStoppedEvent() {
		Map<String, PropertyValueCellEditor> currentEditors = new LinkedHashMap<>(this.currentEditors);
		if (currentEditors.size() > 0) {
			for (String key : currentEditors.keySet()) {
				currentEditors.get(key).stopCellEditing();
			}
		}
	}

	private void setupComponentsNow() {
		removeAll();
		currentEditors.clear();

		/** Types currently displayed by editors. */
		Collection<ParameterType> currentTypes = getProperties();
		if (currentTypes == null) {
			revalidate();
			repaint();
			return;
		}
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4, 4, 4, 4);

		int row = 0;
		for (final ParameterType type : currentTypes) {
			final PropertyValueCellEditor editor = instantiateValueCellEditor(type);
			currentEditors.put(type.getKey(), editor);
			Object value;
			value = getValue(type);
			if (value == null) {
				value = type.getDefaultValue();
			}

			Component editorComponent = editor.getTableCellEditorComponent(null, value, false, row, 1);
			if (!isEnabled()) {
				SwingTools.setEnabledRecursive(editorComponent, false);
			}

			final Operator typesOperator = getOperator();
			editor.addCellEditorListener(new CellEditorListener() {

				@Override
				public void editingCanceled(ChangeEvent e) {}

				@Override
				public void editingStopped(ChangeEvent e) {
					Object valueObj = editor.getCellEditorValue();
					String value = type.toString(valueObj);
					String last = getValue(typesOperator, type);
					// Second check prevents an endless validation loop in case valueObj and last are both null
					if (!Objects.equals(value, last) && valueObj != last) {
						setValue(typesOperator, type, value, false);
					}
				}
			});

			c.gridx = 0;
			c.gridy = row;
			c.weightx = 1;
			c.weighty = 0;

			add(createParameterPanel(type, editor, editorComponent), c);
			row++;
		}

		// label for no parameters case
		if (row == 0) {
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets(10, 10, 10, 10);
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			JLabel noParametersLabel = new ResourceLabel("propertyPanel.no_parameters");
			noParametersLabel.setHorizontalAlignment(SwingConstants.CENTER);
			add(noParametersLabel, c);
			row++;
		}

		c.gridx = 0;
		c.gridy = row;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;

		// Push panel contents to top
		JLabel dummyLabel = new JLabel();
		dummyLabel.setOpaque(false);
		layout.setConstraints(dummyLabel, c);
		add(dummyLabel);

		revalidate();
		repaint();
	}

	/**
	 * Creates a JPanel for a ParameterType.
	 *
	 * @param type
	 *            The ParameterType, for which the panel is created
	 * @param editor
	 *            Editor for the current ParameterType
	 * @param editorComponent
	 *            Editor component for the current ParameterType
	 *
	 * @return JPanel for the given ParameterType
	 */
	protected JPanel createParameterPanel(ParameterType type, PropertyValueCellEditor editor, Component editorComponent) {

		JPanel parameterPanel = null;
		if (editor.rendersLabel()) {
			parameterPanel = new JPanel(new BorderLayout());
			parameterPanel.setOpaque(isOpaque());
			parameterPanel.setBackground(getBackground());
			parameterPanel.setPreferredSize(
					new Dimension((int) parameterPanel.getPreferredSize().getWidth(), VALUE_CELL_EDITOR_HEIGHT));
			parameterPanel.add(editorComponent,
					editorComponent instanceof JCheckBox ? BorderLayout.WEST : BorderLayout.CENTER);
		} else {
			parameterPanel = new JPanel(new GridLayout(1, 2));
			parameterPanel.setOpaque(isOpaque());
			parameterPanel.setBackground(getBackground());
			parameterPanel.setPreferredSize(
					new Dimension((int) parameterPanel.getPreferredSize().getWidth(), VALUE_CELL_EDITOR_HEIGHT));
			final JLabel label = new JLabel(type.getKey().replace('_', ' ') + " ");
			label.setOpaque(isOpaque());
			label.setFont(getFont());
			label.setBackground(getBackground());
			int style = Font.PLAIN;
			if (!type.isOptional()) {
				style |= Font.BOLD;
			}
			if (type.isExpert()) {
				style |= Font.ITALIC;
			}
			label.setFont(label.getFont().deriveFont(style));
			label.setLabelFor(editorComponent);
			if (!isEnabled()) {
				SwingTools.setEnabledRecursive(label, false);
			}

			parameterPanel.add(label);
			parameterPanel.add(editorComponent);
		}

		for (PropertyEditorDecorator decorator : editorDecorators) {
			parameterPanel = decorator.decorate(parameterPanel, type, getOperator());
		}

		JPanel surroundingPanel = new JPanel(new BorderLayout());
		surroundingPanel.add(parameterPanel, BorderLayout.CENTER);
		surroundingPanel.setBorder(PANEL_BORDER);

		if (showHelpButtons) {
			addHelpLabel(type.getKey(), type.getKey().replace("_", " "), type.getDescription(), type.getRange(),
					type.isOptional(), surroundingPanel);
		}

		return surroundingPanel;
	}

	/**
	 * Adds a help icon to the provided JPanel which shows a tooltip when hovering over it.
	 *
	 * @param key
	 *            the key of the parameter
	 * @param title
	 *            the tooltip title
	 * @param description
	 *            the tooltip description
	 * @param range
	 *            the parameter range. Can be <code>null</code> in case the parameter does not have
	 *            a range
	 * @param isOptional
	 *            whether the parameter is optional
	 * @param labelPanel
	 *            the panel which will be used to add the label. The panel needs to have a
	 *            {@link BorderLayout} as layout manager as the label will be added with the
	 *            constraint {@link BorderLayout#EAST}.
	 */
	protected final void addHelpLabel(final String key, final String title, final String description, final String range,
			final boolean isOptional, JPanel labelPanel) {
		// cannot just call {@link SwingTools#addTooltipHelpIconToLabel} since {@link
		// #getToolTipText} must be called in the TipProvider because of the caching in {@link
		// OperatorPropertyPanel#getToolTipText}
		final JLabel helpLabel = SwingTools.initializeHelpLabel(labelPanel);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				TipProvider tipProvider = new TipProvider() {

					@Override
					public String getTip(Object id) {
						if (id == null) {
							return null;
						} else {
							return getToolTipText(key, title, description, range, isOptional);
						}
					}

					@Override
					public Object getIdUnder(Point point) {
						return helpLabel;
					}

					@Override
					public Component getCustomComponent(Object id) {
						return null;
					}

				};
				SwingTools.setupTooltip(tipProvider, getDialogOwner(), helpLabel);
			}
		});
	}

	protected boolean hasEditorFor(ParameterType type) {
		return currentEditors.containsKey(type.getKey());
	}

	protected int getNumberOfEditors() {
		return currentEditors.size();
	}

	/**
	 * Returns the editor for the given parameter key.
	 *
	 * @param key
	 * 		the key
	 * @return the editor or {@code null} if there is no editor for the given key.
	 * @since 8.2
	 */
	public PropertyValueCellEditor getEditorForKey(String key) {
		return currentEditors.get(key);
	}

	protected abstract String getValue(ParameterType type);

	/**
	 * Returns the parameter value for the given operator.
	 *
	 * <p>The default implementation ignores the operator parameter and behaves like {@link
	 * #getValue(ParameterType)}</p>
	 *
	 * @param operator
	 * 		the operator
	 * @param type
	 * 		the parameter of the operator
	 * @return the value for the given operator
	 * @see #getValue(ParameterType)
	 * @since 9.4
	 */
	protected String getValue(Operator operator, ParameterType type) {
		return getValue(type);
	}

	protected abstract void setValue(Operator operator, ParameterType type, String value);

	protected abstract Collection<ParameterType> getProperties();

	protected abstract Operator getOperator();

	/**
	 * @return the dialog owner (if the {@link PropertyPanel}) has one
	 */
	protected JDialog getDialogOwner() {
		return null;
	}

	/**
	 * Subclasses of PropertyPanel (e.g. GenericParameterPanel) can overwrite this method in order
	 * to specify if GUI elements should be updated after setting the Value.
	 **/
	protected void setValue(Operator operator, ParameterType type, String value, boolean updateComponents) {
		setValue(operator, type, value);
	}

	public static PropertyValueCellEditor instantiateValueCellEditor(final ParameterType type, Operator operator) {
		PropertyValueCellEditor editor;
		Class<?> typeClass = type.getClass();
		do {
			Class<? extends PropertyValueCellEditor> editorClass = knownValueEditors.get(typeClass);
			if (editorClass != null) {
				try {
					Constructor<? extends PropertyValueCellEditor> constructor = editorClass
							.getConstructor(typeClass);
					editor = constructor.newInstance(type);
				} catch (Exception e) {
					LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.gui.properties.PropertyPanel.construct_property_editor_error", e), e);

					editor = new DefaultPropertyValueCellEditor(type);
				}
				break;
			} else {
				typeClass = typeClass.getSuperclass();
				editor = new DefaultPropertyValueCellEditor(type);
			}
		} while (typeClass != null);
		editor.setOperator(operator);
		return editor;
	}

	/**
	 * Creates a text for the {@link ToolTipWindow}.
	 */
	protected String getToolTipText(String key, String title, String description, String range, boolean isOptional) {

		// Title
		StringBuilder sb = new StringBuilder(TOOLTIP_INITIAL_SIZE);
		sb.append("<h3 style=\"padding-bottom:4px\">").append(title).append("</h3>");

		// Description
		if (description != null && !description.isEmpty()) {
			sb.append("<p style=\"padding-bottom:4px\">");
			sb.append("<b>").append(I18N.getGUIMessage("gui.dialog.settings.description")).append("</b>: ");
			// prevent the Swing HTML parser from stopping here
			sb.append(CLOSING_TAGS.matcher(description).replaceAll(""));
			sb.append("</p>");
		}

		// Range
		if (range != null && !range.isEmpty()) {
			sb.append("<p style=\"padding-bottom:4px\">");
			sb.append("<b>").append(I18N.getGUIMessage("gui.dialog.settings.range")).append("</b>: ");
			sb.append(range);
			sb.append("</p>");
		}

		// Optional/required
		sb.append("<p style=\"padding-bottom:4px\">");
		sb.append("<b>").append(I18N.getGUIMessage("gui.dialog.settings.optional")).append("</b>: ");
		if (isOptional) {
			sb.append(I18N.getGUIMessage("gui.dialog.settings.true"));
		} else {
			sb.append(I18N.getGUIMessage("gui.dialog.settings.false"));
		}
		sb.append("</p>");

		return sb.toString();
	}

	/**
	 * @param showHelp
	 *            defines whether the parameter help icon should be shown
	 */
	public void setShowParameterHelp(boolean showHelp) {
		this.showHelpButtons = showHelp;
	}

	/**
	 * @return returns <code>true</code> in case the parameter help icon should be shown
	 */
	public boolean isShowParameterHelp() {
		return showHelpButtons;
	}
}
