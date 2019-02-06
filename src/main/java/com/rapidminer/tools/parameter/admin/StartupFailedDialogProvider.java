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
package com.rapidminer.tools.parameter.admin;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.lang.reflect.InvocationTargetException;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.look.borders.EmptyBorder;
import com.rapidminer.gui.look.ui.ButtonUI;


/**
 * Utility class that provides a simple RapidMiner UI independent error dialog
 *
 * @author Jonas Wilms-Pfau
 * @since 9.0
 */
final class StartupFailedDialogProvider {

	/**
	 * Prevent utility class instantiation.
	 */
	private StartupFailedDialogProvider() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Displays an error dialog if possible, otherwise does nothing
	 *
	 * @param pce
	 * 		the exception containing the dialog message and title
	 */
	public static void showErrorMessage(ProvidedConfigurationException pce) {
		if (RapidMiner.getExecutionMode() == RapidMiner.ExecutionMode.UI && !GraphicsEnvironment.isHeadless()) {
			try {
				SwingUtilities.invokeAndWait(() -> {
					Image rapidMinerLogo = makeIcon("com/rapidminer/resources/icons/64/rapidminer_studio.png").getImage();
					Icon errorIcon = makeIcon("com/rapidminer/resources/icons/48/error.png");
					Icon okIcon = makeIcon("com/rapidminer/resources/icons/16/check.png");
					UIManager.getDefaults().put("OptionPane.buttonOrientation", SwingConstants.RIGHT);
					JButton button = new JButton("Okay");
					button.setUI(new ButtonUI());
					button.setBorder(new EmptyBorder());
					button.setIcon(okIcon);
					JOptionPane panel = new JOptionPane(pce.getDialogMessage(), JOptionPane.ERROR_MESSAGE,
							JOptionPane.DEFAULT_OPTION, errorIcon,
							new JButton[]{button}, button);
					JDialog dialog = panel.createDialog(pce.getDialogTitle());
					button.addActionListener(actionEvent -> dialog.dispose());
					dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					dialog.setIconImage(rapidMinerLogo);
					dialog.setVisible(true);
				});
			} catch (InterruptedException | InvocationTargetException e) {
				// do nothing
			}
		}
	}

	/**
	 * Creates an ImageIcon from a resource
	 *
	 * @param path
	 * 		the absolute path to the resource
	 * @return not {@code null}, but the ImageIcon might be empty
	 */
	private static ImageIcon makeIcon(String path) {
		try {
			return new ImageIcon(ImageIO.read(ClassLoader.getSystemResource(path)));
		} catch (Exception e) {
			return new ImageIcon();
		}
	}

}
