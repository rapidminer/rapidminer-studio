/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.rapidminer.ProcessLocation;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.Perspectives;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.OpenAction;
import com.rapidminer.gui.actions.WelcomeComicsAction;
import com.rapidminer.gui.actions.WelcomeNewAction;
import com.rapidminer.gui.actions.WelcomeOpenAction;
import com.rapidminer.gui.actions.WelcomeTemplatesAction;
import com.rapidminer.gui.look.ui.EditorPaneUI;
import com.rapidminer.gui.tools.components.FancyButton;
import com.rapidminer.gui.tools.components.FancyConstants;
import com.rapidminer.gui.tools.components.FancyDropDownButton;
import com.rapidminer.license.License;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.Tools;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Lets the user select with what he wants to start: blank, existing file, recent file, accelerator
 * or tutorial. This panel is shown after RapidMiner was started.
 *
 * @author Ingo Mierswa, Philipp Kersting, Nils Woehler, Marco Boeck
 */
public final class WelcomeScreen extends ImagePanel implements Dockable {

	private class OpenRecentAction extends AbstractAction {

		private static final long serialVersionUID = -1102955323711354094L;

		ProcessLocation process;

		public OpenRecentAction(final ProcessLocation processLocation) {
			super(processLocation.toMenuString(), SwingTools.createIcon("16/gear.png"));
			process = processLocation;

		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			OpenAction.open(process, true);
			WelcomeScreen.this.mainFrame.getPerspectives().showPerspective(Perspectives.DESIGN);
		}

	}

	private static final long serialVersionUID = -6916236648023490473L;

	protected static final String PROXY_HELP = I18N.getGUILabel("welcome.proxy.label");

	public static final String WELCOME_SCREEN_DOCK_KEY = "welcome";

	private static final DockKey DOCK_KEY = new ResourceDockKey(WELCOME_SCREEN_DOCK_KEY);

	private static Image backgroundImage = null;

	// top whitespace constants
	private static final int TOP_WHITESPACE_HEIGHT = 25;
	private static final Dimension TOP_WHITESPACE_DIM = new Dimension(0, TOP_WHITESPACE_HEIGHT);

	// action bar constants
	private static final int ACTIONBAR_INSET = 12;
	private static final Insets ACTIONBAR_BUTTON_INSTES = new Insets(0, ACTIONBAR_INSET, 0, ACTIONBAR_INSET);

	private static final int TOOLBAR_BUTTON_WIDTH = 475;
	private static final int TOOLBAR_BUTTON_HEIGHT = 140;

	private static final Dimension TOOLBAR_BUTTON_DIMENSION = new Dimension(TOOLBAR_BUTTON_WIDTH, TOOLBAR_BUTTON_HEIGHT);

	private static final int LEFT_NEW_INSET = 5;

	private static final int MIN_RECENT_FILES_POPUP_WIDTH = 410;

	static {
		try {
			URL url = Tools.getResource("rm_welcome_bg.png");
			if (url != null) {
				backgroundImage = ImageIO.read(url);
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.WelcomeScreen.loading_images_error",
					e.getMessage());
		}
		DOCK_KEY.setCloseEnabled(false);
		DOCK_KEY.setFloatEnabled(false);
		DOCK_KEY.setAutoHideEnabled(false);
		DOCK_KEY.setMaximizeEnabled(false);

		// try to load fonts
		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Light.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Semibold.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-Bold.ttf")));
			ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, Tools.getResourceInputStream("fonts/OpenSans-ExtraBold.ttf")));
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.WelcomeScreen.font_load_failed",
					e.getMessage());
		}
	}

	// initialize fonts after Open Sans has been loaded in static block
	private static final Font OPEN_SANS_LIGHT_16 = new Font("Open Sans Light", Font.PLAIN, 16);
	private static final Font OPEN_SANS_LIGHT_28 = new Font("Open Sans Light", Font.PLAIN, 28);
	private static final Font OPEN_SANS_LIGHT_48 = new Font("Open Sans Light", Font.PLAIN, 48);

	private final MouseAdapter recentFilesMouseAdapter = new MouseAdapter() {

		@Override
		public void mouseEntered(final MouseEvent e) {
			super.mouseEntered(e);
			recentFilesButton.setArrowButtonVisible(true);
		}

		@Override
		public void mouseExited(final MouseEvent e) {
			super.mouseExited(e);
			if (!recentFilesButton.isPopupMenuVisible()) {
				recentFilesButton.setArrowButtonVisible(false);
			}
		}
	};

	/**
	 * Dropdown button for recent processes
	 */
	private final FancyDropDownButton recentFilesButton = new FancyDropDownButton(new WelcomeOpenAction(), true) {

		private static final long serialVersionUID = 1L;

		@Override
		protected JPopupMenu getPopupMenu() {
			final JPopupMenu menu = new JPopupMenu();
			// set same font as used for menu entries, so String width calculation is accurate
			menu.setFont(OPEN_SANS_LIGHT_16);
			menu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			FontMetrics fontMetrics = menu.getFontMetrics(menu.getFont());

			int maxPopupWidth = Integer.MIN_VALUE;
			List<ProcessLocation> recentFiles = RapidMinerGUI.getRecentFiles();
			if (recentFiles.size() == 0) {
				JLabel noRecentFiles = new ResourceLabel("welcome.no_recent_processes");
				noRecentFiles.setEnabled(false);
				noRecentFiles.setFont(OPEN_SANS_LIGHT_16);
				menu.add(noRecentFiles);
			} else {
				for (ProcessLocation location : recentFiles) {
					menu.add(new OpenRecentAction(location));
					maxPopupWidth = Math.max(maxPopupWidth,
							SwingUtilities.computeStringWidth(fontMetrics, location.toMenuString()));
				}
			}
			int minWidth = recentFilesButton.getWidth();
			menu.setMinimumSize(new Dimension(minWidth, 0));
			for (Component comp : menu.getComponents()) {
				if (JMenuItem.class.isAssignableFrom(comp.getClass())) {
					JMenuItem.class.cast(comp).setFont(OPEN_SANS_LIGHT_16);
				}
			}
			menu.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
					recentFilesButton.setArrowButtonVisible(true);
				}

				@Override
				public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
					if (!recentFilesButton.isHovered()) {
						recentFilesButton.setArrowButtonVisible(false);
						recentFilesButton.repaint();
					}
				}

				@Override
				public void popupMenuCanceled(final PopupMenuEvent e) {
					if (!recentFilesButton.isHovered()) {
						recentFilesButton.setArrowButtonVisible(false);
						recentFilesButton.repaint();
					}
				}
			});

			if (maxPopupWidth < MIN_RECENT_FILES_POPUP_WIDTH) {
				menu.setPopupSize(new Dimension(MIN_RECENT_FILES_POPUP_WIDTH, (int) menu.getPreferredSize().getHeight()));
			}
			return menu;
		}

	};

	private final MainFrame mainFrame;

	public WelcomeScreen(final MainFrame mainFrame) {
		super(backgroundImage, ResizeHandling.CHILDRENS_PREFERRED_SIZE, VerticalAnchor.BOTTOM);
		this.mainFrame = mainFrame;
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		setComponentPopupMenu(null);
		setInheritsPopupMenu(false);

		// left whitespace
		addLeftWhitespace();

		// vertical action bar
		addActionBarPanel();

		// news text panel
		addNewsPanel();
	}

	private void addNewsPanel() {

		// create news panel
		JPanel newsPanel = new JPanel(new GridBagLayout()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				// draw separator on left side
				Graphics2D g2 = (Graphics2D) g;
				int y = (int) (getHeight() * 0.1f);
				int height = (int) (getHeight() * 0.99f);
				g2.setColor(new Color(225, 225, 225));
				g2.drawLine(0, y, 0, height);
			}
		};
		newsPanel.setComponentPopupMenu(null);
		newsPanel.setOpaque(true);
		newsPanel.setBackground(Color.WHITE);
		newsPanel.setSize(500, newsPanel.getHeight());
		newsPanel.setMinimumSize(new Dimension(500, 0));
		newsPanel.setMaximumSize(new Dimension(700, (int) newsPanel.getMaximumSize().getHeight()));

		// add top whitespace
		{
			JPanel topWhitespace = new JPanel();
			topWhitespace.setOpaque(false);

			GridBagConstraints topConstraints = new GridBagConstraints();
			topConstraints.weightx = 1;
			topConstraints.weighty = 0.1;
			topConstraints.fill = GridBagConstraints.BOTH;
			topConstraints.gridheight = 1;
			topConstraints.gridwidth = GridBagConstraints.REMAINDER;

			newsPanel.add(topWhitespace, topConstraints);
		}

		// add news content panel
		{
			JPanel newsContentPanel = new JPanel(new GridBagLayout());
			newsContentPanel.setOpaque(false);

			// add news header
			JLabel newsHeading = new JLabel(I18N.getGUILabel("news"));
			newsHeading.setFont(OPEN_SANS_LIGHT_48);
			newsHeading.setForeground(FancyConstants.HOVERED_TEXTCOLOR);
			newsHeading.setMaximumSize(new Dimension(newsHeading.getWidth(), 100));

			GridBagConstraints newsHeaderConstraints = new GridBagConstraints();
			newsHeaderConstraints.fill = GridBagConstraints.HORIZONTAL;
			newsHeaderConstraints.gridheight = 1;
			newsHeaderConstraints.gridwidth = GridBagConstraints.REMAINDER;
			newsHeaderConstraints.weightx = 1;
			newsHeaderConstraints.weighty = 0;
			newsHeaderConstraints.insets = new Insets(12, LEFT_NEW_INSET, 0, 0);
			newsContentPanel.add(newsHeading, newsHeaderConstraints);
			newsContentPanel.setComponentPopupMenu(null);
			newsContentPanel.setInheritsPopupMenu(false);

			// add news pane
			final JEditorPane newsPane = new ExtendedHTMLJEditorPane("text/html", "") {

				private static final long serialVersionUID = 1L;

				@Override
				public Action[] getActions() {
					return new Action[] {};
				}
			};
			newsPane.setOpaque(false);

			final String loadingMessage = "<html><body><p>"
					+ I18N.getGUILabel("welcome.news_loading.label", RMUrlHandler.PREFERENCES_URL) + "</p></body></html>";
			final String errorMessage = "<html><body><p>" + I18N.getGUILabel("welcome.news_error.label")
					+ "</p></body></html>";
			newsPane.setText(loadingMessage);
			newsPane.setEditable(false);
			newsPane.setPreferredSize(new Dimension(300, 180));
			try {
				String url = I18N.getMessage(I18N.getGUIBundle(), "gui.label.news.url");
				License activeLicense = ProductConstraintManager.INSTANCE.getActiveLicense();
				if (activeLicense != null && activeLicense.getLicenseID() != null) {
					url += "?lid=" + activeLicense.getLicenseID();
				}
				newsPane.setPage(url);
			} catch (IOException e2) {
				LogService.getRoot().log(
						Level.INFO,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.gui.tools.WelcomeScreen.downloading_news_error", e2.getMessage()));
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						newsPane.setText(errorMessage);
					}
				});
			}

			newsPane.addHyperlinkListener(new HyperlinkListener() {

				@Override
				public void hyperlinkUpdate(final HyperlinkEvent e) {
					if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
						if (!RMUrlHandler.handleUrl(e.getDescription())) {
							try {
								RMUrlHandler.browse(e.getURL().toURI());
							} catch (Exception e1) {
								LogService.getRoot().log(
										Level.WARNING,
										I18N.getMessage(LogService.getRoot().getResourceBundle(),
												"com.rapidminer.gui.tools.WelcomeScreen.displaying_news_site_error",
												e.getDescription(), e1.getMessage()));

							}
						}
					}
				}
			});

			// dirty hack to remove the ugly cut/copy/paste popup which is added in the earliest
			// possible place, the EditorPaneUI look&feel
			// implementation..
			MouseListener toRemove = null;
			for (MouseListener l : newsPane.getMouseListeners()) {
				if (EditorPaneUI.class.getPackage().equals(l.getClass().getPackage())) {
					toRemove = l;
				}
			}
			newsPane.removeMouseListener(toRemove);

			JScrollPane newsScrollPane = new JScrollPane(newsPane);
			newsScrollPane.setPreferredSize(new Dimension(300, 180));
			newsScrollPane.setOpaque(false);
			newsScrollPane.getViewport().setOpaque(false);
			newsScrollPane.setBorder(null);
			newsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			newsScrollPane.setComponentPopupMenu(null);
			newsScrollPane.setInheritsPopupMenu(false);

			GridBagConstraints newsConstraint = new GridBagConstraints();
			newsConstraint.fill = GridBagConstraints.BOTH;
			newsConstraint.gridwidth = GridBagConstraints.REMAINDER;
			newsConstraint.weightx = 1;
			newsConstraint.weighty = 1;
			newsConstraint.insets = new Insets(0, LEFT_NEW_INSET, 5, 5);

			newsContentPanel.add(newsScrollPane, newsConstraint);

			GridBagConstraints newsContentConstraints = new GridBagConstraints();
			newsContentConstraints.fill = GridBagConstraints.BOTH;
			newsContentConstraints.gridwidth = GridBagConstraints.REMAINDER;
			newsContentConstraints.weightx = 1;
			newsContentConstraints.weighty = 0.9;
			newsContentConstraints.insets = new Insets(0, 25, 0, 100);

			newsPanel.add(newsContentPanel, newsContentConstraints);
		}

		GridBagConstraints newsPanelConstraints = new GridBagConstraints();
		newsPanelConstraints.fill = GridBagConstraints.BOTH;
		newsPanelConstraints.gridx = 4;
		newsPanelConstraints.weightx = 0.5;
		newsPanelConstraints.weighty = 0.9;
		newsPanelConstraints.gridwidth = 1;
		newsPanelConstraints.gridheight = 1;

		add(newsPanel, newsPanelConstraints);
	}

	private void addLeftWhitespace() {

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.75;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = GridBagConstraints.REMAINDER;

		JPanel leftWhiteSpace = new JPanel();
		leftWhiteSpace.setOpaque(false);
		leftWhiteSpace.setMinimumSize(new Dimension(1, 50));
		add(leftWhiteSpace, c);
	}

	private void addActionBarPanel() {
		JPanel actionBarPanel = new JPanel(new GridBagLayout());
		actionBarPanel.setOpaque(false);

		// add top whitespace
		{
			JPanel topWhitespace = new JPanel();
			topWhitespace.setOpaque(false);
			topWhitespace.setPreferredSize(TOP_WHITESPACE_DIM);
			topWhitespace.setMinimumSize(TOP_WHITESPACE_DIM);

			GridBagConstraints topConstraints = new GridBagConstraints();
			topConstraints.weightx = 1;
			topConstraints.weighty = 0;
			topConstraints.fill = GridBagConstraints.BOTH;
			topConstraints.gridheight = 1;
			topConstraints.gridwidth = GridBagConstraints.REMAINDER;

			actionBarPanel.add(topWhitespace, topConstraints);
		}

		// add bar content panel
		{

			JPanel barContentPanel = new JPanel(new GridBagLayout());
			barContentPanel.setOpaque(false);

			addActionBar(barContentPanel);

			// add space filler
			{
				JPanel separator = new JPanel();
				separator.setOpaque(false);

				GridBagConstraints seperatorConstraints = new GridBagConstraints();
				seperatorConstraints.fill = GridBagConstraints.BOTH;
				seperatorConstraints.weightx = 1;
				seperatorConstraints.weighty = 1;
				seperatorConstraints.gridwidth = GridBagConstraints.REMAINDER;

				barContentPanel.add(separator, seperatorConstraints);
			}

			GridBagConstraints barContentConstraints = new GridBagConstraints();
			barContentConstraints.fill = GridBagConstraints.BOTH;
			barContentConstraints.gridwidth = GridBagConstraints.REMAINDER;
			barContentConstraints.weightx = 1;
			barContentConstraints.weighty = 1;

			actionBarPanel.add(barContentPanel, barContentConstraints);
		}

		GridBagConstraints actionBarPanelConstraints = new GridBagConstraints();
		actionBarPanelConstraints.fill = GridBagConstraints.NONE;
		actionBarPanelConstraints.weightx = 0;
		actionBarPanelConstraints.weighty = 1;
		actionBarPanelConstraints.gridx = 1;
		actionBarPanelConstraints.gridy = 0;
		actionBarPanelConstraints.gridwidth = 1;
		actionBarPanelConstraints.anchor = GridBagConstraints.CENTER;
		actionBarPanelConstraints.gridheight = GridBagConstraints.REMAINDER;
		actionBarPanelConstraints.insets = new Insets(0, 0, 0, 25);

		add(actionBarPanel, actionBarPanelConstraints);
	}

	private void addActionBar(final JPanel actionBarContentPanel) {
		JToolBar actionBar = new JToolBar();
		actionBar.setBorder(null);
		actionBar.setLayout(new GridBagLayout());
		actionBar.setOpaque(false);

		// add comic tutorial button
		{
			GridBagConstraints tutorialButtonConstraint = new GridBagConstraints();
			tutorialButtonConstraint.anchor = GridBagConstraints.CENTER;
			tutorialButtonConstraint.fill = GridBagConstraints.HORIZONTAL;
			tutorialButtonConstraint.weightx = 1;
			tutorialButtonConstraint.weighty = 0;
			tutorialButtonConstraint.gridwidth = GridBagConstraints.REMAINDER;
			tutorialButtonConstraint.insets = ACTIONBAR_BUTTON_INSTES;

			JButton tutorialButton = new FancyButton(new WelcomeComicsAction(this.mainFrame));
			tutorialButton.setMargin(ACTIONBAR_BUTTON_INSTES);
			tutorialButton.setOpaque(false);
			tutorialButton.setPreferredSize(TOOLBAR_BUTTON_DIMENSION);
			tutorialButton.setMinimumSize(TOOLBAR_BUTTON_DIMENSION);
			tutorialButton.setMaximumSize(TOOLBAR_BUTTON_DIMENSION);
			tutorialButton.setBorder(BorderFactory.createCompoundBorder(tutorialButton.getBorder(),
					BorderFactory.createEmptyBorder(0, 10, 0, 0)));
			tutorialButton.setFont(OPEN_SANS_LIGHT_28);

			actionBar.add(tutorialButton, tutorialButtonConstraint);
		}

		// add first whitespace between buttons
		{
			addActionBarButtonSeparator(actionBar, false);
		}

		// templates button
		{
			GridBagConstraints wizardButtonConstraints = new GridBagConstraints();
			wizardButtonConstraints.anchor = GridBagConstraints.CENTER;
			wizardButtonConstraints.fill = GridBagConstraints.HORIZONTAL;
			wizardButtonConstraints.weightx = 1;
			wizardButtonConstraints.weighty = 0;
			wizardButtonConstraints.gridwidth = GridBagConstraints.REMAINDER;
			wizardButtonConstraints.insets = ACTIONBAR_BUTTON_INSTES;

			JButton wizardButton = new FancyButton(new WelcomeTemplatesAction(this.mainFrame));
			wizardButton.setMargin(ACTIONBAR_BUTTON_INSTES);
			wizardButton.setOpaque(false);
			wizardButton.setPreferredSize(TOOLBAR_BUTTON_DIMENSION);
			wizardButton.setMinimumSize(TOOLBAR_BUTTON_DIMENSION);
			wizardButton.setMaximumSize(TOOLBAR_BUTTON_DIMENSION);
			wizardButton.setBorder(BorderFactory.createCompoundBorder(wizardButton.getBorder(),
					BorderFactory.createEmptyBorder(0, 10, 0, 0)));
			wizardButton.setFont(OPEN_SANS_LIGHT_28);

			actionBar.add(wizardButton, wizardButtonConstraints);
		}

		// add separator
		{
			addActionBarButtonSeparator(actionBar, true);
		}

		// add new process button
		{
			GridBagConstraints newProcessConstraints = new GridBagConstraints();
			newProcessConstraints.anchor = GridBagConstraints.CENTER;
			newProcessConstraints.fill = GridBagConstraints.HORIZONTAL;
			newProcessConstraints.weightx = 1;
			newProcessConstraints.weighty = 0;
			newProcessConstraints.gridwidth = GridBagConstraints.REMAINDER;
			newProcessConstraints.insets = ACTIONBAR_BUTTON_INSTES;

			JButton newProcessButton = new FancyButton(new WelcomeNewAction(this.mainFrame));
			newProcessButton.setPreferredSize(TOOLBAR_BUTTON_DIMENSION);
			newProcessButton.setMinimumSize(TOOLBAR_BUTTON_DIMENSION);
			newProcessButton.setMaximumSize(TOOLBAR_BUTTON_DIMENSION);
			newProcessButton.setBorder(BorderFactory.createCompoundBorder(newProcessButton.getBorder(),
					BorderFactory.createEmptyBorder(0, 10, 0, 0)));
			newProcessButton.setFont(OPEN_SANS_LIGHT_28);

			actionBar.add(newProcessButton, newProcessConstraints);
		}

		// add separator
		{
			addActionBarButtonSeparator(actionBar, false);
		}

		// add recent open button
		{
			GridBagConstraints leftButtonConstraint = new GridBagConstraints();
			leftButtonConstraint.gridwidth = 1;
			leftButtonConstraint.anchor = GridBagConstraints.CENTER;
			leftButtonConstraint.fill = GridBagConstraints.BOTH;
			leftButtonConstraint.weightx = 1;
			leftButtonConstraint.insets = new Insets(ACTIONBAR_INSET, ACTIONBAR_INSET, ACTIONBAR_INSET, 0);

			GridBagConstraints rightButtonConstraint = new GridBagConstraints();
			rightButtonConstraint.gridwidth = GridBagConstraints.REMAINDER;
			rightButtonConstraint.anchor = GridBagConstraints.CENTER;
			rightButtonConstraint.fill = GridBagConstraints.VERTICAL;
			rightButtonConstraint.weightx = 0;
			rightButtonConstraint.weighty = 0;
			rightButtonConstraint.insets = new Insets(ACTIONBAR_INSET, 0, ACTIONBAR_INSET, ACTIONBAR_INSET);

			recentFilesButton.setMargin(ACTIONBAR_BUTTON_INSTES);
			recentFilesButton.setOpaque(false);
			Dimension recentFilesButtonDim = new Dimension(recentFilesButton.getWidth(), TOOLBAR_BUTTON_HEIGHT);
			recentFilesButton.setMinimumSize(recentFilesButtonDim);
			recentFilesButton.setPreferredSize(recentFilesButtonDim);
			recentFilesButton.setMaximumSize(recentFilesButtonDim);
			recentFilesButton.setArrowSizeFactor(4f);
			recentFilesButton.addMouseListener(recentFilesMouseAdapter);
			recentFilesButton.addArrowButtonMouseListener(recentFilesMouseAdapter);
			recentFilesButton.setBorder(BorderFactory.createCompoundBorder(recentFilesButton.getBorder(),
					BorderFactory.createEmptyBorder(0, 10, 0, 0)));

			recentFilesButton.addToToolbar(actionBar, leftButtonConstraint, rightButtonConstraint);
			recentFilesButton.setArrowButtonVisible(false);
		}

		GridBagConstraints actionBarConstraints = new GridBagConstraints();
		actionBarConstraints.fill = GridBagConstraints.BOTH;
		actionBarConstraints.weightx = 1;
		actionBarConstraints.weighty = 0;
		actionBarConstraints.gridwidth = GridBagConstraints.REMAINDER;
		actionBarConstraints.anchor = GridBagConstraints.BASELINE;

		actionBarContentPanel.add(actionBar, actionBarConstraints);
	}

	private void addActionBarButtonSeparator(final JToolBar actionBar, boolean displaySeparator) {
		GridBagConstraints separatorConstraints = new GridBagConstraints();
		separatorConstraints.anchor = GridBagConstraints.CENTER;
		separatorConstraints.fill = GridBagConstraints.HORIZONTAL;
		separatorConstraints.weightx = 1;
		separatorConstraints.weighty = 0;
		separatorConstraints.gridwidth = GridBagConstraints.REMAINDER;

		Component separator = null;
		if (displaySeparator) {
			JSeparator jSeparator = new JSeparator();
			jSeparator.setForeground(SwingTools.RAPIDMINER_ORANGE);
			separatorConstraints.insets = new Insets(20, 0, 20, 0);
			separator = jSeparator;
		} else {
			JPanel panel = new JPanel();
			panel.setOpaque(false);
			panel.setPreferredSize(new Dimension(0, 20));
			separator = panel;
		}

		actionBar.add(separator, separatorConstraints);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

}
