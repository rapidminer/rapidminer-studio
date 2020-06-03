/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.repository.internal.remote;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Container for the job scheduler REST service POST requests.
 *
 * @author Marco Boeck
 * @since 9.7
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobScheduleRequest {

	@JsonIgnoreProperties(ignoreUnknown = true)
	protected static class Job {

		@JsonIgnoreProperties(ignoreUnknown = true)
		protected static class JobContext {

			@JsonProperty
			@JsonInclude(JsonInclude.Include.NON_NULL)
			private List<String> inputLocations;
			@JsonProperty
			@JsonInclude(JsonInclude.Include.NON_NULL)
			private List<String> outputLocations;
			@JsonProperty
			@JsonInclude(JsonInclude.Include.NON_NULL)
			private Map<String, String> macros;


			private List<String> getInputLocations() {
				return inputLocations;
			}

			private void setInputLocations(List<String> inputLocations) {
				this.inputLocations = inputLocations;
			}

			private List<String> getOutputLocations() {
				return outputLocations;
			}

			private void setOutputLocations(List<String> outputLocations) {
				this.outputLocations = outputLocations;
			}

			private Map<String, String> getMacros() {
				return macros;
			}

			private void setMacros(Map<String, String> macros) {
				this.macros = macros;
			}
		}

		@JsonProperty
		private String location;
		@JsonProperty
		private String queueName;
		@JsonProperty
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private Long maxTTL;
		@JsonProperty
		private JobContext context = new JobContext();


		private JobContext getContext() {
			return context;
		}

		private String getLocation() {
			return location;
		}

		private void setLocation(String location) {
			this.location = location;
		}

		private String getQueueName() {
			return queueName;
		}

		private void setQueueName(String queueName) {
			this.queueName = queueName;
		}

		private Long getMaxTTL() {
			return maxTTL;
		}

		private void setMaxTTL(Long maxTTL) {
			this.maxTTL = maxTTL;
		}
	}

	@JsonProperty
	private Job job = new Job();
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String cronExpression;
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long startAt;
	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long endAt;


	private Job getJob() {
		return job;
	}

	@JsonIgnore
	public String getLocation() {
		return getJob().getLocation();
	}

	/**
	 * Set the location of the process to run. The format depends on which repository the process lives in:
	 * <ul>
	 *     <li>Legacy repository: path relative to the root. Example: '/home/user/myProcess'</li>
	 *     <li>Versioned repository: prefixed by {@code git://}, followed by the repository name ending in {@code .git},
	 *     followed by path relative to the root, followed by the file ending. Example 'git://my-repository.git/processes/myProcess.rmp'</li>
	 * </ul>
	 *
	 * @param location the location, never {@code null}
	 */
	@JsonIgnore
	public void setLocation(String location) {
		getJob().setLocation(location);
	}

	@JsonIgnore
	public String getQueueName() {
		return getJob().getQueueName();
	}

	/**
	 * Set the name of the queue.
	 *
	 * @param queueName the name of the queue to submit the job in. If {@code null}, the {@code DEFAULT} queue will be
	 *                  used
	 */
	@JsonIgnore
	public void setQueueName(String queueName) {
		getJob().setQueueName(queueName);
	}

	@JsonIgnore
	public Long getMaxTTL() {
		return getJob().getMaxTTL();
	}

	/**
	 * Define a maximum time-to-live for the job if desired. Defaults to no limit.
	 *
	 * @param maxTTL the maximum time-to-live in ms before the job gets killed if not yet finished. If {@code null}, no
	 *               limit will be imposed
	 */
	@JsonIgnore
	public void setMaxTTL(Long maxTTL) {
		getJob().setMaxTTL(maxTTL);
	}

	public String getCronExpression() {
		return cronExpression;
	}

	/**
	 * Set the cron expression when to run the job.
	 *
	 * @param cronExpression the cron expression when the job should run. If {@code null}, the job will run immediately
	 *                       and only once
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	public Long getStartAt() {
		return startAt;
	}

	/**
	 * Set the timestamp (ms since epoch) when the job should start for the first time. Use in combination with {@link
	 * #setCronExpression(String)}. If no cron expression is defined, this becomes the timestamp when the job will run
	 * once.
	 *
	 * @param startAt the start point in ms since epoch. If {@code null}, no start time restrictions will be in place
	 */
	public void setStartAt(Long startAt) {
		this.startAt = startAt;
	}

	public Long getEndAt() {
		return endAt;
	}

	/**
	 * Set the timestamp (ms since epoch) after which the job should not be scheduled anymore. Use in combination with
	 * {@link #setCronExpression(String)}. If no cron expression is defined, this has no effect.
	 *
	 * @param endAt the end point in ms since epoch. If {@code null}, no end time restrictions will be in place
	 */
	public void setEndAt(Long endAt) {
		this.endAt = endAt;
	}

	@JsonIgnore
	public Map<String, String> getMacros() {
		return getJob().getContext().getMacros();
	}

	/**
	 * Sets macros into the process context for this job.
	 *
	 * @param macros the macros. If {@code null}, no macros will be used
	 */
	@JsonIgnore
	public void setMacros(Map<String, String> macros) {
		getJob().getContext().setMacros(macros);
	}

	@JsonIgnore
	public List<String> getInputLocations() {
		return getJob().getContext().getInputLocations();
	}

	/**
	 * Sets input repository locations into the process context for this job.
	 *
	 * @param inputLocations the input repository locations. If {@code null}, none will be used
	 */
	@JsonIgnore
	public void setInputLocations(List<String> inputLocations) {
		getJob().getContext().setInputLocations(inputLocations);
	}

	@JsonIgnore
	public List<String> getOutputLocations() {
		return getJob().getContext().getOutputLocations();
	}

	/**
	 * Sets output repository locations into the process context for this job.
	 *
	 * @param outputLocations the output repository locations. If {@code null}, none will be used
	 */
	@JsonIgnore
	public void setOutputLocations(List<String> outputLocations) {
		getJob().getContext().setOutputLocations(outputLocations);
	}

	@JsonProperty
	private boolean isForce() {
		// need to set this property to false as by default it is true in the Rest API
		return false;
	}
}
