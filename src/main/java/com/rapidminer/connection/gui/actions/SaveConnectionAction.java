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
package com.rapidminer.connection.gui.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.gui.ConnectionGUI;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandler;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Action for saving a connection
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class SaveConnectionAction extends ResourceAction {

	/**
	 * The progress thread id consists of {@value #PROGRESS_THREAD_ID_PREFIX} followed by the connection location.
	 */
	public static final String PROGRESS_THREAD_ID_PREFIX = "saving_connection";

	private final Window parent;
	private final transient ConnectionGUI gui;
	private final transient Supplier<RepositoryLocation> locationSupplier;
	private final Consumer<ValidationResult> testResultConsumer;


	public SaveConnectionAction(Window parent, ConnectionGUI gui, Supplier<RepositoryLocation> location, Consumer<ValidationResult> testResultConsumer, String i18nKey) {
		super(i18nKey);
		this.parent = parent;
		this.gui = gui;
		this.locationSupplier = location;
		this.testResultConsumer = testResultConsumer;
	}

	@Override
	protected void loggedActionPerformed(ActionEvent e) {
		if(!gui.preSaveCheck()){
			return;
		}
		final ConnectionInformation connection = gui.getConnection();
		if (!validOrSaveAnyway(connection)) {
			return;
		}
		parent.setVisible(false);
		final RepositoryLocation location = locationSupplier.get();
		ProgressThread progressThread = new ProgressThread(PROGRESS_THREAD_ID_PREFIX, false, location.toString()) {
			@Override
			public void run() {
				ConnectionInformationContainerIOObject ioobject = new ConnectionInformationContainerIOObject(connection);
				try {
					RepositoryManager.getInstance(null).store(ioobject, location, null);
					SwingTools.invokeLater(parent::dispose);
					logConnectionStatus(ActionStatisticsCollector.ARG_SUCCESS, connection);
				} catch (RepositoryException e) {
					SwingTools.invokeLater(() -> parent.setVisible(true));
					SwingTools.showSimpleErrorMessage(parent, "saving_connection_failed", e, connection.getConfiguration().getName(), location.toString(), e.getMessage());
					logConnectionStatus(ActionStatisticsCollector.ARG_FAILED, connection);
				}
			}

			@Override
			public String getID() {
				return super.getID() + location;
			}
		};
		progressThread.setIndeterminate(true);
		progressThread.start();
	}

	/**
	 * Logs the connection and the status of the saving (success/failed).
	 */
	private void logConnectionStatus(String status, ConnectionInformation connection) {
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_CONNECTION,
				connection.getConfiguration().getType(), status + ActionStatisticsCollector.ARG_SPACER+
				ActionStatisticsCollector.getConnectionInjections(connection.getConfiguration()));
	}

	/**
	 * Verifies the given connection, displays a save anyway dialog if necessary
	 *
	 * @param connection
	 * 		the connection to verify
	 * @return {@code true} if the connection is valid, or the user decided to save anyway
	 */
	private boolean validOrSaveAnyway(ConnectionInformation connection) {
		ValidationResult valueProviderValidation = checkValueProviders(connection);
		String type = connection.getConfiguration().getType();
		ValidationResult connectionValidation = ValidationResult.nullable();
		if (ConnectionHandlerRegistry.getInstance().isTypeKnown(type)) {
			connectionValidation = ConnectionHandlerRegistry.getInstance().getHandler(type).validate(connection);
		}
		ValidationResult result = ValidationResult.merge(valueProviderValidation, connectionValidation);

		// notify UI about validation results
		testResultConsumer.accept(result);

		if (!result.getType().equals(TestResult.ResultType.FAILURE)) {
			return true;
		}

		String message = ConnectionI18N.getConnectionGUIMessageOrNull(result.getMessageKey(), result.getArguments());
		if (message == null) {
			message = ConnectionI18N.getConnectionGUIMessage("validation.failed");
		}
		final String i18nMessage = message;

		return SwingTools.invokeAndWaitWithResult(() -> {
			ConfirmDialog dialog = new ConfirmDialog(parent, "save_invalid_connection", ConfirmDialog.OK_CANCEL_OPTION, false, i18nMessage) {
				@Override
				protected JButton makeOkButton() {
					return makeOkButton("connection.save_anyway");
				}

				@Override
				protected JButton makeCancelButton() {
					return makeCancelButton("connection.back_to_editing");
				}
			};
			dialog.setVisible(true);
			return dialog.wasConfirmed();
		});
	}

	/**
	 * Check if the {@link ValueProvider ValueProviders} used by the given {@link ConnectionInformation} contain setup
	 * errors and collects those.
	 *
	 * @param connection
	 * 		the connection that needs to be checked for value provider misconfiguration
	 * @return the pessimistic merge result for all the checked value providers.
	 * @see ValidationResult#merge(ValidationResult...)
	 */
	static ValidationResult checkValueProviders(ConnectionInformation connection) {
		Objects.requireNonNull(connection);

		final List<ValidationResult> results = new ArrayList<>();
		for (ValueProvider valueProvider : connection.getConfiguration().getValueProviders()) {
			if (ValueProviderHandlerRegistry.getInstance().isTypeKnown(valueProvider.getType())) {
				ValueProviderHandler handler = ValueProviderHandlerRegistry.getInstance().getHandler(valueProvider.getType());
				ValidationResult validate = handler.validate(valueProvider, connection);
				results.add(validate);
			} else {
				results.add(ValidationResult.failure("save_unknown_vp", null, valueProvider.getType()));
			}
		}

		return ValidationResult.merge(results.toArray(new ValidationResult[0]));
	}

}
