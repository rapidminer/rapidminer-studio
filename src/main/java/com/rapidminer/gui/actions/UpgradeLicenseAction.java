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
package com.rapidminer.gui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.SwingUtilities;

import com.rapidminer.core.license.ProductLinkRegistry;
import com.rapidminer.gui.tools.MultiSwingWorker;
import com.rapidminer.gui.tools.NotificationPopup;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RMUrlHandler;


/**
 * This action starts the license upgrade process.
 * 
 * @author Marco Boeck
 * 
 */
public class UpgradeLicenseAction extends ResourceAction {

	private static final long serialVersionUID = 1066532653166353016L;

	/** the uri for the license upgrade */
	private static final String URI_LICENSE_UPGRADE = I18N.getGUILabel("license.url");
	private String productId;

	/**
	 * Creates a new {@link UpgradeLicenseAction} instance.
	 */
	public UpgradeLicenseAction() {
		this(null);
	}

	/**
	 * Creates a new {@link UpgradeLicenseAction} instance.
	 *
	 * @param productId
	 * 		the product id that caused the action
	 */
	public UpgradeLicenseAction(String productId) {
		super(false, "upgrade_license");
		this.productId = productId;
	}

	@Override
	public void loggedActionPerformed(final ActionEvent e) {
		createUpgradeWorker().start();
		if (e != null && e.getSource() != null) {
			NotificationPopup popup = (NotificationPopup) SwingUtilities.getAncestorOfClass(NotificationPopup.class,
					(Component) e.getSource());
			if (popup != null) {
				popup.dispose();
			}
		}
	}

	/**
	 * Creates a new upgrade {@link MultiSwingWorker}.
	 * 
	 * @return
	 */
	private MultiSwingWorker<Void, Void> createUpgradeWorker() {
		return new MultiSwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				RMUrlHandler.handleUrl(ProductLinkRegistry.PURCHASE.get(productId, URI_LICENSE_UPGRADE));
				return null;
			}

			@Override
			protected void done() {
				try {
					// see if anything failed
					get();
				} catch (ExecutionException | InterruptedException e) {
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.license.gui.actions.UpgradeLicenseAction.failed_to_init_upgrade");
				}
			}
		};
	}
}
