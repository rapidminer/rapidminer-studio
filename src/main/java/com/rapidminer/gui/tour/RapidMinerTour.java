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
package com.rapidminer.gui.tour;

import com.rapidminer.gui.tour.AddBreakpointStep.Position;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.rapidminer.gui.tour.Step.BubbleType;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.SimpleOperatorChain;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.tree.AbstractTreeLearner;


/**
 * A class that starts a beginner's tour for RapidMiner as soon as the <code>startTour()</code>
 * method is called. When the user completes an action, the next one is shown by a bubble.
 * 
 * @author Philipp Kersting and Thilo Kamradt
 * 
 */
public class RapidMinerTour extends IntroductoryTour {

	AlignedSide side = AlignedSide.LEFT;

	public RapidMinerTour() {
		super(20, "RapidMiner");
	}

	@Override
	protected void buildTour() {
		// create Steps which will be performed
		addStep(new NewProcessStep("start"));
		addStep(new RunProcessStep("tryrun"));
		addStep(new AddOperatorStep(side, "adddatabase", RepositorySource.class));
		addStep(new AddOperatorStep(side, "dragdrop", AbstractTreeLearner.class));
		addStep(new ChangeParameterStep(side, "changeparam", AbstractTreeLearner.class,
				AbstractTreeLearner.PARAMETER_CRITERION, "information_gain"));
		addStep(new AddBreakpointStep(BubbleType.BUTTON, side, "addbreakpoint", AbstractTreeLearner.class, Position.AFTER));
		addStep(new RunProcessStep("run"));
		addStep(new ResumeFromBreakpointStep(side, "goon", (AbstractTreeLearner.class), Position.DONT_CARE));
		addStep(new SaveProcessStep(side, "saveas", "save_as"));
		addStep(new OpenProcessStep(side, "open", "open"));
		addStep(new RemoveOperatorStep(BubbleType.OPERATOR, side, "remove", AbstractTreeLearner.class));
		addStep(new AddOperatorStep(side, "restore", AbstractLearner.class));
		addStep(new AddBreakpointStep(BubbleType.OPERATOR, side, "restorebreakpoint", AbstractLearner.class, Position.BEFORE));
		addStep(new RemoveBreakpointStep(BubbleType.OPERATOR, side, "removebreakpoint", AbstractLearner.class,
				Position.DONT_CARE));
		addStep(new RenameOperatorStep(BubbleType.OPERATOR, side, "rename", AbstractLearner.class, "Tree"));
		addStep(new SaveProcessStep(side, "save", "save"));
		addStep(new AddOperatorStep(side, "subprocess", SimpleOperatorChain.class));
		addStep(new OpenSubprocessStep(BubbleType.OPERATOR, side, "opensubprocess", SimpleOperatorChain.class));
		addStep(new LeaveSubprocess(side, "leaveSubprocess"));
		addStep(new OpenSubprocessStep(BubbleType.OPERATOR, side, "opensubprocess", SimpleOperatorChain.class));
		addStep(new AddOperatorStep(side, "subprocesses", Operator.class, new Object[] {}));
	}
}
