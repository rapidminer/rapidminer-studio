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
package com.rapidminer.gui.tour;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;


/**
 * This class creates a dialog in which the user can choose a Tour (e.g. RapidMinerTour) and starts
 * the Tour. It also displays the progress-state of the Tours.
 *
 * @author Thilo Kamradt
 *
 */
public class TourChooser extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	private TourManager tourManager;
	private JList list;

	/**
	 * Shows a dialog with a list of the Tours which are currently available and starts the chosen
	 * Tour.
	 */
	public TourChooser() {
		super(ApplicationFrame.getApplicationFrame(), "Tour", ModalityType.MODELESS, new Object[] {});
		tourManager = TourManager.getInstance();
		super.layoutDefault(makeTable(), LARGE, makeOkButton("tour.startTour"), makeCloseButton());
		super.setSize(455, 500);
		super.setResizable(false);
	}

	@Override
	protected void ok() {
		IntroductoryTour choosenTour = (IntroductoryTour) list.getSelectedValue();
		if (choosenTour != null) {
			choosenTour.startTour();
			super.ok();

		}

	}

	@SuppressWarnings("unchecked")
	protected JComponent makeTable() {
		list = new JList(new AbstractListModel() {

			private static final long serialVersionUID = 1L;

			@Override
			public int getSize() {
				return tourManager.size();
			}

			@Override
			public Object getElementAt(int index) {
				return tourManager.get(index);
			}
		});
		list.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {

			}

			@Override
			public void mouseEntered(MouseEvent e) {

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					IntroductoryTour choosenTour = (IntroductoryTour) list.getSelectedValue();
					if (choosenTour != null) {
						choosenTour.startTour();
						TourChooser.this.dispose();
					}
				}
			}
		});
		list.setCellRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				// ICON
				renderer.setIcon(new ImageIcon(Tools.getResource("rapidminer_frame_icon_48.png"), ""));
				String tourKey = ((IntroductoryTour) value).getKey();
				String description = I18N.getMessage(I18N.getGUIBundle(), "gui.tour." + tourKey + ".description");
				String relation = "<br> This Tour relates to: "
						+ I18N.getMessage(I18N.getGUIBundle(), "gui.tour." + tourKey + ".relation");
				String statusValue = "";
				// add progress
				int percent = tourManager.getProgressInPercent(tourKey);
				String progressIcon = "<img  src=\"dynicon://progress/200/8/" + percent + "\"/>";
				switch (percent) {
					case 0:
						statusValue += I18N.getMessage(I18N.getGUIBundle(), "gui.tour.dropDown.not_started");
						break;
					case 100:
						statusValue += progressIcon + "&#160;" + " <img src=\""
								+ Tools.getResource(I18N.getMessage(I18N.getGUIBundle(), "gui.tour.dropDown.icon")) + "\"/>";
						break;
					default:
						statusValue += progressIcon + "&#160;" + percent + "%";
				}
				renderer.setText("<html><div style=\"width:300px\">" + "<h3 style=\"padding-left:5px;color:"
						+ (isSelected ? "white" : "black") + ";\">" + tourKey + "</h3><p style=\"padding-left:5px;\">"
						+ description + "</p><p style=\"padding-left:5px;\">" + relation
						+ "</p><p style=\"padding-left:5px;\">" + statusValue + "</p></div></html>");
				return renderer;
			}
		});
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		// add JSrcollPane if necessary
		JScrollPane scroll = new JScrollPane(list);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		return scroll;
	}

	public static DropDownButton makeAchievmentDropDown() {

		final DropDownButton dropDownToReturn = new DropDownButton(new ResourceActionAdapter(false, "achievements")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected JPopupMenu getPopupMenu() {
				JPopupMenu menu = new JPopupMenu();
				for (JMenuItem item : makeTourList()) {
					menu.add(item);
				}
				return menu;
			}

			private JMenuItem[] makeTourList() {
				TourManager theManager = TourManager.getInstance();
				String[] keys = theManager.getTourkeys();
				JMenuItem[] toReturn = new JMenuItem[keys.length];
				for (int i = 0; i < keys.length; i++) {
					String relation = I18N.getMessage(I18N.getGUIBundle(), "gui.tour." + keys[i] + ".relation");
					String description = I18N.getMessage(I18N.getGUIBundle(), "gui.tour." + keys[i] + ".dropDownText",
							keys[i]);
					String iconName = I18N.getMessage(I18N.getGUIBundle(), "gui.tour." + keys[i] + ".icon");
					String progressBar = this.makeProgressPart(keys[i], theManager);
					String text = "<html><body><div style=\"width:200px\"><h3 style=\"padding-left:5px;color:black;\">"
							+ relation + "</h3><p style=\"padding-left:5px;\">" + description + "</p>" + progressBar
							+ "</div></body></html>";
					Icon icon = new ImageIcon(Tools.getResource(iconName), "");
					toReturn[i] = new JMenuItem(text, icon);
					toReturn[i].setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.tour.dropDown.tooltip"));
					// necessary to start the Tour effectively
					toReturn[i].setActionCommand(keys[i]);
					toReturn[i].addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							// the action Command was set to the Tour-key by creating the the
							// JMenuItems
							TourManager.getInstance().startTour(e.getActionCommand());
						}
					});
				}
				return toReturn;
			}

			private String makeProgressPart(String Tourkey, TourManager theManager) {
				int percent = theManager.getProgressInPercent(Tourkey);
				String progressIcon = "<img  src=\"dynicon://progress/200/8/" + percent + "\"/>";
				switch (percent) {
					case 0:
						return "<p style=\"padding-left:5px;\">"
								+ I18N.getMessage(I18N.getGUIBundle(), "gui.tour.dropDown.not_started") + "</p>";
					case 100:
						return "<p style=\"padding-left:5px;\">" + progressIcon + "&#160;" + " <img src=\""
								+ Tools.getResource(I18N.getMessage(I18N.getGUIBundle(), "gui.tour.dropDown.icon")) + "\"/>"
								+ "</p>";
					default:
						return "<p style=\"padding-left:5px;\">" + progressIcon + "&#160;" + percent + "%" + "</p>";
				}
			}
		};
		dropDownToReturn.setHorizontalTextPosition(SwingConstants.CENTER);
		dropDownToReturn.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {
				dropDownToReturn.setText("");
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				TourManager theManager = TourManager.getInstance();
				String[] keys = theManager.getTourkeys();
				int complete = 0;
				for (int i = 0; i < keys.length; i++) {
					if (theManager.getTourState(keys[i]) == TourState.COMPLETED) {
						complete++;
					}
				}
				complete = complete * 100 / keys.length;
				dropDownToReturn.setText("<html><div><b><font color=\"#00FF00\" >" + complete + "%</font></b></div></html>");
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				new TourChooser().setVisible(true);
			}
		});
		return dropDownToReturn;
	}
}
