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

import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.util.TestExecutionContextImpl;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * The action that triggers when a {@link ConnectionInformation} is tested in the connection UI.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class TestConnectionAction extends ResourceAction {

	private static final ImageIcon CANCEL_ICON = SwingTools.createIcon("24/" + ConnectionI18N.getConnectionGUIMessage("test.cancel.icon"));
	private static final String CANCEL_TESTING = ConnectionI18N.getConnectionGUILabel("test.cancel_testing");

	private static final TestResult NOT_SUPPORTED = new TestResult(TestResult.ResultType.NOT_SUPPORTED, "test.not_registered", null);
	private static final TestResult TEST_RUNNING = new TestResult(TestResult.ResultType.NONE, "test.running", null);

	private final transient Supplier<ConnectionInformation> connectionSupplier;
	private final transient Consumer<TestResult> setTestResult;
	/** Boolean indicating if it's testing or not */
	private final AtomicBoolean isTesting = new AtomicBoolean(false);
	/** Current call count */
	private final AtomicInteger callCount = new AtomicInteger(0);

	private final String name;
	private final transient Icon icon;

	private transient ProgressThread testThread;


	/**
	 * Creates a new test connection action
	 *
	 * @param connection supplier for a connection to test
	 * @param setResult consumer that accepts the test results
	 */
	public TestConnectionAction(Supplier<ConnectionInformation> connection, Consumer<TestResult> setResult) {
		super(false, "test_connection");
		this.connectionSupplier = connection;
		this.setTestResult = setResult;
		this.name = (String) getValue(NAME);
		this.icon = (Icon) getValue(LARGE_ICON_KEY);
	}

	@Override
	protected void loggedActionPerformed(ActionEvent e) {
		final int currentCallCount = callCount.incrementAndGet();
		final ProgressThread currentTestThread = testThread;
		if (isTesting.compareAndSet(true, false)) {
			resetIcons();
			if (currentTestThread != null) {
				currentTestThread.cancel();
			}
			testThread = null;
			setTestResult.accept(null);
			return;
		} else if (!isTesting.compareAndSet(false, true)) {
			return;
		}

		SwingTools.invokeAndWait(() -> {
			setTestResult.accept(TEST_RUNNING);
			putValue(NAME, CANCEL_TESTING);
			putValue(LARGE_ICON_KEY, CANCEL_ICON);
			firePropertyChange("icons", null, null);
		});

		testThread = new ProgressThread("test_connection") {
			@Override
			public void run() {
				TestResult testResult = null;
				ConnectionInformation connection = null;
				try {
					connection = connectionSupplier.get();
					checkCancelled();
					testResult = getTestResult(connection, this);
					logTestResult(testResult, connection);
					checkCancelled();
				} catch (ProgressThreadStoppedException ptse) {
					testResult = null;
					throw ptse;
				} catch (Throwable t) {
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.connection.gui.actions.TestConnectionAction.testing_failed", t);
					testResult = TestResult.failure("test.unexpected_error", t.getMessage());
					logTestResult(testResult, connection);
				} finally {
					if (currentCallCount == callCount.get() && isTesting.compareAndSet(true, false)) {
						resetIcons();
						Optional.ofNullable(testResult).ifPresent(setTestResult);
					}
				}
			}
		};
		testThread.start();
}

	/**
	 * Logs the testResult for the connection.
	 */
	private void logTestResult(TestResult testResult, ConnectionInformation connection) {
		String type = connection != null ? connection.getConfiguration().getType() : "unknown";
		String logResult = testResult.getType() + ActionStatisticsCollector.ARG_SPACER + testResult.getMessageKey();
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_CONNECTION_TEST, type, logResult);
	}

	/**
	 * Tests the given connection
	 *
	 * @param connection
	 * 		the connection to test
	 * @param thread
	 * 		the progress thread in which the test is executed
	 * @return the test result
	 */
	public static TestResult getTestResult(ConnectionInformation connection, ProgressThread thread) {
		if (connection == null) {
			return TestResult.nullable();
		}

		String type = connection.getConfiguration().getType();
		if (!ConnectionHandlerRegistry.getInstance().isTypeKnown(type)) {
			return NOT_SUPPORTED;
		}
		ConnectionHandler handler = ConnectionHandlerRegistry.getInstance().getHandler(type);

		// check validation to make sure we catch basic errors before actually testing
		ValidationResult valResult = handler.validate(connection);
		if (valResult.getType() == ValidationResult.ResultType.FAILURE) {
			return valResult;
		}

		ValidationResult validationResult = SaveConnectionAction.checkValueProviders(connection);
		if (validationResult.getType() == ValidationResult.ResultType.FAILURE) {
			return validationResult;
		}

		// only if validate succeeds, go to actual test
		TestResult result = handler.test(new TestExecutionContextImpl<>(connection, thread));
		if (result == null) {
			return NOT_SUPPORTED;
		}
		return result;
	}

	/**
	 * Resets the icons to the regular icons
	 */
	private void resetIcons() {
		SwingTools.invokeAndWait(() -> {
			putValue(NAME, name);
			putValue(LARGE_ICON_KEY, icon);
			firePropertyChange("icons", null, null);
		});
	}
}
