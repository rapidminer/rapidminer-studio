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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.OperatorDocToHtmlConverter;
import com.rapidminer.gui.OperatorDocumentationBrowser;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.actions.ToggleExpertModeAction;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.PlatformUtilities;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * This panel displays parameters of an operator. It refreshes in either of these cases:
 * <ul>
 * <li>A new operator is selected.</li>
 * <li>The {@link Parameters} of the current operator (which are observed) change in a way such that
 * the parameter value differs from the one displayed by the editor. This should only happen if a
 * parameter value is changed programmatically, e.g. by an operator.</li>
 * <li>{@link #processUpdated(Process)} is called and {@link #getProperties()} returns a different
 * list than the one returned during the last {@link #setupComponents()}.</li>
 * <li>When changing to expert mode.</li>
 * </ul>
 *
 * @author Simon Fischer, Tobias Malbrecht, Nils Woehler
 *
 */
public class OperatorPropertyPanel extends PropertyPanel implements Dockable, ProcessEditor {

	private static final long serialVersionUID = 6056794546696461864L;

	private static final String TAG_PARAMETERS = "parameters";
	private static final String TAG_PARAMETER = "parameter";
	private static final String ATTRIBUTE_PARAMETER_KEY = "key";

	/**
	 * {@link ExecutorService} used to execute XLST transformations of parameter help text.
	 */
	private static final ExecutorService PARAMETER_UPDATE_SERVICE = Executors.newCachedThreadPool();

	private static final Icon OK_ICON = SwingTools.createIcon("16/check.png");
	private static final Icon WARNING_ICON = SwingTools.createIcon("16/sign_warning.png");

	private static final Border TOP_BORDER = BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.PANEL_SEPARATOR);
	private static final Border BOTH_BORDERS = BorderFactory.createMatteBorder(1, 0, 1, 0, Colors.PANEL_SEPARATOR);

	private static final XMLInputFactory XML_STREAM_FACTORY = XMLInputFactory.newFactory();

	public static final String PROPERTY_EDITOR_DOCK_KEY = "property_editor";

	static {
		XML_STREAM_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, true);
	}

	private final DockKey DOCK_KEY = new ResourceDockKey(PROPERTY_EDITOR_DOCK_KEY);

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	private JPanel dockableComponent;

	private final ToggleAction showHelpAction;

	private final JLabel headerLabel = new JLabel("");

	private final Font selectedFont = headerLabel.getFont().deriveFont(Font.BOLD);

	private final Font unselectedFont = headerLabel.getFont();

	private final LinkLocalButton changeCompatibility;
	private final LinkLocalButton showAdvancedParameters;
	private final LinkLocalButton hideAdvancedParameters;

	private final Map<String, String> parameterDescriptionCache = new HashMap<>();

	private Operator operator;

	private final transient Observer<String> parameterObserver = (observable, key) -> {
		PropertyValueCellEditor editor = getEditorForKey(key);
		if (editor == null) {
			setupComponents();
			return;
		}
		Object editorValueObject = editor.getCellEditorValue();
		ParameterType type = operator.getParameters().getParameterType(key);
		String editorValue = type.toString(editorValueObject);
		String opValue = operator.getParameters().getParameterOrNull(key);
		// Second check prevents an endless validation loop in case opValue and editorValueObject are both null
		if (!Objects.equals(opValue, editorValue) && opValue != editorValueObject) {
			editor.getTableCellEditorComponent(null, opValue, false, 0, 1);
		}
	};

	final transient ToggleAction TOGGLE_EXPERT_MODE_ACTION = new ToggleExpertModeAction();

	private final CompatibilityLevelSpinnerModel compatibilityLevelSpinnerModel = new CompatibilityLevelSpinnerModel();
	private final JSpinner compatibilityLevelSpinner = new JSpinner(compatibilityLevelSpinnerModel);
	private final ResourceLabel compatibilityLabel = new ResourceLabel("compatibility_level");
	private final JPanel compatibilityPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

	public OperatorPropertyPanel(final MainFrame mainFrame) {
		super();
		headerLabel.setHorizontalAlignment(SwingConstants.LEFT);

		changeCompatibility = new LinkLocalButton(createCompatibilityAction(PlatformUtilities.getReleaseVersion()));
		showAdvancedParameters = new LinkLocalButton(new ResourceAction(true, "parameters.show_advanced") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				TOGGLE_EXPERT_MODE_ACTION.actionPerformed(null);
			}
		});
		hideAdvancedParameters = new LinkLocalButton(new ResourceAction(true, "parameters.hide_advanced") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				TOGGLE_EXPERT_MODE_ACTION.actionPerformed(null);
			}
		});

		setupComponents();

		compatibilityLevelSpinner.addChangeListener(e -> {
			// compatibility level
			OperatorVersion[] versionChanges = operator.getIncompatibleVersionChanges();

			// sort to have an ascending order
			Arrays.sort(versionChanges);
			if (versionChanges.length > 0) {
				OperatorVersion latestChange = versionChanges[versionChanges.length - 1];
				if (latestChange.isAtLeast(operator.getCompatibilityLevel())) {
					compatibilityLabel.setIcon(WARNING_ICON);
				} else {
					compatibilityLabel.setIcon(OK_ICON);
				}
			}
		});
		showHelpAction = new ToggleAction(true, "show_parameter_help") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionToggled(ActionEvent e) {
				setShowParameterHelp(isSelected());
				mainFrame.getPropertyPanel().setupComponents();
			}
		};
	}

	@Override
	public void processChanged(Process process) {}

	@Override
	public void processUpdated(Process process) {
		setNameFor(operator);
		// check if we have editors for the current parameters. If not, refresh.
		int count = 0; // count hits. If we have to many, also refresh
		List<ParameterType> properties = getProperties();
		if (properties.size() != getNumberOfEditors()) {
			setupComponents();
			return;
		}
		for (ParameterType type : properties) {
			if (hasEditorFor(type)) {
				count++;
			} else {
				setupComponents();
				return;
			}
		}
		if (count != properties.size()) {
			setupComponents();
		}
	}

	@Override
	public void setSelection(List<Operator> selection) {
		final Operator operator = selection.isEmpty() ? null : selection.get(0);
		if (operator == this.operator) {
			return;
		}
		if (this.operator != null) {
			this.operator.getParameters().removeObserver(parameterObserver);
		}
		this.operator = operator;
		if (operator != null) {
			this.operator.getParameters().addObserver(parameterObserver, true);
			if (isShowParameterHelp()) {
				PARAMETER_UPDATE_SERVICE.execute(() -> parseParameterDescriptions(operator));
			}

			// compatibility level
			OperatorVersion[] versionChanges = operator.getIncompatibleVersionChanges();
			if (versionChanges.length == 0) {
				// no incompatible versions exist
				changeCompatibility.setVisible(false);
			} else {
				compatibilityLevelSpinnerModel.setOperator(operator);
				changeCompatibility.setAction(createCompatibilityAction(operator.getCompatibilityLevel().getLongVersion()));
				changeCompatibility.setVisible(true);
			}
			compatibilityLabel.setVisible(false);
			compatibilityLevelSpinner.setVisible(false);

		}

		setNameFor(operator);
		setupComponents();
	}

	@Override
	public Component getComponent() {
		if (dockableComponent == null) {
			final JScrollPane scrollPane = new ExtendedJScrollPane(this);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.getViewport().addChangeListener(e -> {
				if (scrollPane.getVerticalScrollBar().isVisible()) {
					scrollPane.setBorder(BOTH_BORDERS);
				} else {
					scrollPane.setBorder(TOP_BORDER);
				}
			});

			dockableComponent = new JPanel(new BorderLayout());

			JPanel headerPanel = new JPanel(new BorderLayout());
			headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
			headerPanel.add(headerLabel, BorderLayout.CENTER);

			dockableComponent.add(headerPanel, BorderLayout.NORTH);
			dockableComponent.add(scrollPane, BorderLayout.CENTER);

			// compatibility level and warnings

			JPanel advancedPanel = new JPanel();
			advancedPanel.setLayout(new BoxLayout(advancedPanel, BoxLayout.PAGE_AXIS));
			advancedPanel.add(showAdvancedParameters);
			advancedPanel.add(hideAdvancedParameters);
			compatibilityLabel.setLabelFor(compatibilityLevelSpinner);
			compatibilityLevelSpinner.setPreferredSize(new Dimension(80, (int) compatibilityLevelSpinner.getPreferredSize()
					.getHeight()));

			compatibilityPanel.add(compatibilityLabel);
			compatibilityPanel.add(compatibilityLevelSpinner);
			JPanel compPanel = new JPanel();
			compPanel.setLayout(new BoxLayout(compPanel, BoxLayout.PAGE_AXIS));
			compPanel.add(changeCompatibility);
			compPanel.add(compatibilityPanel);

			JPanel combiPanel = new JPanel();
			combiPanel.setLayout(new BoxLayout(combiPanel, BoxLayout.PAGE_AXIS));
			combiPanel.add(advancedPanel);
			combiPanel.add(compPanel);
			dockableComponent.add(combiPanel, BorderLayout.SOUTH);
		}
		return dockableComponent;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	public boolean isExpertMode() {
		return TOGGLE_EXPERT_MODE_ACTION.isSelected();
	}

	public void setExpertMode(boolean isExpert) {
		TOGGLE_EXPERT_MODE_ACTION.setSelected(isExpert);
	}

	@Override
	public void setShowParameterHelp(boolean showHelp) {
		super.setShowParameterHelp(showHelp);
		showHelpAction.setSelected(showHelp);
	}

	@Override
	protected Operator getOperator() {
		return operator;
	}

	@Override
	protected String getToolTipText(String key, String title, String description, String range, boolean isOptional) {
		if (parameterDescriptionCache.containsKey(key)) {
			description = parameterDescriptionCache.get(key);
		}
		return super.getToolTipText(key, title, description, range, isOptional);
	}

	@Override
	protected String getValue(ParameterType type) {
		return operator.getParameters().getParameterOrNull(type.getKey());
	}

	@Override
	protected String getValue(Operator operator, ParameterType type) {
		operator = operator == null ? this.operator : operator;
		return operator.getParameters().getParameterOrNull(type.getKey());
	}

	@Override
	protected void setValue(Operator operator, ParameterType type, String value) {
		if (value.length() == 0) {
			value = null;
		}
		operator.setParameter(type.getKey(), value);
	}

	@Override
	protected List<ParameterType> getProperties() {
		List<ParameterType> visible = new ArrayList<>();
		boolean isExpertMode = isExpertMode();
		int advancedCount = 0;
		if (operator != null) {
			List<ParameterType> nonHidden = operator.getParameters().getParameterTypes().stream().filter(parameterType -> !parameterType.isHidden()).collect(Collectors.toList());
			List<ParameterType> nonExpert = nonHidden.stream().filter(pt -> !pt.isExpert()).collect(Collectors.toList());
			advancedCount = nonHidden.size() - nonExpert.size();
			if (isExpertMode) {
				visible = nonHidden;
			} else {
				visible = nonExpert;
			}
		}

		hideAdvancedParameters.setVisible(isExpertMode && advancedCount > 0);
		hideAdvancedParameters.setToolTipText(I18N.getGUIMessage("gui.action.parameters.hide_advanced.tip", advancedCount));
		showAdvancedParameters.setVisible(!isExpertMode);
		showAdvancedParameters.setToolTipText(I18N.getGUIMessage("gui.action.parameters.show_advanced.tip", advancedCount));
		return visible;
	}

	/**
	 * Starts a progress thread which parses the parameter descriptions for the provided operator ,
	 * cleans the {@link #parameterDescriptionCache}, and stores parsed descriptions in the
	 * {@link #parameterDescriptionCache}.
	 */
	private void parseParameterDescriptions(final Operator operator) {
		parameterDescriptionCache.clear();
		URL documentationURL = OperatorDocumentationBrowser.getDocResourcePath(operator);
		if (documentationURL != null) {
			try (InputStream documentationStream = documentationURL.openStream()) {
				XMLStreamReader reader = XML_STREAM_FACTORY.createXMLStreamReader(documentationStream);
				String parameterKey = null;

				// The builder that stores the parameter description text
				StringBuilder parameterTextBuilder = null;
				boolean inParameters = false;
				while (reader.hasNext()) {
					switch (reader.next()) {
						case XMLStreamReader.START_ELEMENT:
							if (!inParameters && reader.getLocalName().equals(TAG_PARAMETERS)) {
								inParameters = true;
							} else {
								AttributesImpl attributes = new AttributesImpl();
								for (int i = 0; i < reader.getAttributeCount(); i++) {
									attributes.addAttribute("", reader.getAttributeLocalName(i), reader.getAttributeName(i)
											.toString(), reader.getAttributeType(i), reader.getAttributeValue(i));
								}

								// Check if no parameter was found
								if (reader.getLocalName().equals(TAG_PARAMETER)) {
									parameterKey = attributes.getValue(ATTRIBUTE_PARAMETER_KEY);

									// In case a parameter key was found, create a new string
									// builder
									if (parameterKey != null) {
										parameterTextBuilder = new StringBuilder();
									}
								}

								if (parameterTextBuilder != null) {
									appendParameterStartTag(reader.getLocalName(), attributes, parameterTextBuilder);
								}
							}
							break;
						case XMLStreamReader.END_ELEMENT:
							// end parsing when end of parameters element is reached
							if (reader.getLocalName().equals(TAG_PARAMETERS)) {
								return;
							}

							if (parameterTextBuilder != null) {

								// otherwise add element to description text
								parameterTextBuilder.append("</");
								parameterTextBuilder.append(reader.getLocalName());
								parameterTextBuilder.append(">");

								// Store description when parameter element ends
								if (reader.getLocalName().equals(TAG_PARAMETER)) {
									final String parameterDescription = parameterTextBuilder.toString();
									if (!parameterDescriptionCache.containsKey(parameterKey)) {
										Source xmlSource = new StreamSource(new StringReader(parameterDescription));
										try {
											String desc = OperatorDocToHtmlConverter.applyXSLTTransformation(xmlSource);
											parameterDescriptionCache.put(parameterKey, StringEscapeUtils.unescapeHtml(desc));
										} catch (TransformerException e) {
											// ignore
										}
									}
								}
							}
							break;
						case XMLStreamReader.CHARACTERS:
							if (parameterTextBuilder != null) {
								parameterTextBuilder.append(StringEscapeUtils.escapeHtml(reader.getText()));
							}
							break;
						default:
							// ignore other events
							break;
					}
				}
			} catch (IOException | XMLStreamException e) {
				// ignore
			}
		}
	}

	private void appendParameterStartTag(String localName, Attributes attributes, StringBuilder parameterTextBuilder) {
		parameterTextBuilder.append('<');
		parameterTextBuilder.append(localName);
		for (int i = 0; i < attributes.getLength(); i++) {
			parameterTextBuilder.append(' ');
			parameterTextBuilder.append(attributes.getLocalName(i));
			parameterTextBuilder.append("=\"");
			parameterTextBuilder.append(attributes.getValue(i));
			parameterTextBuilder.append('"');

		}
		parameterTextBuilder.append(" >");
	}

	private void setNameFor(Operator operator) {
		if (operator != null) {
			headerLabel.setFont(selectedFont);
			if (operator.getName().equals(operator.getOperatorDescription().getName())) {
				headerLabel.setText(operator.getName());
			} else {
				headerLabel.setText(operator.getName() + " (" + operator.getOperatorDescription().getName() + ")");
			}
			headerLabel.setIcon(operator.getOperatorDescription().getSmallIcon());

		} else {
			headerLabel.setFont(unselectedFont);
			headerLabel.setText("Select an operator to configure it.");
			headerLabel.setIcon(null);
		}
	}

	/**
	 * Creates the action to change compatibility mode.
	 *
	 * @param version
	 * @return
	 */
	private ResourceAction createCompatibilityAction(String version) {
		String key = "parameters.change_compatibility_current";

		// different action if old comp level
		if (operator != null) {
			OperatorVersion[] versionChanges = operator.getIncompatibleVersionChanges();
			Arrays.sort(versionChanges);
			if (versionChanges.length > 0) {
				OperatorVersion latestChange = versionChanges[versionChanges.length - 1];
				if (latestChange.isAtLeast(operator.getCompatibilityLevel())) {
					key = "parameters.change_compatibility_old";
				}
			}
		}

		return new ResourceAction(true, key, version) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				compatibilityLevelSpinner.setVisible(true);
				compatibilityLabel.setVisible(true);
				changeCompatibility.setVisible(false);
			}
		};
	}
}
