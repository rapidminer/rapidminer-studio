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
package com.rapidminer.tutorial.gui;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.studio.internal.NoStartupDialogRegistreredException;
import com.rapidminer.studio.internal.StartupDialogProvider.ToolbarButton;
import com.rapidminer.studio.internal.StartupDialogRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;
import com.rapidminer.tutorial.Tutorial;
import com.rapidminer.tutorial.TutorialManager;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.RelativeDockablePosition;


/**
 * A browser for the {@link Tutorial} steps file, see {@link Tutorial#getSteps()} .
 *
 * @since 7.0.0
 * @author Marcel Michel
 */
public class TutorialBrowser extends JPanel implements Dockable {

	private static final long serialVersionUID = 1L;

	public static final String TUTORIAL_BROWSER_DOCK_KEY = "tutorial_browser";
	private static final DockKey DOCK_KEY = new ResourceDockKey(TUTORIAL_BROWSER_DOCK_KEY);

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	public static final RelativeDockablePosition POSITION = new RelativeDockablePosition(0, 1, 0.15, 1);

	private static final String EXPLANATION_BACKGROUND_NAME = "tutorial/explanation_background.png";
	private static final String EXPLANATION_HEADER_NAME = "tutorial/explanation_header.png";

	private static final String CHALLENGE_BACKGROUND_NAME = "tutorial/challenge_background.png";
	private static final String CHALLENGE_HEADER_NAME = "tutorial/challenge_header.png";

	private static final String ACTIVITY_BACKGROUND_NAME = "tutorial/activity_background.png";
	private static final String ACTIVITY_HEADER_NAME = "tutorial/activity_header.png";

	private static final URL EXPLANATION_BACKGROUND_RESOURCE = Tools.getResource("tutorial/explanation_background.png");
	private static final URL EXPLANATION_HEADER_RESOURCE = Tools.getResource("tutorial/explanation_header.png");

	private static final URL CHALLENGE_BACKGROUND_RESOURCE = Tools.getResource("tutorial/challenge_background.png");
	private static final URL CHALLENGE_HEADER_RESOURCE = Tools.getResource("tutorial/challenge_header.png");

	private static final URL ACTIVITY_BACKGROUND_RESOURCE = Tools.getResource("tutorial/activity_background.png");
	private static final URL ACTIVITY_HEADER_RESOURCE = Tools.getResource("tutorial/activity_header.png");

	private static final String XSL_FILE_PATH = "tutorial/tutorial_browser.xsl";

	private static final String XML_ATTRIBUTE_BACKGROUND_IMAGE = "background-image";
	private static final String XML_ATTRIBUTE_TOTAL = "total";
	private static final String XML_ATTRIBUTE_INDEX = "index";
	private static final String XML_ATTRIBUTE_IMAGE = "image";

	private static final String XML_TAG_STEP = "step";
	private static final String XML_TAG_ICON = "icon";
	private static final String XML_TAG_TASKS = "tasks";
	private static final String XML_TAG_QUESTIONS = "questions";
	private static final String XML_TAG_INFO = "info";

	private static final String PROGRESS_THREAD_ID = "tutorial_browser.load_tutorial";

	private static final String INFO_TEMPLATE = "<html><div style=\"margin:10px;color:#555555;font-family:'Open Sans';font-size:14;text-align:center;\">%s</div></html>";
	private static final String HEADLINE_TEMPLATE = "<html><span style=\"font-family:'Open Sans';font-size: 13;color:#333333;\">%s</html>";
	private static final String ICON_TEMPLATE = "<html><div style=\"font-size: 12; color: %s;\">%s</div></html>";
	private static final String LINK_COLOR_HEX = SwingTools.getColorHexValue(Colors.LINKBUTTON_LOCAL);
	private static final String LINK_STYLE = "a {color:" + LINK_COLOR_HEX + ";font-family:'Open Sans';font-size:13;}";

	private static final String NEXT_TUTORIAL_URL = "tutorial://next";

	private static final String NO_TUTORIAL_SELECTED = I18N.getGUILabel("tutorial_browser.no_tutorial_selected");
	private static final String NO_STEPS_AVAILABLE = I18N.getGUILabel("tutorial_browser.no_steps_available");
	private static final String DISPLAY_TUTORIAL_STEPS = I18N.getGUILabel("tutorial_browser.displaying_steps");
	private static final String TRANSFORMATION_FAILURE = I18N.getGUILabel("tutorial_browser.transformation_failure");

	private JLabel tutorialNameLabel;
	private JButton previousStepButton;
	private JButton nextStepButton;
	private JEditorPane jEditorPane;
	private JScrollPane scrollPane;
	private Tutorial selectedTutorial;
	private Tutorial nextTutorial;
	private List<String> steps = new ArrayList<>();
	private int stepIndex = -1;

	private TutorialSelector tutorialSelector;
	private Observer<Tutorial> tutorialObserver;

	/**
	 * Creates a new browser which is linked to the given {@link TutorialSelector}.
	 *
	 * @param tutorialSelector
	 *            the selector which should be observed
	 */
	public TutorialBrowser(TutorialSelector tutorialSelector) {
		this.tutorialSelector = tutorialSelector;
		initGUI();
		tutorialObserver = new Observer<Tutorial>() {

			@Override
			public void update(Observable<Tutorial> observable, Tutorial tutorial) {
				selectedTutorial = tutorial;
				nextTutorial = null;

				// find next tutorial
				if (selectedTutorial != null) {
					List<Tutorial> tutorials = selectedTutorial.getGroup().getTutorials();
					int currIndex = tutorials.indexOf(selectedTutorial);
					if (currIndex != -1 && currIndex + 1 < tutorials.size()) {
						nextTutorial = tutorials.get(currIndex + 1);
					}
				}

				updateTutorial(tutorial);
			}
		};
		tutorialSelector.addObserver(tutorialObserver, true);
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	private void initGUI() {
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);
		add(createHeaderPanel(), BorderLayout.NORTH);
		add(createContentPanel(), BorderLayout.CENTER);
		add(createFooterPanel(), BorderLayout.SOUTH);
	}

	private Component createHeaderPanel() {
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		buttonPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colors.TAB_BORDER));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 0);
		gbc.gridx = 0;

		// tutorial title
		{
			tutorialNameLabel = new JLabel(" ");
			buttonPanel.add(tutorialNameLabel, gbc);
		}

		// filler
		{
			gbc.gridx += 1;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			buttonPanel.add(new JLabel(), gbc);
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 0;
		}

		// back to tutorials button
		{
			gbc.gridx += 1;
			gbc.insets = new Insets(10, 0, 10, 0);
			JLabel backToTutorialsIcon = new JLabel(String.format(ICON_TEMPLATE, LINK_COLOR_HEX, Ionicon.HOME.getHtml()));
			buttonPanel.add(backToTutorialsIcon, gbc);

			LinkLocalButton backToTutorialsButton = new LinkLocalButton(new ResourceAction("tutorials.view_all_tutorials") {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						StartupDialogRegistry.INSTANCE.showStartupDialog(ToolbarButton.TUTORIAL);
					} catch (NoStartupDialogRegistreredException e1) {
						SwingTools.showVerySimpleErrorMessage("tutorials_not_available");
					}
				}
			});
			HTMLEditorKit htmlKit = (HTMLEditorKit) backToTutorialsButton.getEditorKit();
			htmlKit.getStyleSheet().addRule(LINK_STYLE);
			gbc.gridx += 1;
			gbc.insets = new Insets(10, 0, 10, 10);
			buttonPanel.add(backToTutorialsButton, gbc);
		}
		return buttonPanel;
	}

	private Component createFooterPanel() {
		JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		previousStepButton = new JButton();
		previousStepButton.setAction(new ResourceAction("tutorial_browser.previous_step") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				displayStep(--stepIndex);
			}
		});
		previousStepButton.setEnabled(false);
		buttonPanel.add(previousStepButton);

		nextStepButton = new JButton();
		nextStepButton.setAction(new ResourceAction("tutorial_browser.next_step") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				displayStep(++stepIndex);
			}
		});
		nextStepButton.setEnabled(false);
		nextStepButton.setHorizontalTextPosition(SwingConstants.LEFT);
		buttonPanel.add(nextStepButton);

		JPanel footer = new JPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.TAB_BORDER));
		footer.add(buttonPanel, BorderLayout.CENTER);

		return footer;
	}

	private Component createContentPanel() {
		JPanel contentPanel = new JPanel();
		contentPanel.setOpaque(false);

		jEditorPane = new JEditorPane();
		jEditorPane.setEditable(false);

		scrollPane = new ExtendedJScrollPane(jEditorPane);
		scrollPane.setBorder(null);

		HTMLEditorKit kit = new HTMLEditorKit();
		jEditorPane.setEditorKit(kit);
		jEditorPane.setMargin(new Insets(0, 0, 0, 0));

		Document doc = kit.createDefaultDocument();
		jEditorPane.setDocument(doc);
		jEditorPane.setText(String.format(INFO_TEMPLATE, NO_TUTORIAL_SELECTED));
		jEditorPane.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (NEXT_TUTORIAL_URL.equals(e.getDescription())) {
						ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_GETTING_STARTED,
								"tutorial_browser", "next_tutorial");
						if (nextTutorial == null) {
							try {
								StartupDialogRegistry.INSTANCE.showStartupDialog(ToolbarButton.TUTORIAL);
							} catch (NoStartupDialogRegistreredException e1) {
								SwingTools.showVerySimpleErrorMessage("tutorials_not_available");
							}
						} else {
							try {
								MainFrame mainFrame = RapidMinerGUI.getMainFrame();
								Process tutorialProcess = nextTutorial.makeProcess();
								mainFrame.setOpenedProcess(tutorialProcess, false, null);
								TutorialManager.INSTANCE.completedTutorial(nextTutorial.getIdentifier());
								tutorialSelector.setSelectedTutorial(nextTutorial);
							} catch (RuntimeException | MalformedRepositoryLocationException | IOException
									| XMLException e1) {
								SwingTools.showSimpleErrorMessage("cannot_open_tutorial", e1, nextTutorial.getTitle(),
										e1.getMessage());
							}
						}
					} else {
						ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_GETTING_STARTED,
								"tutorial_browser", "open_remote_url");
						try {
							RMUrlHandler.browse(e.getURL().toURI());
						} catch (Exception e1) {
							SwingTools.showSimpleErrorMessage("cannot_open_browser_url", e1, e.getDescription());
						}
					}
				}
			}
		});
		return scrollPane;
	}

	private void updateTutorial(final Tutorial tutorial) {
		if (tutorial == null) {
			steps = new ArrayList<>();
			tutorialNameLabel.setText(null);
			jEditorPane.setText(String.format(INFO_TEMPLATE, NO_TUTORIAL_SELECTED));
			updateStepButtons();
		} else {
			tutorialNameLabel.setText(String.format(HEADLINE_TEMPLATE, tutorial.getTitle()));
			jEditorPane.setText(String.format(INFO_TEMPLATE, DISPLAY_TUTORIAL_STEPS));
			ProgressThread thread = new ProgressThread(PROGRESS_THREAD_ID) {

				@Override
				public void run() {
					try (InputStream stepFileStream = tutorial.getSteps()) {
						if (stepFileStream == null) {
							SwingTools.invokeLater(new Runnable() {

								@Override
								public void run() {
									displaySteps(null);
								}
							});
							return;
						}

						final List<String> steps = new ArrayList<>();
						DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
						DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
						org.w3c.dom.Document doc = dBuilder.parse(new InputSource(stepFileStream));
						doc.getDocumentElement().normalize();

						// enrich document information
						enrichNodeList(doc.getElementsByTagName(XML_TAG_ICON));
						enrichNodeList(doc.getElementsByTagName(XML_TAG_INFO));
						enrichNodeList(doc.getElementsByTagName(XML_TAG_TASKS));
						enrichNodeList(doc.getElementsByTagName(XML_TAG_QUESTIONS));

						TransformerFactory tFactory = TransformerFactory.newInstance();
						Transformer transformer = tFactory
								.newTransformer(new StreamSource(Tools.getResourceInputStream(XSL_FILE_PATH)));
						NodeList stepList = doc.getElementsByTagName(XML_TAG_STEP);
						for (int i = 0; i < stepList.getLength(); i++) {
							Node nNode = stepList.item(i);

							// enrich step node
							((Element) nNode).setAttribute(XML_ATTRIBUTE_INDEX, String.valueOf(i + 1));
							((Element) nNode).setAttribute(XML_ATTRIBUTE_TOTAL, String.valueOf(stepList.getLength()));

							DOMImplementationLS lsImpl = (DOMImplementationLS) nNode.getOwnerDocument().getImplementation()
									.getFeature("LS", "3.0");
							LSSerializer lsSerializer = lsImpl.createLSSerializer();
							lsSerializer.getDomConfig().setParameter("xml-declaration", false);

							String currentStep = lsSerializer.writeToString(nNode);
							ByteArrayOutputStream output = new ByteArrayOutputStream();
							org.w3c.dom.Document document = dBuilder.parse(new InputSource(new StringReader(currentStep)));
							transformer.transform(new DOMSource(document), new StreamResult(output));

							steps.add(output.toString(UTF_8.name()));
						}
						SwingTools.invokeLater(new Runnable() {

							@Override
							public void run() {
								displaySteps(steps);
							}
						});
					} catch (Exception e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.tutorial.gui.TutorialBrowser.failed_to_load_stepfile",
								new Object[] { tutorial.getTitle(), e });
						SwingTools.invokeLater(new Runnable() {

							@Override
							public void run() {
								jEditorPane.setText(String.format(INFO_TEMPLATE, TRANSFORMATION_FAILURE));
							}

						});
					}
				}
			};
			thread.setIndeterminate(true);
			thread.addDependency(PROGRESS_THREAD_ID);
			thread.start();
		}
	}

	private void enrichNodeList(NodeList nList) {
		for (int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			switch (nNode.getNodeName()) {
				case XML_TAG_ICON:
					((Element) nNode).setAttribute(XML_ATTRIBUTE_IMAGE, SwingTools.getIconPath(nNode.getTextContent()));
					break;
				case XML_TAG_INFO:
					((Element) nNode).setAttribute(XML_ATTRIBUTE_IMAGE,
							EXPLANATION_HEADER_RESOURCE == null ? SwingTools.getIconPath(EXPLANATION_HEADER_NAME)
									: EXPLANATION_HEADER_RESOURCE.toExternalForm());
					((Element) nNode).setAttribute(XML_ATTRIBUTE_BACKGROUND_IMAGE,
							EXPLANATION_BACKGROUND_RESOURCE == null ? SwingTools.getIconPath(EXPLANATION_BACKGROUND_NAME)
									: EXPLANATION_BACKGROUND_RESOURCE.toExternalForm());
					break;
				case XML_TAG_TASKS:
					((Element) nNode).setAttribute(XML_ATTRIBUTE_IMAGE, ACTIVITY_HEADER_RESOURCE == null
							? SwingTools.getIconPath(ACTIVITY_HEADER_NAME) : ACTIVITY_HEADER_RESOURCE.toExternalForm());
					((Element) nNode).setAttribute(XML_ATTRIBUTE_BACKGROUND_IMAGE,
							ACTIVITY_BACKGROUND_RESOURCE == null ? SwingTools.getIconPath(ACTIVITY_BACKGROUND_NAME)
									: ACTIVITY_BACKGROUND_RESOURCE.toExternalForm());
					break;
				case XML_TAG_QUESTIONS:
					((Element) nNode).setAttribute(XML_ATTRIBUTE_IMAGE, CHALLENGE_HEADER_RESOURCE == null
							? SwingTools.getIconPath(CHALLENGE_HEADER_NAME) : CHALLENGE_HEADER_RESOURCE.toExternalForm());
					((Element) nNode).setAttribute(XML_ATTRIBUTE_BACKGROUND_IMAGE,
							CHALLENGE_BACKGROUND_RESOURCE == null ? SwingTools.getIconPath(CHALLENGE_BACKGROUND_NAME)
									: CHALLENGE_BACKGROUND_RESOURCE.toExternalForm());
					break;
				default:
					break;
			}
		}
	}

	private void displaySteps(List<String> steps) {
		this.steps = steps;
		this.stepIndex = -1;
		if (steps != null && steps.size() > 0) {
			displayStep(0);
		} else {
			jEditorPane.setText(String.format(INFO_TEMPLATE, NO_STEPS_AVAILABLE));
			updateStepButtons();
		}
	}

	private void displayStep(final int index) {
		final Runnable changeStep = new Runnable() {

			@Override
			public void run() {
				jEditorPane.setText(steps.get(index));
				stepIndex = index;
				updateStepButtons();
			}
		};
		new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					SwingUtilities.invokeAndWait(changeStep);
				} catch (InvocationTargetException | InterruptedException e) {
					SwingUtilities.invokeLater(changeStep);
				}
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						scrollPane.getViewport().setViewPosition(new Point(0, 0));
					}
				});
			}

		}).start();

	}

	private void updateStepButtons() {
		if (steps == null) {
			nextStepButton.setEnabled(false);
			previousStepButton.setEnabled(false);
		} else {
			nextStepButton.setEnabled(stepIndex < steps.size() - 1);
			previousStepButton.setEnabled(stepIndex > 0);
		}
	}
}
