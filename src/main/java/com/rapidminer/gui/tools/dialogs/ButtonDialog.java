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
package com.rapidminer.gui.tools.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.RapidMiner;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthEditorPane;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Dialog that provides some helper methods to create buttons. Automatically registers accelerators
 * and action listeners. Override {@link #ok()}, {@link #cancel()} and {@link #close()} to customize
 * the behaviour.
 *
 * The user can query if the ok button was pressed ({@link #wasConfirmed}).
 *
 * @author Simon Fischer, Marco Boeck
 */
public class ButtonDialog extends JDialog {

	/**
	 * A builder for {@link ButtonDialog}s. After calling all relevant setters, call
	 * {@link #build()} to create the actual dialog instance.
	 * <p>
	 * <strong>Attention:</strong> Without setting an explicit parent via {@link #setOwner(Window)},
	 * modal dialogs will display inconsistent behavior and sometimes appear behind other dialogs.
	 * This can be a big problem as the user will not find the dialog, yet he is blocked from
	 * interacting with the rest of the program!
	 * </p>
	 *
	 *
	 * @author Marco Boeck
	 * @since 6.5.0
	 *
	 */
	public static class ButtonDialogBuilder {

		/**
		 * Default buttons which can automatically added to the button dialog.
		 *
		 */
		public enum DefaultButtons {
			/** labelled Ok, sets the status to confirmed */
			OK_BUTTON,

			/** labelled Cancel, sets the status to not confirmed */
			CANCEL_BUTTON,

			/** labelled Close, sets the status to not confirmed */
			CLOSE_BUTTON;
		}

		/** the i18n key */
		private String key;

		/** the optional i18n arguments */
		private Object[] i18nArgs;

		/** the modality type which should be used for the dialog */
		private ModalityType modalityType = ModalityType.MODELESS;

		/** the owner window for the dialog or {@code null} */
		private Window owner;

		/** the graphics config to use for the dialog or {@code null} */
		private GraphicsConfiguration graphicsConfig;

		/** if {@code true}, the layout is done by the builder as well */
		private boolean doLayout = false;

		/** the main component if layout should be done */
		private JComponent mainComponent;

		/** the size if layout should be done */
		private int size;

		/** the buttons if layout should be done */
		private AbstractButton[] buttons;

		/** default buttons if layout should be done */
		private DefaultButtons[] defaultButtons;

		/**
		 * Create a new builder for a {@link ButtonDialog}.
		 *
		 * @param key
		 *            the i18n key used for the properties gui.dialog.-key-.title and
		 *            gui.dialog.-key-.icon
		 */
		public ButtonDialogBuilder(final String key) {
			if (key == null || key.trim().isEmpty()) {
				throw new IllegalArgumentException("key must not be null or empty!");
			}

			this.key = key;
		}

		/**
		 * Sets the parent window for the dialog. Can be a {@link Dialog}, can be a {@link Frame},
		 * can be a {@link Window}. This is used to determine modality order. By default, no parent
		 * is set.
		 *
		 * @param owner
		 *            the parent of the dialog
		 * @return the builder itself, never {@code null}
		 */
		public ButtonDialogBuilder setOwner(final Window owner) {
			this.owner = owner;
			return this;
		}

		/**
		 * Sets optional i18n arguments which are used to replace placeholders. By default, no
		 * arguments are passed.
		 *
		 * @param args
		 *            arguments which will replace the placeholders in the I18n-Properties message.
		 *            The first argument will replace <code>{0}</code>, the second <code>{1}</code>
		 *            and so on.
		 * @return the builder itself, never {@code null}
		 */
		public ButtonDialogBuilder setI18nArguments(final Object... args) {
			this.i18nArgs = args;
			return this;
		}

		/**
		 * Sets the modality type which should be used for the dialog. By default, a dialog is
		 * {@link ModalityType#MODELESS}.
		 * <p>
		 * <strong>Attention:</strong> Without setting an explicit parent via
		 * {@link #setOwner(Window)} , modal dialogs will display inconsistent behavior and
		 * sometimes appear behind other dialogs. This can be a big problem as the user will not
		 * find the dialog, yet he is blocked from interacting with the rest of the program!
		 * </p>
		 *
		 * @param modalityType
		 *            the modality type
		 * @return the builder itself, never {@code null}
		 */
		public ButtonDialogBuilder setModalityType(final ModalityType modalityType) {
			if (modalityType == null) {
				throw new IllegalArgumentException("modalityType must not be null!");
			}

			this.modalityType = modalityType;
			return this;
		}

		/**
		 * Sets the graphics configuration which should be used for the dialog. Determines on which
		 * screen the dialog opens in a multi-monitor setup. By default, the default configuration
		 * is used.
		 *
		 * @param graphicsConfig
		 *            the graphics config to use
		 * @return the builder itself, never {@code null}
		 */
		public ButtonDialogBuilder setGraphicsConfiguration(final GraphicsConfiguration graphicsConfig) {
			if (graphicsConfig == null) {
				throw new IllegalArgumentException("graphicsConfig must not be null!");
			}

			this.graphicsConfig = graphicsConfig;
			return this;
		}

		/**
		 * Optionally specify the contents of the button dialog including layout. To specify
		 * buttons, call {@link #setButtons(AbstractButton...)}.
		 *
		 * @param mainComponent
		 *            the component in the center of the dialog
		 * @param size
		 *            the size of the dialog, see constants in {@link ButtonDialog}
		 * @return the builder itself, never {@code null}
		 */
		public ButtonDialogBuilder setContent(final JComponent mainComponent, final int size) {
			if (mainComponent == null) {
				throw new IllegalArgumentException("mainComponent must not be null!");
			}

			this.doLayout = true;
			this.mainComponent = mainComponent;
			this.size = size;
			return this;
		}

		/**
		 * Optionally specify custom buttons for the button dialog. Custom buttons always take
		 * precedence over default buttons. Has no effect unless
		 * {@link #setContent(JComponent, int)} has also been called.
		 *
		 * @param buttons
		 *            the custom buttons which should be added to the dialog. If neither custom nor
		 *            default buttons are specified, a default ok button is added
		 * @return the the builder itself, never {@code null}
		 */
		public ButtonDialogBuilder setButtons(final AbstractButton... buttons) {
			this.buttons = buttons;
			return this;
		}

		/**
		 * Optionally specify default buttons for the button dialog. Custom buttons always take
		 * precedence over default buttons. Has no effect unless
		 * {@link #setContent(JComponent, int)} has also been called.
		 *
		 * @param defaultButtons
		 *            the default buttons which should be added to the dialog. If neither custom nor
		 *            default buttons are specified, a default ok button is added
		 * @return the the builder itself, never {@code null}
		 */
		public ButtonDialogBuilder setButtons(final DefaultButtons... defaultButtons) {
			this.defaultButtons = defaultButtons;
			return this;
		}

		/**
		 * Creates the actual {@link ButtonDialog} instance according to the specified settings.
		 *
		 * @return the dialog instance, never {@code null}
		 */
		public ButtonDialog build() {
			ButtonDialog dialog = new ButtonDialog(owner, key, modalityType, graphicsConfig, i18nArgs);

			// see if we also should prepare the layout
			if (doLayout) {
				// prepare desired buttons
				boolean useCustomButtons = buttons != null && buttons.length > 0;
				boolean useDefaultButtons = defaultButtons != null && defaultButtons.length > 0;
				AbstractButton[] buttonArray;
				if (useCustomButtons) {
					// use user supplied buttons
					buttonArray = buttons;
				} else if (useDefaultButtons) {
					// create specified default buttons
					ArrayList<AbstractButton> list = new ArrayList<>(defaultButtons.length);
					for (DefaultButtons defB : defaultButtons) {
						switch (defB) {
							case OK_BUTTON:
								list.add(dialog.makeOkButton());
								break;
							case CANCEL_BUTTON:
								list.add(dialog.makeCancelButton());
								break;
							case CLOSE_BUTTON:
								list.add(dialog.makeCloseButton());
								break;
						}
					}
					buttonArray = list.toArray(new AbstractButton[list.size()]);
				} else {
					// create single default button
					buttonArray = new AbstractButton[] { dialog.makeOkButton() };
				}

				// do the layout
				dialog.layoutDefault(mainComponent, size, buttonArray);
			}

			return dialog;
		}
	}

	private static final long serialVersionUID = 1L;

	/** the maximum height ({@value #MAX_HEIGHT}) before size {@link #HUGE} will be reduced */
	private static final int MAX_HEIGHT = 800;

	/** 720x540 */
	public static final int NORMAL = 1;

	/** 720x300 */
	public static final int BROAD = 12;

	/** 360x540 */
	public static final int NARROW = 2;

	/** 800x600 */
	public static final int LARGE = 3;

	/** 1020x700 */
	public static final int WIDE = 11;

	/** 1000x760, automatically reduced to {@link #LARGE} for small resolutions */
	public static final int HUGE = 9;

	/** 1000x760 */
	public static final int HUGE_FORCED = 10;

	/** 600x200 */
	public static final int MESSAGE = 4;

	/** 500x250 */
	public static final int MESSAGE_BIT_EXTENDED = 15;

	/** 600x400 */
	public static final int MESSAGE_EXTENDED = 5;

	/** 420x300 */
	public static final int DEFAULT_SIZE = 8;

	/** 570x170 */
	public static final int EXTENSIVE = 14;

	/** 520x770 */
	public static final int TALL = 16;

	/** 720x700 */
	public static final int NORMAL_EXTENDED = 13;

	private static final Dimension DIMENSION_MESSAGE = new Dimension(600, 200);
	private static final Dimension DIMENSION_MESSAGE_BIT_EXTENDED = new Dimension(500, 250);
	private static final Dimension DIMENSION_MESSAGE_EXTENDED = new Dimension(600, 400);
	private static final Dimension DIMENSION_DEFAULT = new Dimension(420, 300);
	private static final Dimension DIMENSION_NORMAL = new Dimension(720, 540);
	private static final Dimension DIMENSION_NORMAL_EXTENDED = new Dimension(720, 700);
	private static final Dimension DIMENSION_BROAD = new Dimension(720, 300);
	private static final Dimension DIMENSION_NARROW = new Dimension(360, 540);
	private static final Dimension DIMENSION_LARGE = new Dimension(800, 600);
	private static final Dimension DIMENSION_WIDE = new Dimension(1020, 700);
	private static final Dimension DIMENSION_HUGE = new Dimension(1000, 760);
	private static final Dimension DIMENSION_EXTENSIVE = new Dimension(570, 170);
	private static final Dimension DIMENSION_TALL = new Dimension(520, 770);

	public static final int GAP = 6;

	public static final String WINDOW_CLOSING_EVENT_STRING = "WINDOW_CLOSING";

	protected static final Insets INSETS = new Insets(GAP, GAP, GAP, GAP);

	protected FixedWidthEditorPane infoTextLabel;

	/**
	 * Arguments which will replace the place holder in the I18n-Properties message. The first
	 * argument will replace <code>{0}</code>, the second <code>{1}</code> and so on.
	 */
	protected final Object[] arguments;

	private Component centerComponent;

	private String key = null;

	protected boolean wasConfirmed = false;

	private final LinkedList<ChangeListener> listeners = new LinkedList<>();

	/**
	 * @deprecated Use {@link ButtonDialogBuilder} instead
	 */
	@Deprecated
	public ButtonDialog(String key, Object... arguments) {
		super(ApplicationFrame.getApplicationFrame(),
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), false);
		this.arguments = arguments;
		configure(key);
		pack();
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, key, "open");
		checkForEDT();
	}

	/**
	 * @deprecated Use {@link ButtonDialogBuilder} instead
	 */
	@Deprecated
	public ButtonDialog(String key, boolean modal, Object... arguments) {
		super(ApplicationFrame.getApplicationFrame(),
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), modal);
		this.arguments = arguments;
		configure(key);
		pack();
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, key, "open");
		checkForEDT();
	}

	/**
	 * @deprecated Use {@link ButtonDialogBuilder} instead
	 */
	@Deprecated
	public ButtonDialog(String key, ModalityType type, Object... arguments) {
		super(ApplicationFrame.getApplicationFrame(),
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), type);
		this.arguments = arguments;
		configure(key);
		pack();
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, key, "open");
		checkForEDT();
	}

	/**
	 * @deprecated Use {@link ButtonDialogBuilder} instead
	 */
	@Deprecated
	public ButtonDialog(Dialog owner, String key, boolean modal, Object... arguments) {
		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), modal);
		this.arguments = arguments;
		configure(key);
		pack();
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, key, "open");
		checkForEDT();
	}

	/**
	 * @deprecated Use {@link ButtonDialogBuilder} instead
	 */
	@Deprecated
	public ButtonDialog(Dialog owner, String key, Object... arguments) {
		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), false);
		this.arguments = arguments;
		configure(key);
		pack();
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, key, "open");
		checkForEDT();
	}

	/**
	 * @deprecated Use {@link ButtonDialogBuilder} instead
	 */
	@Deprecated
	public ButtonDialog(Frame owner, String key, boolean modal, Object... arguments) {
		this(owner, key, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS, arguments);
	}

	/**
	 * @deprecated Use {@link ButtonDialogBuilder} instead
	 */
	@Deprecated
	public ButtonDialog(Frame owner, String key, Object... arguments) {
		this(owner, key, ModalityType.APPLICATION_MODAL, arguments);
	}

	/**
	 * Constructor used by the {@link ButtonDialogBuilder} and can also be used when subclassing.
	 *
	 * @param owner
	 *            the owner or {@code null}. Note that an owner should be set if the dialog will be
	 *            modal, otherwise the order ends up being undefined and causing all sorts of
	 *            trouble
	 * @param key
	 *            the i18n key
	 * @param modalityType
	 *            the modality type
	 * @param arguments
	 *            the optional i18n arguments
	 * @since 6.5.0
	 */
	protected ButtonDialog(Window owner, String key, ModalityType modalityType, Object... arguments) {
		this(owner, key, modalityType, owner != null ? owner.getGraphicsConfiguration() : null, arguments);
	}

	/**
	 * Constructor used by the {@link ButtonDialogBuilder} and can also be used when subclassing.
	 *
	 * @param owner
	 *            the owner or {@code null}. Note that an owner should be set if the dialog will be
	 *            modal, otherwise the order ends up being undefined and causing all sorts of
	 *            trouble
	 * @param key
	 *            the i18n key
	 * @param modalityType
	 *            the modality type
	 * @param graphicsConfig
	 *            the graphics config to use or {@code null}
	 * @param arguments
	 *            the optional i18n arguments
	 * @since 6.5.0
	 */
	protected ButtonDialog(Window owner, String key, ModalityType modalityType, GraphicsConfiguration graphicsConfig,
			Object... arguments) {
		super(owner, I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title", arguments), modalityType,
				graphicsConfig);
		this.arguments = arguments;
		configure(key);

		pack();
		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, key, "open");
		checkForEDT();
	}

	private void configure(String key) {
		this.key = key;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	protected final String getKey() {
		return "gui.dialog." + key;
	}

	/**
	 * Returns the internationalized info text for this dialog. Argument formation is already
	 * applied.
	 */
	protected String getInfoText() {
		return I18N.getMessage(I18N.getGUIBundle(), getKey() + ".message", this.arguments);
	}

	protected Icon getInfoIcon() {
		return SwingTools.createIcon("48/" + I18N.getMessage(I18N.getGUIBundle(), getKey() + ".icon"));
	}

	/**
	 * Returns the internationalized title for this dialog. Argument formation is already applied.
	 */
	protected String getDialogTitle() {
		return I18N.getMessage(I18N.getGUIBundle(), getKey() + ".title", this.arguments);
	}

	private JPanel makeInfoPanel() {
		return makeInfoPanel(getInfoText(), getInfoIcon());
	}

	private JPanel makeInfoPanel(String message, Icon icon) {
		JLabel infoIcon = new JLabel(icon);
		infoIcon.setVerticalAlignment(SwingConstants.TOP);
		JPanel infoPanel = new JPanel(new BorderLayout(20, 0));
		infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 16, 4));
		infoPanel.add(infoIcon, BorderLayout.WEST);
		int width;
		if (centerComponent != null) {
			width = (int) centerComponent.getPreferredSize().getWidth() - 88; // icon plus padding
			if (width < 420) {
				width = 420;
			}
		} else {
			width = 420;
		}

		infoTextLabel = new FixedWidthEditorPane(width, message);
		// set the background as for infoPanel such that infoTextLabel looks like a JLabel
		infoTextLabel.setBackground(infoPanel.getBackground());

		infoPanel.add(infoTextLabel, BorderLayout.CENTER);

		return infoPanel;
	}

	protected void layoutDefault(JComponent centerComponent, int size, Collection<AbstractButton> buttons) {
		layoutDefault(centerComponent, size, buttons.toArray(new AbstractButton[buttons.size()]));
	}

	protected void layoutDefault(JComponent centerComponent, Collection<AbstractButton> buttons) {
		layoutDefault(centerComponent, DEFAULT_SIZE, buttons.toArray(new AbstractButton[buttons.size()]));
	}

	protected void layoutDefault(JComponent centerComponent, AbstractButton... buttons) {
		layoutDefault(centerComponent, DEFAULT_SIZE, buttons);
	}

	protected void layoutDefault(JComponent centerComponent, int size, AbstractButton... buttons) {
		layoutDefault(centerComponent, makeButtonPanel(buttons), size);
	}

	protected void layoutDefault(final JComponent centerComponent, JPanel buttonPanel) {
		layoutDefault(centerComponent, buttonPanel, DEFAULT_SIZE);
	}

	protected void layoutDefault(final JComponent centerComponent, JPanel buttonPanel, int size) {
		this.centerComponent = centerComponent;
		setTitle(getDialogTitle());
		setLayout(new BorderLayout());
		add(makeInfoPanel(), BorderLayout.NORTH);
		if (centerComponent != null) {
			JPanel centerPanel = new JPanel(new BorderLayout());
			centerPanel.setBorder(BorderFactory.createEmptyBorder(0, GAP, 0, GAP));
			centerPanel.add(centerComponent, BorderLayout.CENTER);
			add(centerPanel, BorderLayout.CENTER);
		}
		add(buttonPanel, BorderLayout.SOUTH);
		this.addComponentListener(new ComponentListener() {

			@Override
			public void componentHidden(ComponentEvent e) {}

			@Override
			public void componentMoved(ComponentEvent e) {}

			@Override
			public void componentResized(ComponentEvent e) {
				if (infoTextLabel != null && centerComponent != null) {
					int prefHeightBefore = infoTextLabel.getPreferredSize().height;
					infoTextLabel.setWidth(centerComponent.getWidth() - 88);
					int prefHeightAfter = infoTextLabel.getPreferredSize().height;
					int heightDiff = prefHeightAfter - prefHeightBefore;
					if (heightDiff > 0) {
						// re-pack this dialog if the infoTextLabel has changed its prefHeight after
						// the resize
						// fixes center component being overlapped/cut off
						ButtonDialog.this.pack();
					}
				}

			}

			@Override
			public void componentShown(ComponentEvent e) {}
		});
		switch (size) {
			case DEFAULT_SIZE:
				break;
			default:
				setPreferredSize(getDefaultSize(size));
				break;
		}

		revalidate();
		pack();
		setDefaultLocation();
	}

	protected void setDefaultLocation() {
		setLocationRelativeTo(getOwner() != null ? getOwner() : ApplicationFrame.getApplicationFrame());
	}

	protected void setDefaultSize() {
		setDefaultSize(NORMAL);
	}

	protected Dimension getDefaultSize(int size) {
		switch (size) {
			case NARROW:
				return DIMENSION_NARROW;
			case NORMAL:
				return DIMENSION_NORMAL;
			case BROAD:
				return DIMENSION_BROAD;
			case LARGE:
				return DIMENSION_LARGE;
			case HUGE:
				// this dimension is too large for HD-ready displays and also for presentation
				// resolutions
				// return the next smaller dimension instead to avoid components being too large for
				// display
				if (RapidMinerGUI.getMainFrame() != null
						&& RapidMinerGUI.getMainFrame().getGraphicsConfiguration() != null) {
					if (RapidMinerGUI.getMainFrame().getGraphicsConfiguration().getBounds().getHeight() <= MAX_HEIGHT) {
						return getDefaultSize(LARGE);
					} else {
						return DIMENSION_HUGE;
					}
				} else {
					if (Toolkit.getDefaultToolkit().getScreenSize().getHeight() <= MAX_HEIGHT) {
						return getDefaultSize(LARGE);
					} else {
						return DIMENSION_HUGE;
					}
				}
			case HUGE_FORCED:
				return DIMENSION_HUGE;
			case WIDE:
				return DIMENSION_WIDE;
			case MESSAGE:
				return DIMENSION_MESSAGE;
			case MESSAGE_BIT_EXTENDED:
				return DIMENSION_MESSAGE_BIT_EXTENDED;
			case NORMAL_EXTENDED:
				return DIMENSION_NORMAL_EXTENDED;
			case MESSAGE_EXTENDED:
				return DIMENSION_MESSAGE_EXTENDED;
			case EXTENSIVE:
				return DIMENSION_EXTENSIVE;
			case TALL:
				return DIMENSION_TALL;
			default:
				return DIMENSION_DEFAULT;
		}
	}

	protected void setDefaultSize(int size) {
		if (size != DEFAULT_SIZE) {
			setPreferredSize(getDefaultSize(size));
		}
		pack();
	}

	protected JPanel makeButtonPanel(Collection<AbstractButton> buttons) {
		return makeButtonPanel(buttons.toArray(new AbstractButton[buttons.size()]));
	}

	protected JPanel makeButtonPanel(AbstractButton... buttons) {
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, GAP, GAP));
		for (final AbstractButton button : buttons) {
			if (button != null) {
				buttonPanel.add(button);
				button.addActionListener(
						e -> ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, key, button.getActionCommand()));
			}
		}
		return buttonPanel;
	}

	/** Will be default button. */
	protected JButton makeOkButton() {
		return makeOkButton("ok");
	}

	protected JButton makeOkButton(String i18nKey) {
		Action okAction = new ResourceAction(i18nKey) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				wasConfirmed = true;
				ok();
			}
		};
		JButton button = new JButton(okAction);
		getRootPane().setDefaultButton(button);

		return button;
	}

	/** Will listen to ESCAPE. */
	protected JButton makeCancelButton() {
		return makeCancelButton("cancel");
	}

	protected JButton makeCancelButton(String i18nKey) {
		Action cancelAction = new ResourceAction(i18nKey) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				wasConfirmed = false;
				cancel();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);
		return new JButton(cancelAction);
	}

	/** Will be default button and listen to ESCAPE. */
	protected JButton makeCloseButton() {
		Action action = new ResourceAction("close") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				wasConfirmed = false;
				close();
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CLOSE");
		getRootPane().getActionMap().put("CLOSE", action);
		JButton button = new JButton(action);
		getRootPane().setDefaultButton(button);
		return button;
	}

	protected void cancel() {
		dispose();
	}

	protected void ok() {
		dispose();
	}

	/**
	 *
	 * Calls {@link #setConfirmed(boolean)} and {@link #ok()} or {@link #cancel()} depending on the
	 * provided input.
	 * <p>
	 * Necessary for extensions that cannot call the protected {@link #ok()}, {@link #cancel()} and
	 * {@link #setConfirmed(boolean)} methods.
	 *
	 * @param accept
	 *            defines whether the user has accepted the dialog. If {@code true} {@link #ok()} is
	 *            called and {@link #setConfirmed(boolean)} is set to {@code true} as well. If
	 *            {@code false} {@link #cancel()} is called and {@link #setConfirmed(boolean)} is
	 *            set to {@code false}.
	 * @since 6.5.0
	 */
	public void accept(boolean accept) {
		if (accept) {
			setConfirmed(true);
			ok();
		} else {
			setConfirmed(false);
			cancel();
		}
	}

	protected void close() {
		dispose();
	}

	/** Returns true iff the user pressed the generated ok button. */
	public boolean wasConfirmed() {
		return wasConfirmed;
	}

	protected void setConfirmed(boolean b) {
		this.wasConfirmed = b;
	}

	public static TitledBorder createTitledBorder(String title) {
		TitledBorder border = new TitledBorder(createBorder(), title) {

			private static final long serialVersionUID = 3113821577644055057L;

			@Override
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				super.paintBorder(c, g, x - EDGE_SPACING, y, width + 2 * EDGE_SPACING, height);
			}
		};
		return border;
	}

	public static Border createBorder() {
		return BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY);
	}

	public static GridLayout createGridLayout(int rows, int columns) {
		return new GridLayout(rows, columns, GAP, GAP);
	}

	public void addChangeListener(ChangeListener l) {
		listeners.add(l);
	}

	public void removeChangeListener(ChangeListener l) {
		listeners.remove(l);
	}

	protected void fireStateChanged() {
		ChangeEvent e = new ChangeEvent(this);
		for (ChangeListener l : listeners) {
			l.stateChanged(e);
		}
	}

	private void checkForEDT() {
		if (RapidMiner.getVersion().isDevelopmentBuild() && !SwingUtilities.isEventDispatchThread()) {
			System.err.println("Button dialog constructor is not in EDT!");
			new Exception().printStackTrace();
		}
	}
}
