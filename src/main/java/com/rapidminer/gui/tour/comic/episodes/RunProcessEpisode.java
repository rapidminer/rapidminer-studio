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
package com.rapidminer.gui.tour.comic.episodes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.processeditor.NewOperatorEditor;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.BubbleWindow.BubbleListener;
import com.rapidminer.gui.tour.ButtonBubble;
import com.rapidminer.gui.tour.DockableBubble;
import com.rapidminer.gui.tour.OperatorBubble;
import com.rapidminer.gui.tour.RunProcessStep;
import com.rapidminer.gui.tour.Step;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.gui.ComicGuiTools;
import com.rapidminer.gui.tour.comic.states.EpisodePanel;
import com.rapidminer.gui.tour.comic.states.EpisodeState;
import com.rapidminer.gui.tour.comic.states.EpisodeStateMachine;
import com.rapidminer.gui.tour.comic.states.EpisodeTransition;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.learner.tree.ParallelDecisionTreeLearner;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryBrowser;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * The "run process" episode.
 *
 * @author Marcin Skirzynski, Marco Boeck
 *
 */
public class RunProcessEpisode extends AbstractEpisode {

	private static final List<Step> listOfSteps = new LinkedList<>();

	private static final List<BubbleWindow> listOfBubbles = Collections.synchronizedList(new LinkedList<BubbleWindow>());

	/** the step which indicates to run the process */
	private static Step runProcessStep;

	/** the bubble explaining the design perspective */
	private static BubbleWindow designBubble;

	/** the bubble explaining the result perspective */
	private static BubbleWindow resultBubble;

	/** the bubble directing the user back to the design perspective */
	private static BubbleWindow goBackToDesignBubble;

	/** the bubble explaining an {@link Operator} */
	private static BubbleWindow operatorBubble;

	/** the bubble explaining an {@link Operator} port */
	private static BubbleWindow portBubble;

	/** the bubble explaining to connect operators now */
	private static BubbleWindow connectBubble;

	/** the bubble explaining the repositories view */
	private static BubbleWindow repositoryBubble;

	/** the bubble explaining the operators view */
	private static BubbleWindow operatorsBubble;

	/** indicate if the comic has been started for the first time */
	private static boolean firstTime = true;

	/** the preview icon */
	private ImageIcon previewImageIcon;

	private static EpisodeState PRE_COMIC = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-1-inactive.png") {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			super.onTransition(episode);
			// only show perspective bubble on first launch, not when we re-enter this via error
			// state
			if (firstTime) {
				designBubble = new ButtonBubble(RapidMinerGUI.getMainFrame(), null, AlignedSide.BOTTOM,
						"design_perspective_comic", "workspace_design", true, true,
						new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
				registerBubble(designBubble);
				designBubble.setVisible(true);
				designBubble.addBubbleListener(new BubbleListener() {

					@Override
					public void bubbleClosed(BubbleWindow bw) {
						repositoryBubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT,
								"repository_comic", RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY,
								new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
						registerBubble(repositoryBubble);
						repositoryBubble.addBubbleListener(episode.getEpisodeEventDelegator());
						repositoryBubble.setVisible(true);
					}

					@Override
					public void actionPerformed(BubbleWindow bw) {
						repositoryBubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT,
								"repository_comic", RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY,
								new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
						registerBubble(repositoryBubble);
						repositoryBubble.addBubbleListener(episode.getEpisodeEventDelegator());
						repositoryBubble.setVisible(true);
					}
				});
				firstTime = false;
			} else {
				repositoryBubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT, "repository_comic",
						RepositoryBrowser.REPOSITORY_BROWSER_DOCK_KEY, new JButton[] { getBubbleWindowNextButton(true) },
						new Object[0]);
				registerBubble(repositoryBubble);
				repositoryBubble.addBubbleListener(episode.getEpisodeEventDelegator());
				repositoryBubble.setVisible(true);
			}
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			switch (event) {
				case BUBBLE_EVENT:
					return EpisodeTransition.NEXT;
				default:
			}
			return EpisodeTransition.IGNORE;
		}

	};

	private static EpisodeState EMPTY_PROCESS = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-1.png") {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			RapidMinerGUI.getMainFrame().getProcessPanel().repaint();
			super.onTransition(episode);
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			switch (event) {
				case DRAG_REPOSITORY_STARTED:
					runProcessStep.killStep();
					RepositoryLocation location = (RepositoryLocation) object;
					if (location.getAbsoluteLocation().equals("//Samples/data/Deals")) {
						unregisterBubble(repositoryBubble, false);
						unregisterBubble(designBubble, false);
						return EpisodeTransition.NEXT;
					}
					break;
				default:
					// If event does not change process at all, do nothing
					if (stateMachine.getEpisode().getAllOperatorsInCurrentProcess().size() != 0) {
						return EpisodeTransition.ERROR;
					}
					break;
			}
			return EpisodeTransition.IGNORE;
		}

		@Override
		public boolean isRestorableState() {
			return false;
		}

	};

	private static EpisodeState DEALS_DRAGED = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-1-drop.png") {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			switch (event) {
				case DRAG_ENDED:
					if (stateMachine.getEpisode().getAllOperatorsInCurrentProcess().size() == 1) {
						return EpisodeTransition.NEXT;
					} else {
						return EpisodeTransition.PREVIOUS;
					}
				default:
					break;
			}
			return EpisodeTransition.IGNORE;
		}

		@Override
		public boolean isRestorableState() {
			return false;
		}
	};

	private static EpisodeState DEALS_DROPPED = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-2-inactive.png") {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			super.onTransition(episode);
			operatorBubble = new OperatorBubble(RapidMinerGUI.getMainFrame(), AlignedSide.BOTTOM, "operator_comic",
					RepositorySource.class, 1, new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
			registerBubble(operatorBubble);
			operatorBubble.setVisible(true);
			operatorBubble.addBubbleListener(new BubbleListener() {

				@Override
				public void bubbleClosed(BubbleWindow bw) {
					portBubble = new OperatorBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT, "port_comic",
							RepositorySource.class, 1, new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
					registerBubble(portBubble);
					portBubble.setVisible(true);
					portBubble.addBubbleListener(new BubbleListener() {

						@Override
						public void bubbleClosed(BubbleWindow bw) {
							operatorsBubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT,
									"operatortree_comic", NewOperatorEditor.NEW_OPERATOR_DOCK_KEY,
									new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
							registerBubble(operatorsBubble);
							operatorsBubble.addBubbleListener(episode.getEpisodeEventDelegator());
							operatorsBubble.setVisible(true);
						}

						@Override
						public void actionPerformed(BubbleWindow bw) {
							operatorsBubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT,
									"operatortree_comic", NewOperatorEditor.NEW_OPERATOR_DOCK_KEY,
									new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
							registerBubble(operatorsBubble);
							operatorsBubble.addBubbleListener(episode.getEpisodeEventDelegator());
							operatorsBubble.setVisible(true);
						}
					});
				}

				@Override
				public void actionPerformed(BubbleWindow bw) {
					portBubble = new OperatorBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT, "port_comic",
							RepositorySource.class, 1, new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
					registerBubble(portBubble);
					portBubble.setVisible(true);
					portBubble.addBubbleListener(new BubbleListener() {

						@Override
						public void bubbleClosed(BubbleWindow bw) {
							operatorsBubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT,
									"operatortree_comic", NewOperatorEditor.NEW_OPERATOR_DOCK_KEY,
									new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
							registerBubble(operatorsBubble);
							operatorsBubble.addBubbleListener(episode.getEpisodeEventDelegator());
							operatorsBubble.setVisible(true);
						}

						@Override
						public void actionPerformed(BubbleWindow bw) {
							operatorsBubble = new DockableBubble(RapidMinerGUI.getMainFrame(), AlignedSide.RIGHT,
									"operatortree_comic", NewOperatorEditor.NEW_OPERATOR_DOCK_KEY,
									new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
							registerBubble(operatorsBubble);
							operatorsBubble.addBubbleListener(episode.getEpisodeEventDelegator());
							operatorsBubble.setVisible(true);
						}
					});
				}
			});
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			switch (event) {
				case BUBBLE_EVENT:
					return EpisodeTransition.NEXT;
				default:
					Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();
					if (operators.size() != 1 || !EpisodeHelper.containsRetrieveOperator(operators, "//Samples/data/Deals")) {
						return EpisodeTransition.ERROR;
					}
			}
			return EpisodeTransition.IGNORE;
		}

	};

	private static EpisodeState BUBBLES = new EpisodePanel("comics/images/tutorial-comic-episode-run_process-panel-2.png") {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			RapidMinerGUI.getMainFrame().getProcessPanel().repaint();
			super.onTransition(episode);
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			switch (event) {
				case DRAG_OPERATOR_STARTED:
					Operator draggedOperator = (Operator) object;
					if (draggedOperator instanceof ParallelDecisionTreeLearner) {
						unregisterBubble(operatorBubble, false);
						unregisterBubble(portBubble, false);
						unregisterBubble(operatorsBubble, false);
						return EpisodeTransition.NEXT;
					}
					break;
				default:
					Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();
					if (operators.size() != 1 || !EpisodeHelper.containsRetrieveOperator(operators, "//Samples/data/Deals")) {
						return EpisodeTransition.ERROR;
					}
			}
			return EpisodeTransition.IGNORE;
		}

		@Override
		public boolean isRestorableState() {
			return false;
		}

	};

	private static EpisodeState TREE_DRAGGED = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-2-drop.png") {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			switch (event) {
				case DRAG_ENDED:
					if (stateMachine.getEpisode().getAllOperatorsInCurrentProcess().size() == 2) {
						return EpisodeTransition.NEXT;
					} else {
						return EpisodeTransition.PREVIOUS;
					}
				default:
					break;
			}
			return EpisodeTransition.IGNORE;
		}

		@Override
		public boolean isRestorableState() {
			return false;
		}

	};

	private static EpisodeState TREE_DROPPED = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-3-inactive.png") {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			super.onTransition(episode);
			connectBubble = new OperatorBubble(RapidMinerGUI.getMainFrame(), AlignedSide.BOTTOM, "connect_comic",
					ParallelDecisionTreeLearner.class, 1, new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
			registerBubble(connectBubble);
			connectBubble.addBubbleListener(episode.getEpisodeEventDelegator());
			connectBubble.setVisible(true);
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			switch (event) {
				case BUBBLE_EVENT:
					return EpisodeTransition.NEXT;
				default:
					Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();

					if (operators.size() != 2) {
						return EpisodeTransition.ERROR;
					}

					RepositorySource retrieveDataOperator = EpisodeHelper.getRetrieveOperator(operators,
							"//Samples/data/Deals");
					ParallelDecisionTreeLearner decisionTree = EpisodeHelper.getOperator(operators,
							ParallelDecisionTreeLearner.class);

					if (retrieveDataOperator == null || decisionTree == null) {
						return EpisodeTransition.ERROR;
					}
			}
			return EpisodeTransition.IGNORE;
		}
	};

	private static EpisodeState CONNECT_BUBBLE = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-3.png") {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			RapidMinerGUI.getMainFrame().getProcessPanel().repaint();
			super.onTransition(episode);
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();

			if (operators.size() != 2) {
				return EpisodeTransition.ERROR;
			}

			RepositorySource retrieveDataOperator = EpisodeHelper.getRetrieveOperator(operators, "//Samples/data/Deals");
			ParallelDecisionTreeLearner decisionTree = EpisodeHelper.getOperator(operators,
					ParallelDecisionTreeLearner.class);

			if (retrieveDataOperator == null || decisionTree == null) {
				return EpisodeTransition.ERROR;
			}

			if (EpisodeHelper.areConnected(retrieveDataOperator, 0, decisionTree, 0)
					&& (EpisodeHelper.isConnected(decisionTree, 0, 0) || EpisodeHelper.isConnected(decisionTree, 0, 1))) {
				unregisterBubble(connectBubble, false);
				runProcessStep.start();
				return EpisodeTransition.NEXT;
			} else {
				return EpisodeTransition.IGNORE;
			}
		}

		@Override
		public boolean isRestorableState() {
			return false;
		}

	};

	private static EpisodeState OPERATORS_CONNECTED = new EpisodePanel(
			"comics/images/tutorial-comic-episode-run_process-panel-4.png") {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {

			if (event == EpisodeEvent.PROCESS_STARTED) {
				return EpisodeTransition.NEXT;
			}

			Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();

			if (operators.size() != 2) {
				return EpisodeTransition.ERROR;
			}

			RepositorySource retrieveDataOperator = EpisodeHelper.getRetrieveOperator(operators, "//Samples/data/Deals");
			ParallelDecisionTreeLearner decisionTree = EpisodeHelper.getOperator(operators,
					ParallelDecisionTreeLearner.class);

			if (retrieveDataOperator == null || decisionTree == null) {
				return EpisodeTransition.ERROR;
			}

			if (!EpisodeHelper.areConnected(retrieveDataOperator, 0, decisionTree, 0)
					|| !EpisodeHelper.isConnected(decisionTree, 0, 0) && !EpisodeHelper.isConnected(decisionTree, 0, 1)) {
				runProcessStep.killStep();
				return EpisodeTransition.PREVIOUS;
			}

			return EpisodeTransition.IGNORE;
		}

	};

	private static EpisodeState PROCESS_STARTED = new EpisodePanel() {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			if (event == EpisodeEvent.PROCESS_ENDED) {
				return EpisodeTransition.NEXT;
			}
			return EpisodeTransition.IGNORE;
		}

	};

	private static EpisodeState PROCESS_ENDED = new EpisodePanel() {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine episodeStateMachine, final EpisodeEvent event,
				final Object object) {
			if (event == EpisodeEvent.PERSPECTIVE_CHANGED) {
				if (RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals("result")) {
					return EpisodeTransition.NEXT;
				}
			}
			return EpisodeTransition.IGNORE;
		}
	};

	private static EpisodeState ARRIVED_IN_RESULTPERSPECTIVE = new EpisodePanel() {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			super.onTransition(episode);
			resultBubble = new ButtonBubble(RapidMinerGUI.getMainFrame(), null, AlignedSide.BOTTOM,
					"result_perspective_comic", "workspace_result", true, true,
					new JButton[] { getBubbleWindowNextButton(true) }, new Object[0]);
			registerBubble(resultBubble);
			resultBubble.setVisible(true);
			goBackToDesignBubble = new ButtonBubble(RapidMinerGUI.getMainFrame(), null, AlignedSide.BOTTOM,
					"design_perspective_go_back_comic", "workspace_design", "result");
			registerBubble(goBackToDesignBubble);
			resultBubble.addBubbleListener(new BubbleListener() {

				@Override
				public void bubbleClosed(BubbleWindow bw) {
					if (goBackToDesignBubble != null) {
						goBackToDesignBubble.setVisible(true);
					}
				}

				@Override
				public void actionPerformed(BubbleWindow bw) {
					if (goBackToDesignBubble != null) {
						goBackToDesignBubble.setVisible(true);
					}
				}
			});
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine episodeStateMachine, final EpisodeEvent event,
				final Object object) {
			if (event == EpisodeEvent.PERSPECTIVE_CHANGED) {
				if (RapidMinerGUI.getMainFrame().getPerspectives().getCurrentPerspective().getName().equals("design")) {
					return EpisodeTransition.NEXT;
				}
			}
			return EpisodeTransition.IGNORE;
		}
	};

	private static EpisodeState FINAL = new EpisodePanel() {

		@Override
		public void onTransition(final AbstractEpisode episode) {
			super.onTransition(episode);
			ComicManager.getInstance().finishCurrentEpisode();
			ComicGuiTools.createAndShowComicFinishedPopup(episode);
		}

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			return EpisodeTransition.IGNORE;
		}

	};

	/**
	 * Creates a new {@link RunProcessEpisode} instance.
	 */
	public RunProcessEpisode() {
		super(StartingPerspective.DESIGN);

		getEpisodeStateMachine().addNextEpisodeState(PRE_COMIC);
		getEpisodeStateMachine().addNextEpisodeState(EMPTY_PROCESS);
		getEpisodeStateMachine().addNextEpisodeState(DEALS_DRAGED);
		getEpisodeStateMachine().addNextEpisodeState(DEALS_DROPPED);
		getEpisodeStateMachine().addNextEpisodeState(BUBBLES);
		getEpisodeStateMachine().addNextEpisodeState(TREE_DRAGGED);
		getEpisodeStateMachine().addNextEpisodeState(TREE_DROPPED);
		getEpisodeStateMachine().addNextEpisodeState(CONNECT_BUBBLE);
		getEpisodeStateMachine().addNextEpisodeState(OPERATORS_CONNECTED);
		getEpisodeStateMachine().addNextEpisodeState(PROCESS_STARTED);
		getEpisodeStateMachine().addNextEpisodeState(PROCESS_ENDED);
		getEpisodeStateMachine().addNextEpisodeState(ARRIVED_IN_RESULTPERSPECTIVE);
		getEpisodeStateMachine().addNextEpisodeState(FINAL);

		try {
			previewImageIcon = new ImageIcon(ImageIO.read(Tools
					.getResource("comics/images/tutorial-comic-episode-run_process-preview.png")));
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.Episode.failed_to_load_preview_image",
					toString());
		}
	}

	@Override
	public String getI18NKey() {
		return "episode_run_process";
	}

	@Override
	public String getDefaultProcessXML() {
		return null;
	}

	@Override
	public void initState() {
		getEpisodeStateMachine().setCurrentState(PRE_COMIC);
	}

	@Override
	public synchronized void startEpisode() {
		initSteps();

		super.startEpisode();
	}

	@Override
	public synchronized void finishEpisode() {
		cleanup();

		// reset this flag
		firstTime = true;

		super.finishEpisode();
	}

	@Override
	protected ImageIcon getPreviewImageIcon() {
		return previewImageIcon;
	}

	@Override
	public void cleanup() {
		// we are finished, kill any leftover steps
		for (Step step : listOfSteps) {
			if (step != null) {
				step.killStep();
			}
		}

		// remove leftover bubbles
		for (BubbleWindow bw : new LinkedList<>(listOfBubbles)) {
			unregisterBubble(bw, false);
		}
	}

	/**
	 * Registers the given {@link BubbleWindow}. Needed to kill them on cleanup.
	 *
	 * @param bw
	 */
	private static void registerBubble(BubbleWindow bw) {
		listOfBubbles.add(bw);
	}

	/**
	 * Unregisters the given {@link BubbleWindow}.
	 *
	 * @param bw
	 *            the {@link BubbleWindow} to kill
	 * @param notifyListener
	 *            if <code>true</code>, the listeners will be notified the bubble was closed
	 */
	private static void unregisterBubble(BubbleWindow bw, boolean notifyListener) {
		if (bw != null) {
			listOfBubbles.remove(bw);
			bw.killBubble(notifyListener);
			bw = null;
		}
	}

	/**
	 * Inits the {@link Step}s. Done here because the step require the GUI to be fully initialized
	 * before constructing them.
	 */
	private static void initSteps() {
		// remove leftover steps
		for (Step step : listOfSteps) {
			if (step != null) {
				step.killStep();
			}
		}
		listOfSteps.clear();

		runProcessStep = new RunProcessStep("run_comic_first");

		listOfSteps.add(runProcessStep);
	}

}
