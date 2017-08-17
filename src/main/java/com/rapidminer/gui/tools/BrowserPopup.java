/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui.tools;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;


/**
 * Displays a Browser popup with sliding animations
 *
 * <p>
 * The HTML content must contain an initialize() JS method
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5.0
 *
 */
public class BrowserPopup extends JDialog implements Supplier<String> {

	/**
	 * Provides functions invokable within the JavaScript of the web page displayed in the webview.
	 */
	public final class CTACallbacks {

		/**
		 * Opens the given url in the system browser.
		 *
		 * @param url
		 * 		the url to open
		 */
		public void openLink(String url) {
			if (url != null) {
				closeWithReason(url);
				// It is freezing on Linux otherwise
				SwingUtilities.invokeLater(() -> {
					RMUrlHandler.openInBrowser(url);
				});
			}
		}

		public void close() {
			closeWithReason(Reason.CLOSED);
		}

		public void maybeLater() {
			closeWithReason(Reason.LATER);
		}

		public void closeWithReason(String reason) {
			// Store the reason
			reasonQueue.offer(reason);
			if (!closed.compareAndSet(false, true)) {
				return;
			}
			// Slide out & dispose
			Platform.runLater(() -> {
				if (webView != null) {
					ImageView imageView = new ImageView(webView.snapshot(null, null));
					animatedPane.getChildren().set(0, imageView);
					dropShadow.setBlurType(BlurType.TWO_PASS_BOX);
				}
				TranslateTransition slideOut = new TranslateTransition(Duration.millis(TRANSLATION_MS), animatedPane);
				slideOut.setInterpolator(Interpolator.EASE_IN);
				slideOut.setByX(translationDistance);
				slideOut.setOnFinished(e -> dispose());
				slideOut.play();
			});
		}

		@Override
		public String toString() {
			return "Callback interface of RapidMiner Studio. Available functions: "
					+ "openLink('url'), maybeLater(),closeWithReason('reason'), close()";
		}

	}

	/**
	 * Predefined reasons for the closing of the CTA window.
	 */
	private final class Reason {
		public static final String CLOSED = "closed";
		public static final String UNKNOWN = "unknown";
		public static final String LATER = "later";
		public static final String CANCELED = "canceled";
		public static final String JS_INVALID = "js_invalid";
		public static final String FAILED_LOADING = "failed_loading";
		public static final String TIMEOUT = "loading_timeout";

		private Reason() {
			throw new UnsupportedOperationException("Static utility class");
		}
	}

	private static final long serialVersionUID = 1L;

	/** the timeout (in milliseconds) until the loading fails */
	private static final int TIMEOUT = 15_000;

	private static final int DEFAULT_WIDTH = 500;
	private static final int DEFAULT_HEIGHT = 200;

	/**
	 * Check if the OS supports transparency, if not disable all the animation and shadow and show a
	 * simple dialog
	 */
	private static final boolean MODERN_UI = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration().isTranslucencyCapable();

	/** Mac OS X requires the fonts very early */
	private static final String[] FONT_NAMES = new String[]{"OpenSans.ttf", "OpenSans-Bold.ttf", "OpenSans-BoldItalic.ttf",
			"OpenSans-ExtraBold.ttf", "OpenSans-ExtraBoldItalic.ttf", "OpenSans-Italic.ttf", "OpenSans-Light.ttf",
			"OpenSans-LightItalic.ttf", "OpenSans-Semibold.ttf", "OpenSans-SemiboldItalic.ttf", "ionicons.ttf"};
	/** Loading fonts - do not remove! */
	private static final Font[] FONTS = Stream.of(FONT_NAMES)
			.map(font -> Font.loadFont(
					BrowserPopup.class.getResource("/com/rapidminer/resources/fonts/" + font).toExternalForm(), 12))
			.toArray(Font[]::new);

	/** Default Mac Dialog shadow */
	private static final String MAC_OS_X_SHADOW = "Window.shadow";

	private static final int TRANSLATION_MS = MODERN_UI ? 1_000 : 1;

	// This is required for the slide-in animation
	private static final int RIGHT_MARGIN = MODERN_UI ? 100 : 0;
	// This is required to display the shadow
	private static final int BORDER_PADDING = MODERN_UI ? 25 : 0;
	// The shadow is slightly moved downwards
	private static final int SHADOW_OFFSET_X = 0;
	private static final int SHADOW_OFFSET_Y = MODERN_UI ? 2 : 0;
	private static final Color SHADOW_COLOR = Color.color(0, 0, 0, 0.12);
	private static final int BORDER_SIZE = MODERN_UI ? 1 : 0;
	private static final Background BORDER_BG = MODERN_UI
			? new Background(new BackgroundFill(Color.grayRgb(230), null, null)) : Background.EMPTY;
	/** ionicons X icon */
	private static final String CLOSE_SYMBOL = "\uf2d7";
	private static final double CLOSE_RADIUS = 11;

	private static final Font CLOSE_FONT = Font.font("Ionicons", FontWeight.EXTRA_BOLD, 18);
	private static final Background CLOSE_BG = new Background(new BackgroundFill(Color.grayRgb(230), null, null));

	private static final Background CLOSE_BG_HOVER = new Background(new BackgroundFill(Color.grayRgb(154), null, null));

	/** The distance of the close button to the upper right side */
	private static final Insets CLOSE_MARGIN = new Insets(10, 15, 0, 0);

	/** Close with Alt+F4 or the close button in decorated mode */
	private static final int CLOSE_OPERATION = MODERN_UI ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE;

	/** Transparent color in overlay mode, white in dialog mode */
	private static final java.awt.Color BG_COLOR = MODERN_UI ? new java.awt.Color(0, 0, 0, 0) : java.awt.Color.WHITE;

	private static final String JS_ROOT = "window";
	private static final String JS_NAME = "CTA";
	private static final String JS_INITIALIZE = "initialize()";

	/** This is required since 8u112 due to the changes of https://bugs.openjdk.java.net/browse/JDK-8089681 */
	private final CTACallbacks ctaCallbacks = new CTACallbacks();

	/** Hack to keep translucent window alive after it was iconified */
	private final transient WindowListener transparencyFix = new WindowAdapter() {

		@Override
		public void windowActivated(WindowEvent e) {
			setBackground(BG_COLOR);
		}

		@Override
		public void windowIconified(WindowEvent e) {
			setBackground(java.awt.Color.WHITE);
		}
	};

	/** Distance required for the slide-in / slide-out animation */
	private final int translationDistance;

	private int width;
	private int height;

	// Is the bubble ready to display
	private boolean isLoaded = false;
	// Was this bubble already animated?
	private boolean wasAnimated = false;

	/** See if the dialog is closed */
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/** Queue used to store the reason */
	private BlockingQueue<String> reasonQueue = new ArrayBlockingQueue<>(1);

	private final JFXPanel contentPanel;

	private final Timer failTimer = new Timer(TIMEOUT, (ActionEvent ev) -> {
		Platform.runLater(() -> {
					ctaCallbacks.closeWithReason(Reason.TIMEOUT);
					logCtaFailure(Reason.TIMEOUT);
				}
		);
	});

	/** Actual HTML content of the popup */
	private final String html;

	/** Animated Stack Pane */
	private final StackPane animatedPane = new StackPane();

	/** Used to decrease the quality on transition */
	private transient WebView webView;

	/** Used to decrease shadow quality for transition */
	private final transient DropShadow dropShadow = new DropShadow(BORDER_PADDING, SHADOW_OFFSET_X, SHADOW_OFFSET_Y, SHADOW_COLOR);

	/**
	 * Creates a 500x200 Browser Popup
	 *
	 * @param html
	 */
	public BrowserPopup(String html) {
		this(html, DEFAULT_WIDTH, DEFAULT_HEIGHT);
	}

	/**
	 * @param onboardingDialog
	 * 		the parent dialog
	 */
	public BrowserPopup(String html, int width, int height) {
		super(RapidMinerGUI.getMainFrame());
		this.width = width;
		this.height = height;

		translationDistance = width + RIGHT_MARGIN + BORDER_PADDING;

		// Hack to keep translucent window alive after it was iconified
		RapidMinerGUI.getMainFrame().addWindowListener(transparencyFix);

		this.setLocationRelativeTo(RapidMinerGUI.getMainFrame());
		this.setDefaultCloseOperation(CLOSE_OPERATION);
		this.html = html;
		this.setUndecorated(MODERN_UI);
		// Make it transparent
		getRootPane().setOpaque(!MODERN_UI);
		getRootPane().putClientProperty(MAC_OS_X_SHADOW, Boolean.FALSE);
		getContentPane().setBackground(BG_COLOR);
		getRootPane().setBackground(BG_COLOR);
		setBackground(BG_COLOR);
		this.contentPanel = new JFXPanel();
		this.contentPanel.setOpaque(!MODERN_UI);
		this.contentPanel.setBackground(BG_COLOR);
		SwingTools.disableClearType(contentPanel);
		this.add(contentPanel);
		this.getContentPane()
				.setPreferredSize(new Dimension(width + RIGHT_MARGIN + BORDER_PADDING, height + 2 * BORDER_PADDING));
		this.createScene();
		this.pack();
	}

	/**
	 * Waits up to 4 hours for the user to click a button in the BrowserPopup
	 */
	@Override
	public String get() {
		String reason;
		if (closed.get() && reasonQueue.isEmpty()) {
			reason = Reason.CLOSED;
		} else {
			try {
				// Give the user 4 hours to click the button
				reason = reasonQueue.poll(4, TimeUnit.HOURS);
				if (reason != null) {
					// reoffer the reason
					reasonQueue.offer(reason);
				} else {
					reason = Reason.UNKNOWN;
				}
			} catch (InterruptedException e) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tools.BrowserPopup.result.interupted", e);
				reason = Reason.UNKNOWN;
				Thread.currentThread().interrupt();
			}
		}
		return reason;
	}

	@Override
	public void setVisible(boolean visible) {
		boolean wasVisible = this.isVisible();
		//Don't steal the focus from the user
		this.setFocusableWindowState(false);
		super.setVisible(visible);
		this.setFocusableWindowState(true);
		if (!wasVisible && visible) {
			slideIn();
		}
	}

	@Override
	public void dispose() {
		closed.set(true);
		SwingUtilities.invokeLater(() -> {
			try {
				RapidMinerGUI.getMainFrame().removeWindowListener(transparencyFix);
				super.dispose();
			} catch (NullPointerException npe) {
				/**
				 * This is known to happen on the JFXPanel, but we can recover from this
				 *
				 * https://bugs.openjdk.java.net/browse/JDK-8089371
				 * https://bugs.openjdk.java.net/browse/JDK-8098836
				 */
				LogService.getRoot().log(Level.INFO, "com.rapidminer.gui.tools.BrowserPopup.dispose.failed", npe);
				dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSED));
				setVisible(false);
			}
		});
	}

	/**
	 * Creates scene with the webview and the close button
	 */
	private void createScene() {
		// make it possible to create the dialog a second time
		Platform.setImplicitExit(false);

		Platform.runLater(() -> {
					// Transparent root pane
					BorderPane rootPane = new BorderPane();
					rootPane.setBackground(Background.EMPTY);
					// Add padding to see the Shadow
					rootPane.setPadding(new Insets(BORDER_PADDING - SHADOW_OFFSET_Y, RIGHT_MARGIN, BORDER_PADDING + SHADOW_OFFSET_Y,
							BORDER_PADDING));
					rootPane.setCenter(animatedPane);

					// Enable cache for smoother animation
					animatedPane.setCache(true);
					animatedPane.setCacheHint(CacheHint.SPEED);
					animatedPane.setCacheShape(true);
					/** Just fill the whole pane and add a background */
					animatedPane.setPadding(new Insets(BORDER_SIZE));
					animatedPane.setBackground(BORDER_BG);

					// Reduce dropShadow Quality
					dropShadow.setBlurType(BlurType.TWO_PASS_BOX);
					animatedPane.setEffect(dropShadow);

					webView = new WebView();
					Button closeButton = createCloseButton();
					// Show button only on hover
					closeButton.opacityProperty()
							.bind(Bindings.when(webView.hoverProperty().or(closeButton.hoverProperty())).then(1).otherwise(0));

					// Reduce WebView Quality
					webView.setContextMenuEnabled(false);
					webView.setFontSmoothingType(FontSmoothingType.GRAY);
					webView.setCache(true);
					webView.setCacheHint(CacheHint.SPEED);
					// Translate the animatedPane out of sight
					animatedPane.setTranslateX(translationDistance);
					// The webView is overlayed by the close button
					animatedPane.getChildren().addAll(webView, closeButton);
					// move close button to the upper right corner
					StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
					StackPane.setMargin(closeButton, CLOSE_MARGIN);
					// Create a transparent root scene
					Scene rootScene = new Scene(rootPane, Color.TRANSPARENT);
					contentPanel.setScene(rootScene);
					// prepare the browser
					configureWebEngine(webView);
				}

		);

	}

	/**
	 * Creates a close button
	 *
	 * Round button with a X inside
	 *
	 * @return
	 */
	private Button createCloseButton() {
		Button closeButton = new Button(CLOSE_SYMBOL);
		closeButton.setCancelButton(true);
		closeButton.setDefaultButton(false);
		closeButton.setShape(new Circle(CLOSE_RADIUS));
		closeButton.setMinSize(2 * CLOSE_RADIUS, 2 * CLOSE_RADIUS);
		closeButton.setBorder(Border.EMPTY);
		closeButton.setBackground(CLOSE_BG);
		closeButton.setFont(CLOSE_FONT);
		closeButton.setPadding(Insets.EMPTY);
		closeButton.setTextFill(Color.WHITE);
		closeButton.setVisible(MODERN_UI);
		// Hover effect
		closeButton.backgroundProperty()
				.bind(Bindings.when(closeButton.hoverProperty()).then(CLOSE_BG_HOVER).otherwise(CLOSE_BG));
		// Close action
		closeButton.setOnAction((e) -> ctaCallbacks.closeWithReason(Reason.CLOSED));
		return closeButton;
	}

	/**
	 * Configures the engine behind the webView.
	 */
	private void configureWebEngine(WebView webView) {
		final WebEngine webEngine = webView.getEngine();

		webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {

			private boolean hasFallbackTimer = false;

			@Override
			public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
				if (oldState == newState) {
					return;
				}

				switch (newState) {
					case SUCCEEDED:
						JSObject win = (JSObject) webEngine.executeScript(JS_ROOT);
						win.setMember(JS_NAME, ctaCallbacks);
						try {
							// Check JS initialization
							webEngine.executeScript(JS_INITIALIZE);
							// Stop the timer
							failTimer.stop();
							// Translate the webView in
							isLoaded = true;
							slideIn();
						} catch (RuntimeException e) {
							// initialize method does not exist, fail silently
							failTimer.stop();
							ctaCallbacks.closeWithReason(Reason.JS_INVALID);
							logCtaFailure(Reason.JS_INVALID);
						}
						break;
					case FAILED:
						failTimer.stop();
						ctaCallbacks.closeWithReason(Reason.FAILED_LOADING);
						logCtaFailure(Reason.FAILED_LOADING);
						break;
					case RUNNING:
						if (!hasFallbackTimer) {
							hasFallbackTimer = true;
							failTimer.setRepeats(false);
							failTimer.start();
						}
						break;
					case CANCELLED:
						ctaCallbacks.closeWithReason(Reason.CANCELED);
						logCtaFailure(Reason.CANCELED);
						break;
					case READY:
					case SCHEDULED:
					default:
						break;

				}
			}

		});
		webEngine.loadContent(html);
	}

	/**
	 * Slide In animation
	 */
	private void slideIn() {
		if (this.isVisible() && isLoaded && !wasAnimated) {
			Platform.runLater(() -> {
				animatedPane.setTranslateX(translationDistance);
				TranslateTransition slideIn = new TranslateTransition(Duration.millis(TRANSLATION_MS), animatedPane);
				slideIn.setInterpolator(Interpolator.EASE_OUT);
				slideIn.setByX(-1 * translationDistance);
				slideIn.setOnFinished(e -> Platform.runLater(() -> {
					wasAnimated = true;
					if (webView != null) {
						dropShadow.setBlurType(BlurType.GAUSSIAN);
						webView.setFontSmoothingType(FontSmoothingType.LCD);
						webView.setCacheHint(CacheHint.QUALITY);
					}
				}));
				slideIn.play();
			});
		}
	}

	/**
	 * Logs CTA externalization statistics.
	 */
	private void logCtaFailure(String value) {
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_CTA,
				ActionStatisticsCollector.VALUE_CTA_FAILURE, value);
	}

}
