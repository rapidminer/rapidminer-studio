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
package com.rapidminer.gui.tour.comic.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ScrollableJPopupMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.ComicManagerEvent;
import com.rapidminer.gui.tour.comic.episodes.AbstractEpisode;
import com.rapidminer.gui.tour.comic.gui.actions.RunEpisodeAction;
import com.rapidminer.gui.tour.comic.gui.actions.StopCurrentEpisodeAction;
import com.rapidminer.tools.DynamicIconUrlStreamHandler;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.Tools;


/**
 * This dialog shows all available comics and lets the user start/stop them. Also creates a
 * drop-down button which displays the comics in a stylish popup.
 *
 * @author Marco Boeck
 *
 */
public class ComicDialog extends ButtonDialog {

	private static final long serialVersionUID = -5398410416334063867L;

	/** the height of a cell in the episode list */
	private static int CELL_HEIGHT = 120;

	/** the singleton instance */
	private static ComicDialog instance;

	private static final Object LOCK = new Object();

	/** the trophy icon */
	private static final ImageIcon TROPHY_ICON;

	static {
		ImageIcon icon = null;
		try {
			icon = new ImageIcon(ImageIO.read(Tools.getResource("comics/images/trophy.png")));
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.gui.ComicDialog.failed_to_load_icon");
		} finally {
			TROPHY_ICON = icon;
		}
	}

	/** the episode JList */
	private JList<AbstractEpisode> episodeList;

	/** the series manager observer */
	private Observer<ComicManagerEvent> observer;

	/** the run button */
	private JButton runButton;

	/** the stop button */
	private JButton cancelButton;

	/**
	 * Creates a new {@link ComicDialog} instance.
	 */
	private ComicDialog() {
		super(ApplicationFrame.getApplicationFrame(), "comic_dialog", ModalityType.MODELESS, new Object[] {});

		initGUI();
	}

	/**
	 * Inits the GUI.
	 */
	private void initGUI() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// run episode button
		runButton = new JButton(new RunEpisodeAction());

		// stop episode button
		cancelButton = new JButton(new StopCurrentEpisodeAction());
		cancelButton.setEnabled(false);

		// episode list
		episodeList = new JList<>(new ComicListModel());
		episodeList.setVisibleRowCount(4);
		DefaultListSelectionModel listSelectionModel = new DefaultListSelectionModel() {

			private static final long serialVersionUID = 1L;

			@Override
			public void setSelectionInterval(final int index0, final int index1) {
				// only allow to select the currently running episode while an episode is running
				if (ComicManager.getInstance().isEpisodeRunning()) {
					if (!episodeList.getModel().getElementAt(index0).isEpisodeInProgress()) {
						return;
					}
				}
				super.removeSelectionInterval(episodeList.getSelectedIndex(), episodeList.getSelectedIndex());
				super.setSelectionInterval(index0, index1);
			}

			@Override
			public void removeSelectionInterval(final int index0, final int index1) {
				// deselecting is not allowed
				return;
			}

		};
		episodeList.setSelectionModel(listSelectionModel);
		episodeList.setFixedCellHeight(CELL_HEIGHT);
		episodeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		episodeList.setCellRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				AbstractEpisode episode = (AbstractEpisode) value;

				// create special icon once an episode has been finished
				if (episode.getMaxAchievedProgress() == 100) {
					label.setIcon(SwingTools.createOverlayIcon(episode.getPreviewIcon(), TROPHY_ICON));
				} else {
					label.setIcon(episode.getPreviewIcon());
				}

				StringBuilder goalList = new StringBuilder();
				goalList.append("<ul style=\"font-style: italic;\">");
				for (String goal : episode.getGoals().split(";")) {
					goalList.append("<li>");
					goalList.append(goal.trim());
					goalList.append("</li>");
				}
				goalList.append("</ul>");
				label.setToolTipText(I18N.getGUILabel("comic.list.tooltip", episode.getTitle(), episode.getDescription(),
						goalList.toString()));
				label.setBorder(new EmptyBorder(0, 10, 0, 0));
				String progressBar = createDynamicProgresBar(episode, 200);
				String episodeIndex = "";
				if ("RapidMiner Studio".equals(episode.getComponentName())) {
					episodeIndex = String.valueOf(ComicManager.getInstance().indexOf(episode) + 1) + ". ";
				}
				String text = "<html><body><div style=\"width:200px;\">" + "<p style=\"padding-left:5px;color:"
						+ (isSelected ? "white" : "black") + ";font-size:14;font-weight:bold;\">" + episodeIndex
						+ episode.getTitle() + "</p><div style=\"padding-left:5px;color:" + (isSelected ? "white" : "gray")
						+ ";\">" + episode.getComponentName() + "</div><p style=\"padding-left:5px;padding-top:10px;\">"
						+ episode.getDescription() + "</p>" + progressBar + "</div></body></html>";
				if (ComicManager.getInstance().isEpisodeRunning()) {
					if (episode == ComicManager.getInstance().getCurrentEpisode()) {
						String runningText = I18N.getMessage(I18N.getGUIBundle(), "gui.label.comic.dialog.running.label");
						text = "<html><body><div style=\"width:200px;\">" + "<p style=\"padding-left:5px;color:"
								+ (isSelected ? "white" : "black") + ";font-size:14;font-weight:bold;\">" + episodeIndex
								+ episode.getTitle() + "</p><div style=\"padding-left:5px;color:"
								+ (isSelected ? "white" : "gray") + ";\">" + episode.getComponentName()
								+ "</div><p style=\"padding-left:5px;padding-top:10px;color:red;font-weight:bold;\">"
								+ runningText + "</p>" + progressBar + "</div></body></html>";
					} else {
						text = "<html><body><div style=\"width:200px;\">"
								+ "<p style=\"padding-left:5px;color:gray;font-size:14;font-weight:bold;\">" + episodeIndex
								+ episode.getTitle() + "</p><div style=\"padding-left:5px;color:gray;\">"
								+ episode.getComponentName()
								+ "</div><p style=\"padding-left:5px;padding-top:10px;color:gray\">"
								+ episode.getDescription() + "</p>" + progressBar + "</div></body></html>";
					}
				}
				label.setText(text);
				return label;
			}
		});
		// preselect first episode which the user has not yet completed
		int selectIndex = 0;
		for (int i = 0; i < episodeList.getModel().getSize(); i++) {
			if (episodeList.getModel().getElementAt(i).getMaxAchievedProgress() < 100) {
				selectIndex = i;
				break;
			}
		}
		episodeList.setSelectedIndex(selectIndex);
		episodeList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				// don't start episode while one is running
				if (ComicManager.getInstance().isEpisodeRunning()) {
					return;
				}

				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
					RunEpisodeAction.startComic(getSelectedEpisode());
				}
			}
		});
		gbc.gridwidth = 2;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(10, 5, 5, 5);
		panel.add(new ExtendedJScrollPane(episodeList), gbc);

		// add series manager state observer
		observer = new Observer<ComicManagerEvent>() {

			@Override
			public void update(final Observable<ComicManagerEvent> observable, final ComicManagerEvent arg) {
				switch (arg) {
					case COMIC_ADDED:
						((ComicListModel) episodeList.getModel()).fireEpisodeAdded(arg.getEpisode());
						break;
					case EPISODE_CHANGED:
						break;
					case EPISODE_STARTED:
						runButton.setEnabled(false);
						cancelButton.setEnabled(true);
						episodeList.setSelectedValue(ComicManager.getInstance().getCurrentEpisode(), true);
						break;
					case EPISODE_FINISHED:
						// if an episode finishes, select the next one in the list (if there is one)
						episodeList.setSelectedValue(arg.getEpisode(), true);
						int prevIndex = episodeList.getSelectedIndex();
						int newIndex = prevIndex + 1;
						if (episodeList.getModel().getSize() > newIndex) {
							episodeList.setSelectedIndex(newIndex);
							episodeList.ensureIndexIsVisible(newIndex);
						}
						runButton.setEnabled(true);
						cancelButton.setEnabled(false);
						break;
					case EPISODE_CANCELED:
					default:
						runButton.setEnabled(true);
						cancelButton.setEnabled(false);
						break;
				}
			}
		};
		ComicManager.getInstance().addObserver(observer, true);

		setResizable(false);
		layoutDefault(panel, LARGE, runButton, cancelButton);
		setPreferredSize(new Dimension(450, 650));
		pack();
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
	}

	/**
	 * Returns the selected {@link AbstractEpisode} or <code>null</code> if none is selected.
	 *
	 * @return
	 */
	public AbstractEpisode getSelectedEpisode() {
		return episodeList.getSelectedValue();
	}

	/**
	 * Prepares and shows the dialog if it is not yet shown currently.
	 * <p>
	 * Use this instead of {@link #setVisible(boolean)}.
	 * </p>
	 * Otherwise does nothing.
	 */
	public void showDialog() {
		if (!isVisible()) {
			setVisible(true);
		}
	}

	/**
	 * Returns the {@link ComicDialog} instance.
	 *
	 * @return
	 */
	public static ComicDialog getInstance() {
		synchronized (LOCK) {
			if (instance == null) {
				instance = new ComicDialog();
			}
		}
		return instance;
	}

	/**
	 * Creates a {@link DropDownButton} button which will show the available comics.
	 *
	 * @return
	 */
	public static DropDownButton makeDropDownButton() {
		// init dialog so listener is registered, otherwise starting a comic via the popup will not
		// be registered by the GUI
		ComicDialog.getInstance();
		final DropDownButton dropDownToReturn = new DropDownButton(new ResourceActionAdapter(false, "comic_dropdown")) {

			private static final long serialVersionUID = 3551453930121891910L;

			@Override
			protected JPopupMenu getPopupMenu() {
				JPopupMenu menu = new ScrollableJPopupMenu(ScrollableJPopupMenu.SIZE_NORMAL);
				for (JMenuItem item : createComicItems()) {
					menu.add(item);
				}
				return menu;
			}

			/**
			 * Creates a list of menu items; one for each comic.
			 *
			 * @return
			 */
			private List<JMenuItem> createComicItems() {
				List<JMenuItem> list = new LinkedList<>();
				ComicManager seriesManager = ComicManager.getInstance();
				for (int i = 0; i < seriesManager.getNumberOfEpisodes(); i++) {
					final AbstractEpisode episode = seriesManager.getEpisodeByIndex(i);
					final JMenuItem item = new JMenuItem();
					if (episode.getMaxAchievedProgress() == 100) {
						item.setIcon(SwingTools.createOverlayIcon(episode.getPreviewIcon(), TROPHY_ICON));
					} else {
						item.setIcon(episode.getPreviewIcon());
					}

					StringBuilder goalList = new StringBuilder();
					goalList.append("<ul style=\"font-style: italic;\">");
					for (String goal : episode.getGoals().split(";")) {
						goalList.append("<li>");
						goalList.append(goal.trim());
						goalList.append("</li>");
					}
					goalList.append("</ul>");
					item.setToolTipText(I18N.getGUILabel("comic.list.tooltip", episode.getTitle(), episode.getDescription(),
							goalList.toString()));
					String progressBar = createDynamicProgresBar(episode, 200);
					String episodeIndex = "";
					if ("RapidMiner Studio".equals(episode.getComponentName())) {
						episodeIndex = String.valueOf(ComicManager.getInstance().indexOf(episode) + 1) + ". ";
					}
					String text = "<html><body><div style=\"width:200px\">"
							+ "<p style=\"padding-left:5px;color:black;font-size:14;font-weight:bold;\">" + episodeIndex
							+ episode.getTitle() + "</p><div style=\"padding-left:5px;color:gray;\">"
							+ episode.getComponentName() + "</div><p style=\"padding-left:5px;padding-top:10px;\">"
							+ episode.getDescription() + "</p>" + progressBar + "</div></body></html>";
					// if there is currently a comic running, only enable the running comic menu
					// item to stop the comic
					if (ComicManager.getInstance().isEpisodeRunning()) {
						item.setEnabled(episode.isEpisodeInProgress());
						if (episode == ComicManager.getInstance().getCurrentEpisode()) {
							String runningText = I18N.getMessage(I18N.getGUIBundle(),
									"gui.label.comic.popup.click_to_stop.label");
							String stopTip = I18N.getMessage(I18N.getGUIBundle(), "gui.label.comic.popup.click_to_stop.tip");
							text = "<html><body><div style=\"width:200px\">"
									+ "<p style=\"padding-left:5px;color:black;font-size:14;font-weight:bold;\">"
									+ episodeIndex + episode.getTitle() + "</p><div style=\"padding-left:5px;color:gray;\">"
									+ episode.getComponentName()
									+ "</div><p style=\"padding-left:5px;padding-top:10px;color:red;font-weight:bold;\">"
									+ runningText + "</p>" + progressBar + "</div></body></html>";
							item.setToolTipText(stopTip);
						}
					}
					item.setText(text);
					item.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(final ActionEvent e) {
							if (episode.isEpisodeInProgress()) {
								StopCurrentEpisodeAction.stopCurrentEpisode();
							} else {
								RunEpisodeAction.startComic(episode);
							}
						}
					});
					// use HTML formatting to make it look nice

					list.add(item);
				}
				return list;
			}

		};
		dropDownToReturn.setHorizontalTextPosition(SwingConstants.CENTER);
		return dropDownToReturn;
	}

	/**
	 * Helper method to create a progres bar for the progress of the specified
	 * {@link AbstractEpisode}. This bar is realized via dynicon. See
	 * {@link DynamicIconUrlStreamHandler}.
	 *
	 * @param episode
	 * @param width
	 * @return
	 */
	private static String createDynamicProgresBar(final AbstractEpisode episode, final int width) {
		int percent = episode.getMaxAchievedProgress();
		String progressIcon = "<img src=\"dynicon://progress/" + width + "/8/" + percent + "\"/>";
		if (percent == 100) {
			return "<p style=\"padding-left:5px;padding-bottom:15px;\">" + progressIcon + "&#160;" + " <img src=\""
					+ Tools.getResource("icons/16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.label.comic.finished.icon"))
					+ "\"/>" + "</p>";
		}
		return "<p style=\"padding-left:5px;padding-bottom:15px;\">" + progressIcon + "&#160;" + percent + "%" + "</p>";
	}

}
