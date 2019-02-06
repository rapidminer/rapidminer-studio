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
package com.rapidminer.gui.look;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.swing.Popup;
import javax.swing.PopupFactory;


/**
 * Popup factory for OS X that forces heavyweight tooltips, so that they can overlap heavyweight windows (e.g. the
 * native Chromium browser).
 *
 * <p>
 * Taken from: https://stackoverflow.com/a/37185169/2333093
 * </p>
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public class HeavyweightOSXPopupFactory extends PopupFactory {

	private boolean couldEnforceHeavyWeightComponents = true;


	@Override
	public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException {
		enforceHeavyWeightComponents();
		return super.getPopup(owner, contents, x, y);
	}

	private void enforceHeavyWeightComponents() {
		if (!couldEnforceHeavyWeightComponents) {
			return;
		}

		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			try {
				Method setPopupTypeMethod = PopupFactory.class.getDeclaredMethod("setPopupType", int.class);
				setPopupTypeMethod.setAccessible(true);
				// 2 is the heavyweight constant
				setPopupTypeMethod.invoke(this, 2);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException aE) {
				// if it fails once, it will fail every time. Do not try again
				couldEnforceHeavyWeightComponents = false;
			}
			return null;
		});
	}
}
