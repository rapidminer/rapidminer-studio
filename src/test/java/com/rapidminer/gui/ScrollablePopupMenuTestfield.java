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
package com.rapidminer.gui;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Field;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.rapidminer.gui.tools.ScrollableJPopupMenu;


/**
 * Quick testing possibility to check the behavior of the {@link ScrollableJPopupMenu}, run this to see a {@link JFrame}
 * including several menus that each have two {@link JMenu submenus}, backed by a normal and a scrollable popup menu.
 * <p>
 * The first menu contains normal, pre-filled sub menus; the second fills its submenus as soon as the specified submenu
 * is selected (which prevents the first element from being selected on first mnemonic key pressed); the last menu
 * fills its submenus when itself is selected.
 *
 * @see DockableMenu
 *
 * @author Jan Czogalla
 * @since 8.2
 */
public class ScrollablePopupMenuTestfield {

	public static void main(String[] args) {
		JFrame testFrame = new JFrame("Testing popup menus");
		JMenuBar menuBar = new JMenuBar();
		// Simple pre-filled popup
		JMenu menu = new JMenu("Pre-filled");
		menu.setMnemonic('f');
		JMenu normalSub = new JMenu("Normal submenu");
		normalSub.setMnemonic('N');
		JMenu scrollableSub = createScrollableMenu("Scrollable submenu");
		scrollableSub.setMnemonic('S');
		fillWithGenericItems(normalSub);
		fillWithGenericItems(scrollableSub);
		menu.add(normalSub);
		menu.add(scrollableSub);
		menuBar.add(menu);
		// Fill on menu selection
		menu = new JMenu("Fill on selection");
		menu.setMnemonic('s');
		normalSub = new JMenu("Normal submenu");
		normalSub.setMnemonic('N');
		scrollableSub = createScrollableMenu("Scrollable submenu");
		scrollableSub.setMnemonic('S');
		MenuListener fillListener = new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				JMenu source = (JMenu) e.getSource();
				source.removeAll();
				fillWithGenericItems(source);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
				// ignore
			}

			@Override
			public void menuCanceled(MenuEvent e) {
				// ignore
			}
		};
		normalSub.addMenuListener(fillListener);
		scrollableSub.addMenuListener(fillListener);
		menu.add(normalSub);
		menu.add(scrollableSub);
		menuBar.add(menu);
		// Fill on parent selection
		menu = new JMenu("Fill on parent");
		menu.setMnemonic('p');
		normalSub = new JMenu("Normal submenu");
		normalSub.setMnemonic('N');
		scrollableSub = createScrollableMenu("Scrollable submenu");
		scrollableSub.setMnemonic('S');
		JMenu finalNormalSub = normalSub;
		JMenu finalScrollableSub = scrollableSub;
		menu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				finalNormalSub.removeAll();
				fillWithGenericItems(finalNormalSub);
				finalScrollableSub.removeAll();
				fillWithGenericItems(finalScrollableSub);
			}

			@Override
			public void menuDeselected(MenuEvent e) {

			}

			@Override
			public void menuCanceled(MenuEvent e) {

			}
		});
		menu.add(normalSub);
		menu.add(scrollableSub);
		menuBar.add(menu);
		testFrame.setJMenuBar(menuBar);
		testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		testFrame.setPreferredSize(new Dimension(600, 400));
		testFrame.pack();
		testFrame.setVisible(true);
	}

	private static void fillWithGenericItems(JMenu menu) {
		for (int i = 0; i < 10; i++) {
			JMenuItem item = menu.add("Entry " + i);
		}
	}

	private static JMenu createScrollableMenu(String name) {
		JMenu menu = new JMenu(name) {

			{
				ensure();
			}

			private void ensure() {
				Field popupMenuField;
				ScrollableJPopupMenu popupMenu = new ScrollableJPopupMenu(ScrollableJPopupMenu.SIZE_TINY);
				popupMenu.setInvoker(this);
				WinListener popupListener = new WinListener(popupMenu);
				// set listener first; if something goes wrong later, the private field will jsut be replaced in super method
				try {
					Field popupListenerField = JMenu.class.getDeclaredField("popupListener");
					popupListenerField.setAccessible(true);
					popupListenerField.set(this, popupListener);
				} catch (NoSuchFieldException | IllegalAccessException e) {
					Logger.getGlobal().warning("Popup could not be made scrollable: Error while setting listener");
					return;
				}
				// set popup second; if the set operation does not succeed, the private field will stay at null,
				// since this is invoked from the constructor before any call to the super method could be made.
				try {
					popupMenuField = JMenu.class.getDeclaredField("popupMenu");
					popupMenuField.setAccessible(true);
					popupMenuField.set(this, popupMenu);
				} catch (NoSuchFieldException | IllegalAccessException e) {
					Logger.getGlobal().warning("Popup could not be made scrollable: Error while setting popup");
				}
			}
		};
		return menu;
	}
}
