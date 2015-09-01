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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.ButtonBubble;
import com.rapidminer.gui.tour.RunProcessStep;
import com.rapidminer.gui.tour.Step;
import com.rapidminer.gui.tour.comic.ComicManager;
import com.rapidminer.gui.tour.comic.gui.ComicGuiTools;
import com.rapidminer.gui.tour.comic.states.EpisodePanel;
import com.rapidminer.gui.tour.comic.states.EpisodeState;
import com.rapidminer.gui.tour.comic.states.EpisodeStateMachine;
import com.rapidminer.gui.tour.comic.states.EpisodeTransition;
import com.rapidminer.operator.ModelApplier;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.learner.tree.ParallelDecisionTreeLearner;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * The "apply model" episode.
 *
 * @author Marco Boeck
 *
 */
public class ApplyModelEpisode extends AbstractEpisode {

	private static final List<Step> listOfSteps = new LinkedList<>();

	/** the step which indicates to run the process */
	private static Step runProcessStep;

	/** the bubble directing the user back to the design perspective */
	private static BubbleWindow goBackToDesignBubble;

	private static EpisodeState EMPTY_PROCESS = new EpisodePanel(
			"comics/images/tutorial-comic-episode-apply_model-apply-model-1.png") {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			if (event == EpisodeEvent.PROCESS_UPDATED) {
				if (stateMachine.getEpisode().getAllOperatorsInCurrentProcess().size() > 0) {
					return EpisodeTransition.NEXT;
				}
			}

			return EpisodeTransition.IGNORE;
		}

	};

	private static EpisodeState PROCESS_CREATION = new EpisodePanel(
			"comics/images/tutorial-comic-episode-apply_model-apply-model-1.png") {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();

			if (operators.size() >= 4) {
				RepositorySource retrieveDataOperator = EpisodeHelper.getRetrieveOperator(operators, "//Samples/data/Deals");
				RepositorySource retrieveDataTestTestOperator = EpisodeHelper.getRetrieveOperator(operators,
						"//Samples/data/Deals-Testset");
				ParallelDecisionTreeLearner decisionTree = EpisodeHelper.getOperator(operators,
						ParallelDecisionTreeLearner.class);
				ModelApplier applyModel = EpisodeHelper.getOperator(operators, ModelApplier.class);

				if (retrieveDataOperator != null && retrieveDataTestTestOperator != null && decisionTree != null
						&& applyModel != null) {
					return EpisodeTransition.NEXT;
				}

			}

			return EpisodeTransition.IGNORE;
		}

	};

	private static EpisodeState OPERATORS_CONNECTION = new EpisodePanel(
			"comics/images/tutorial-comic-episode-apply_model-apply-model-2.png") {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			if (event == EpisodeEvent.PROCESS_UPDATED) {
				Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();

				if (operators.size() < 4) {
					return EpisodeTransition.PREVIOUS;
				}

				RepositorySource retrieveDataOperator = EpisodeHelper.getRetrieveOperator(operators, "//Samples/data/Deals");
				RepositorySource retrieveDataTestOperator = EpisodeHelper.getRetrieveOperator(operators,
						"//Samples/data/Deals-Testset");
				ParallelDecisionTreeLearner decisionTree = EpisodeHelper.getOperator(operators,
						ParallelDecisionTreeLearner.class);
				ModelApplier applyModel = EpisodeHelper.getOperator(operators, ModelApplier.class);

				if (retrieveDataOperator == null || retrieveDataTestOperator == null || decisionTree == null
						|| applyModel == null) {
					return EpisodeTransition.PREVIOUS;
				}

				if (EpisodeHelper.areConnected(retrieveDataOperator, 0, decisionTree, 0)
						&& EpisodeHelper.areConnected(decisionTree, 0, applyModel, 0)
						&& EpisodeHelper.areConnected(retrieveDataTestOperator, 0, applyModel, 1)
						&& (EpisodeHelper.isConnected(applyModel, 0, 0) || EpisodeHelper.isConnected(applyModel, 0, 1))) {
					runProcessStep.start();
					return EpisodeTransition.NEXT;
				}
			}

			return EpisodeTransition.IGNORE;
		}

	};

	private static EpisodeState OPERATORS_CONNECTED = new EpisodePanel(
			"comics/images/tutorial-comic-episode-apply_model-apply-model-3.png") {

		@Override
		public EpisodeTransition onEvent(final EpisodeStateMachine stateMachine, final EpisodeEvent event,
				final Object object) {
			if (event == EpisodeEvent.PROCESS_UPDATED) {
				Collection<Operator> operators = stateMachine.getEpisode().getAllOperatorsInCurrentProcess();

				if (operators.size() < 4) {
					runProcessStep.killStep();
					return EpisodeTransition.PREVIOUS;
				}

				RepositorySource retrieveDataOperator = EpisodeHelper.getRetrieveOperator(operators, "//Samples/data/Deals");
				RepositorySource retrieveDataTestOperator = EpisodeHelper.getRetrieveOperator(operators,
						"//Samples/data/Deals-Testset");
				ParallelDecisionTreeLearner decisionTree = EpisodeHelper.getOperator(operators,
						ParallelDecisionTreeLearner.class);
				ModelApplier applyModel = EpisodeHelper.getOperator(operators, ModelApplier.class);

				if (retrieveDataOperator == null || retrieveDataTestOperator == null || decisionTree == null
						|| applyModel == null) {
					runProcessStep.killStep();
					return EpisodeTransition.PREVIOUS;
				}

				if (!EpisodeHelper.areConnected(retrieveDataOperator, 0, decisionTree, 0)
						|| !EpisodeHelper.areConnected(decisionTree, 0, applyModel, 0)
						|| !EpisodeHelper.areConnected(retrieveDataTestOperator, 0, applyModel, 1)
						|| !EpisodeHelper.isConnected(applyModel, 0, 0) && !EpisodeHelper.isConnected(applyModel, 0, 1)) {
					runProcessStep.killStep();
					return EpisodeTransition.PREVIOUS;
				}
			}

			if (event == EpisodeEvent.PROCESS_STARTED) {
				return EpisodeTransition.NEXT;
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
		public void onTransition(final AbstractEpisode episode) {
			super.onTransition(episode);
		}

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
			goBackToDesignBubble = new ButtonBubble(RapidMinerGUI.getMainFrame(), null, AlignedSide.BOTTOM,
					"design_perspective_go_back_comic", "workspace_design", "result");
			goBackToDesignBubble.setVisible(true);
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

	/** the preview icon */
	private ImageIcon previewImageIcon;

	/** the default process xml */
	private String defaultXML;

	/**
	 * Creates a new {@link ApplyModelEpisode} instance.
	 */
	public ApplyModelEpisode() {
		super(StartingPerspective.DESIGN);

		getEpisodeStateMachine().addNextEpisodeState(EMPTY_PROCESS);
		getEpisodeStateMachine().addNextEpisodeState(PROCESS_CREATION);
		getEpisodeStateMachine().addNextEpisodeState(OPERATORS_CONNECTION);
		getEpisodeStateMachine().addNextEpisodeState(OPERATORS_CONNECTED);
		getEpisodeStateMachine().addNextEpisodeState(PROCESS_STARTED);
		getEpisodeStateMachine().addNextEpisodeState(PROCESS_ENDED);
		getEpisodeStateMachine().addNextEpisodeState(ARRIVED_IN_RESULTPERSPECTIVE);
		getEpisodeStateMachine().addNextEpisodeState(FINAL);

		try {
			previewImageIcon = new ImageIcon(ImageIO.read(Tools
					.getResource("comics/images/tutorial-comic-episode-apply_model-preview.png")));
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.Episode.failed_to_load_preview_image",
					toString());
		}
		try {
			URL url = Tools.getResource("comics/processes/apply_model.xml");
			if (url != null) {
				defaultXML = Tools.readTextFile(url.openStream());
			}
		} catch (IOException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.tour.comic.Episode.failed_to_load_default_xml",
					toString());
		}
	}

	@Override
	public String getI18NKey() {
		return "episode_apply_model";
	}

	@Override
	public String getDefaultProcessXML() {
		return defaultXML;
	}

	@Override
	public void initState() {
		getEpisodeStateMachine().setCurrentState(EMPTY_PROCESS);
	}

	@Override
	public synchronized void startEpisode() {
		initSteps();

		super.startEpisode();
	}

	@Override
	public synchronized void finishEpisode() {
		cleanup();

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
		if (goBackToDesignBubble != null) {
			goBackToDesignBubble.killBubble(false);
			goBackToDesignBubble = null;
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

		runProcessStep = new RunProcessStep("run_comic_third");

		listOfSteps.add(runProcessStep);
	}

}
