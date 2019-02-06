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
package com.rapidminer.tutorial.gui;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilder;
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
import com.rapidminer.gui.DockableMenu;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FeedbackForm;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.studio.internal.NoStartupDialogRegistreredException;
import com.rapidminer.studio.internal.StartupDialogProvider.ToolbarButton;
import com.rapidminer.studio.internal.StartupDialogRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
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

	private enum NextStepType {
		STEP,
		TUTORIAL,
		CHAPTER
	}

	/**
	 * Button that can change its action. It is updated by {@link #updateStepButtons()} and can show the next
	 * tutorial step if present, the next tutorial in this chapter if present or bring up the tutorial overview
	 * if the end of a chapter is reached.
	 *
	 * @author Jan Czogalla
	 * @since 8.2
	 */
	private class NextStepButton extends JButton {

		private final Action nextStepAction = new ResourceAction("tutorial_browser.next_step") {

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				activateNextStep();
			}
		};

		private final Action nextTutorialAction = new ResourceAction("tutorial_browser.next_tutorial") {

			@Override
			protected void loggedActionPerformed(ActionEvent e) {
				activateNextTutorial();
				NextStepButton.this.setEnabled(false);
			}
		};

		private final Action nextChapterAction = new ResourceAction("tutorial_browser.next_chapter") {

			@Override
			protected void loggedActionPerformed(ActionEvent e) {
				activateTutorialStartup();
			}
		};

		private NextStepButton() {
			setNextAction(NextStepType.STEP);
		}

		/** Sets the action and look of this button depending on the selected action. */
		private void setNextAction(NextStepType type) {
			Color foreGround = Color.WHITE;
			boolean highlight = true;
			switch (type) {
				case STEP:
					setAction(nextStepAction);
					foreGround = Color.BLACK;
					highlight = false;
					break;
				case TUTORIAL:
					setAction(nextTutorialAction);
					break;
				case CHAPTER:
					setAction(nextChapterAction);
					break;
			}
			putClientProperty(RapidLookTools.PROPERTY_BUTTON_HIGHLIGHT, highlight);
			setForeground(foreGround);
		}
	}

	private static final long serialVersionUID = 1L;

	public static final String TUTORIAL_BROWSER_DOCK_KEY = "tutorial_browser";
	private static final DockKey DOCK_KEY = new ResourceDockKey(TUTORIAL_BROWSER_DOCK_KEY);
	private static final String FEEDBACK_KEY_TUTORIAL = "tutorial";

	static {
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
		DockableMenu.registerHideInDockableMenuPrefix(TUTORIAL_BROWSER_DOCK_KEY);
	}

	public static final RelativeDockablePosition POSITION = new RelativeDockablePosition(0, 1, 0.15, 1);

	private static final String EXPLANATION_BACKGROUND_NAME = "tutorial/explanation_background.png";
	private static final String EXPLANATION_HEADER_NAME = "tutorial/explanation_header.png";

	private static final String CHALLENGE_BACKGROUND_NAME = "tutorial/challenge_background.png";
	private static final String CHALLENGE_HEADER_NAME = "tutorial/challenge_header.png";

	private static final String ACTIVITY_BACKGROUND_NAME = "tutorial/activity_background.png";
	private static final String ACTIVITY_HEADER_NAME = "tutorial/activity_header.png";

	private static final URL EXPLANATION_BACKGROUND_RESOURCE = Tools.getResource(EXPLANATION_BACKGROUND_NAME);
	private static final URL EXPLANATION_HEADER_RESOURCE = Tools.getResource(EXPLANATION_HEADER_NAME);

	private static final URL CHALLENGE_BACKGROUND_RESOURCE = Tools.getResource(CHALLENGE_BACKGROUND_NAME);
	private static final URL CHALLENGE_HEADER_RESOURCE = Tools.getResource(CHALLENGE_HEADER_NAME);

	private static final URL ACTIVITY_BACKGROUND_RESOURCE = Tools.getResource(ACTIVITY_BACKGROUND_NAME);
	private static final URL ACTIVITY_HEADER_RESOURCE = Tools.getResource(ACTIVITY_HEADER_NAME);

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

	private static final String NO_TUTORIAL_SELECTED = I18N.getGUILabel("tutorial_browser.no_tutorial_selected");
	private static final String NO_STEPS_AVAILABLE = I18N.getGUILabel("tutorial_browser.no_steps_available");
	private static final String DISPLAY_TUTORIAL_STEPS = I18N.getGUILabel("tutorial_browser.displaying_steps");
	private static final String TRANSFORMATION_FAILURE = I18N.getGUILabel("tutorial_browser.transformation_failure");

	private JLabel tutorialNameLabel;
	private JButton previousStepButton;
	private NextStepButton nextStepButton;
	private JEditorPane jEditorPane;
	private JScrollPane scrollPane;
	private Tutorial selectedTutorial;
	private Tutorial nextTutorial;
	private List<String> steps = new ArrayList<>();
	private int stepIndex = -1;

	private JPanel contentPanel;
	private GridBagConstraints contentGbc;
	private FeedbackForm feedbackForm;

	private TutorialSelector tutorialSelector;

	/**
	 * Creates a new browser which is linked to the given {@link TutorialSelector}.
	 *
	 * @param tutorialSelector
	 *            the selector which should be observed
	 */
	public TutorialBrowser(TutorialSelector tutorialSelector) {
		this.tutorialSelector = tutorialSelector;
		initGUI();
		Observer<Tutorial> tutorialObserver = (observable, tutorial) -> {

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
				public void loggedActionPerformed(ActionEvent e) {
					activateTutorialStartup();
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
			public void loggedActionPerformed(ActionEvent e) {
				ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_GETTING_STARTED,
						"tutorial:" + selectedTutorial.getIdentifier(), "step_" + (stepIndex + 1) + "_previous");
				displayStep(--stepIndex);
			}
		});
		previousStepButton.setEnabled(false);
		buttonPanel.add(previousStepButton);

		nextStepButton = new NextStepButton();
		nextStepButton.setEnabled(false);
		nextStepButton.setHorizontalTextPosition(SwingConstants.LEFT);
		buttonPanel.add(nextStepButton);

		JPanel footer = new JPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.TAB_BORDER));
		footer.add(buttonPanel, BorderLayout.CENTER);

		return footer;
	}

	/** @since 8.2 */
	private void activateNextStep() {
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_GETTING_STARTED,
				"tutorial:" + selectedTutorial.getIdentifier(), "step_" + (stepIndex + 1) + "_next");
		displayStep(++stepIndex);
	}

	/** @since 8.2 */
	private void activateNextTutorial() {
		try {
			MainFrame mainFrame = RapidMinerGUI.getMainFrame();
			Process tutorialProcess = nextTutorial.makeProcess();
			mainFrame.setOpenedProcess(tutorialProcess);
			TutorialManager.INSTANCE.completedTutorial(nextTutorial.getIdentifier());
			tutorialSelector.setSelectedTutorial(nextTutorial);
		} catch (RuntimeException | MalformedRepositoryLocationException | IOException | XMLException e1) {
			SwingTools.showSimpleErrorMessage("cannot_open_tutorial", e1, nextTutorial.getTitle(), e1.getMessage());
		}
	}

	/** @since 8.2 */
	private void activateTutorialStartup() {
		try {
			StartupDialogRegistry.INSTANCE.showStartupDialog(ToolbarButton.TUTORIAL);
		} catch (NoStartupDialogRegistreredException e1) {
			SwingTools.showVerySimpleErrorMessage("tutorials_not_available");
		}
	}

	private Component createContentPanel() {
		contentGbc = new GridBagConstraints();

		contentPanel = new JPanel(new GridBagLayout()) {

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(getParent().getWidth(), super.getPreferredSize().height);
			}
		};
		contentPanel.setBackground(Colors.WHITE);

		jEditorPane = new JEditorPane() {

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(getParent().getWidth(), super.getPreferredSize().height);
			}
		};
		jEditorPane.setEditable(false);
		contentGbc.gridx = 0;
		contentGbc.gridy = 0;
		contentGbc.weightx = 1.0f;
		contentGbc.fill = GridBagConstraints.HORIZONTAL;
		contentPanel.add(jEditorPane, contentGbc);

		// add filler at bottom
		contentGbc.gridy += 1;
		contentGbc.weighty = 1.0f;
		contentGbc.fill = GridBagConstraints.BOTH;
		contentPanel.add(new JLabel(), contentGbc);

		// prepare contentGbc for feedback form
		contentGbc.gridy += 1;
		contentGbc.weighty = 0.0f;
		contentGbc.fill = GridBagConstraints.HORIZONTAL;

		scrollPane = new ExtendedJScrollPane(contentPanel);
		scrollPane.setBorder(null);

		HTMLEditorKit kit = new HTMLEditorKit();
		jEditorPane.setEditorKit(kit);
		jEditorPane.setMargin(new Insets(0, 0, 0, 0));

		Document doc = kit.createDefaultDocument();
		jEditorPane.setDocument(doc);
		jEditorPane.setText(String.format(INFO_TEMPLATE, NO_TUTORIAL_SELECTED));
		jEditorPane.addHyperlinkListener(e -> {

			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_GETTING_STARTED,
						TUTORIAL_BROWSER_DOCK_KEY, "open_remote_url");
				RMUrlHandler.handleUrl(e.getURL().toString());
			}
		});
		return scrollPane;
	}

	private void updateTutorial(final Tutorial tutorial) {
		if (feedbackForm != null) {
			contentPanel.remove(feedbackForm);
			feedbackForm = null;
		}
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
							SwingTools.invokeLater(() -> displaySteps(null));
							return;
						}

						final List<String> steps = new ArrayList<>();
						DocumentBuilder dBuilder = XMLTools.createDocumentBuilder();
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
						SwingTools.invokeLater(() -> displaySteps(steps));
					} catch (Exception e) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.tutorial.gui.TutorialBrowser.failed_to_load_stepfile",
								new Object[] { tutorial.getTitle(), e });
						SwingTools.invokeLater(() -> jEditorPane.setText(String.format(INFO_TEMPLATE, TRANSFORMATION_FAILURE)));
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
		if (steps != null && !steps.isEmpty()) {
			displayStep(0);
		} else {
			jEditorPane.setText(String.format(INFO_TEMPLATE, NO_STEPS_AVAILABLE));
			updateStepButtons();
		}
	}

	private void displayStep(final int index) {
		final Runnable changeStep = () -> {

			jEditorPane.setText(steps.get(index));
			stepIndex = index;
			updateStepButtons();
		};
		new Thread(() -> {

			try {
				SwingUtilities.invokeAndWait(changeStep);
			} catch (InvocationTargetException | InterruptedException e) {
				SwingUtilities.invokeLater(changeStep);
			}
			SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point(0, 0)));
		}).start();

	}

	private void updateStepButtons() {
		if (steps == null) {
			nextStepButton.setNextAction(NextStepType.CHAPTER);
			nextStepButton.setEnabled(true);
			previousStepButton.setEnabled(false);
			return;
		}
		previousStepButton.setEnabled(stepIndex > 0);
		if (stepIndex < steps.size() - 1) {
			nextStepButton.setNextAction(NextStepType.STEP);
			nextStepButton.setEnabled(true);
			if (feedbackForm != null) {
				contentPanel.remove(feedbackForm);
			}
			return;
		}
		nextStepButton.setNextAction(nextTutorial != null ? NextStepType.TUTORIAL : NextStepType.CHAPTER);
		nextStepButton.setEnabled(true);

		// last step, add feedback form if we have a tutorial selected
		if (selectedTutorial != null) {
			if (feedbackForm == null) {
				feedbackForm = new FeedbackForm(FEEDBACK_KEY_TUTORIAL, selectedTutorial.getIdentifier());
				feedbackForm.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.TAB_BORDER));
			}
			contentPanel.add(feedbackForm, contentGbc);
		}
	}
}
