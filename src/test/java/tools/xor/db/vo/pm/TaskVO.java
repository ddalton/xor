/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.db.vo.pm;

import java.util.Date;
import java.util.List;
import java.util.Set;

import tools.xor.db.vo.base.IdentityVO;
import tools.xor.db.vo.base.PersonVO;

public class TaskVO extends IdentityVO {	
		
	private Set<TaskVO> taskChildren;

	public Set<TaskVO> getTaskChildren() {
		return this.taskChildren;
	}

	public void setTaskChildren(Set<TaskVO> taskChildren) {
		this.taskChildren = taskChildren;
	}
	
	private List<TaskVO> dependants;

	public List<TaskVO> getDependants() {
		return this.dependants;
	}

	public void setDependants(List<TaskVO> dependencies) {
		this.dependants = dependencies;
	}	

	private TaskVO taskParent;

	public TaskVO getTaskParent() {
		return this.taskParent;
	}

	public void setTaskParent(TaskVO taskParent) {
		this.taskParent = taskParent;
	}

	private String taskUri;

	public void setTaskUri(String value) {
		this.taskUri = value;
	}
	public String getTaskUri() {
		return this.taskUri;
	}
	
	private Integer depSeq;
	
	public Integer getDepSeq() {
		return depSeq;
	}
	
	public void setDepSeq(Integer value) {
		this.depSeq = value;
	}

	private PersonVO assignedTo;
	
	public PersonVO getAssignedTo() {
		return assignedTo;
	}

	public void setAssignedTo(PersonVO assignedTo) {
		this.assignedTo = assignedTo;
	}
	
	private TaskDetailsVO taskDetails;	
	
	public TaskDetailsVO getTaskDetails() {
		return taskDetails;
	}

	public void setTaskDetails(TaskDetailsVO taskDetails) {
		this.taskDetails = taskDetails;
	}	
	
	private TaskVO auditTask;	
	
	public TaskVO getAuditTask() {
		return auditTask;
	}	
	
	public void setAuditTask(TaskVO audit) {
		this.auditTask = audit;
	}	
	
	// The task related to the audit
	private TaskVO auditedTask;	
	
	public TaskVO getAuditedTask() {
		return auditedTask;
	}	
	
	public void setAuditedTask(TaskVO value) {
		this.auditedTask = value;
	}		

	private QuoteVO quote;
	
	public QuoteVO getQuote() {
		return quote;
	}

	public void setQuote(QuoteVO quote) {
		this.quote = quote;
	}
	
	private Date scheduledFinish;	
	
	public Date getScheduledFinish() {
		return scheduledFinish;
	}

	public void setScheduledFinish(Date scheduledFinish) {
		this.scheduledFinish = scheduledFinish;
	}
	
	public TaskVO getAlternateTask() {
		return alternateTask;
	}

	public void setAlternateTask(TaskVO alternateTask) {
		this.alternateTask = alternateTask;
	}

	// The task related to the audit
	private TaskVO alternateTask;
	
	private PersonVO ownedBy;

	public PersonVO getOwnedBy() {
		return this.ownedBy;
	}

	public void setOwnedBy(PersonVO ownedBy) {
		this.ownedBy = ownedBy;
	}	

	private String subTask;

	public String getSubTask() {
		return subTask;
	}

	public void setSubTask(String subTask) {
		this.subTask = subTask;
	}	
}
