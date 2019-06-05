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
package com.rapidminer.connection.util;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ProgressThreadStoppedException;
import com.rapidminer.tools.ProgressListener;


/**
 * Test for {@link TestExecutionContext}
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class TestExecutionContextImplTest {

	@Test
	public void testWithSubjectNoProgressThread(){
		String subject = RandomStringUtils.randomAlphabetic(10);
		TestExecutionContext<String> context  = new TestExecutionContextImpl<>(subject);
		Assert.assertEquals(subject, context.getSubject());
		// Check that nothing explodes
		context.getProgressListener().setTotal(100);
		context.getProgressListener().setCompleted(5);
		context.getProgressListener().setMessage("Nearly there");
		context.getProgressListener().complete();
		context.checkCancelled();
	}


	@Test
	public void testNoSubjectWithProgressThread() {
		ProgressThread thread = new ProgressThread("test") {
			@Override
			public void run() {
				// does nothing
			}
		};
		String subject = null;
		TestExecutionContext<String> context = new TestExecutionContextImpl<>(subject, thread);
		Assert.assertEquals(subject, context.getSubject());
		// Check that nothing explodes
		context.getProgressListener().setTotal(100);
		context.getProgressListener().setCompleted(5);
		context.getProgressListener().setMessage("Nearly there");
		context.getProgressListener().complete();
		context.checkCancelled();
		ProgressListener listener = context.getProgressListener();
		thread.cancel();
		try {
			context.checkCancelled();
			Assert.fail("Should be stopped");
		} catch (ProgressThreadStoppedException e){
			// do nothing
		}
		try {
			context.getProgressListener();
			Assert.fail("Should be stopped");
		} catch (ProgressThreadStoppedException e){
			// do nothing
		}
		try {
			context.getSubject();
			Assert.fail("Should be stopped");
		} catch (ProgressThreadStoppedException e){
			// do nothing
		}
		try {
			listener.setMessage("Should be stopped");
			Assert.fail("Should be stopped");
		} catch (ProgressThreadStoppedException e){
			// do nothing
		}
	}
}