package com.rapidminer.gui.properties;

import java.awt.Dialog.ModalityType;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BooleanSupplier;

import javax.swing.JPanel;

import com.rapidminer.RapidMiner;
import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder;
import com.rapidminer.gui.tools.dialogs.ButtonDialog.ButtonDialogBuilder.DefaultButtons;
import com.rapidminer.license.StudioLicenseConstants;
import com.rapidminer.tools.ParameterService;


/**
 * Listener for the {@link SettingsDialog} that checks if additional permissions are activated. In
 * case of an activation a dialog with the warning is shown, but only once per session.
 *
 * @author Joao Pedro Pinheiro
 * @since 7.4
 */

public class AdditionalPermissionsListener extends WindowAdapter {

	/** stores if the beta features are activated when the listener is constructed */
	private boolean permissionsActiveBefore;

	/**
	 * Constructs a Listener for extra permissions activation
	 */
	AdditionalPermissionsListener() {
		permissionsActiveBefore = Boolean.parseBoolean(
				ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_ADDITIONAL_PERMISSIONS));
	}

	@Override
	public void windowClosed(WindowEvent e) {
		boolean permissionsActiveNow = Boolean.parseBoolean(
				ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_ADDITIONAL_PERMISSIONS));

		if (!permissionsActiveBefore && permissionsActiveNow) {
			BooleanSupplier isAllowed = () -> ProductConstraintManager.INSTANCE.getActiveLicense()
					.getPrecedence() >= StudioLicenseConstants.UNLIMITED_LICENSE_PRECEDENCE
					|| ProductConstraintManager.INSTANCE.isTrialLicense();
			if (ProductConstraintManager.INSTANCE.isInitialized() && isAllowed.getAsBoolean()) {
				ButtonDialog dialog = new ButtonDialogBuilder("additional_permissions")
						.setOwner(ApplicationFrame.getApplicationFrame())
						.setButtons(DefaultButtons.OK_BUTTON, DefaultButtons.CANCEL_BUTTON)
						.setModalityType(ModalityType.APPLICATION_MODAL).setContent(new JPanel(), ButtonDialog.DEFAULT_SIZE)
						.build();
				dialog.getRootPane().getDefaultButton().setText("Grant");
				dialog.getRootPane().getDefaultButton().setMnemonic('G');
				dialog.setVisible(true);
				if (!dialog.wasConfirmed()) {
					ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_ADDITIONAL_PERMISSIONS,
							String.valueOf(false));
					ParameterService.saveParameters();
				}
			} else {
				ParameterService.setParameterValue(RapidMiner.PROPERTY_RAPIDMINER_UPDATE_ADDITIONAL_PERMISSIONS,
						String.valueOf(false));
				ParameterService.saveParameters();

				ButtonDialog smallLicenseDialog = new ButtonDialogBuilder("small_license")
						.setOwner(ApplicationFrame.getApplicationFrame()).setButtons(DefaultButtons.OK_BUTTON)
						.setModalityType(ModalityType.APPLICATION_MODAL).setContent(new JPanel(), ButtonDialog.MESSAGE)
						.build();
				smallLicenseDialog.setVisible(true);
			}
		}
	}

}
