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

package tools.xor.db.pm;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;

import tools.xor.ExtendedProperty;
import tools.xor.annotation.XorInput;
import tools.xor.annotation.XorOutput;
import tools.xor.annotation.XorRead;
import tools.xor.annotation.XorUpdate;
import tools.xor.db.base.Identity;
import tools.xor.db.base.Person;

@Entity
public class Task extends Identity {	
		
	private Set<Task> taskChildren;

	@OneToMany(mappedBy="taskParent", cascade = CascadeType.ALL, orphanRemoval = true )
	public Set<Task> getTaskChildren() {
		return this.taskChildren;
	}

	public void setTaskChildren(Set<Task> taskChildren) {
		this.taskChildren = taskChildren;
	}
	
	private List<Task> dependants;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true )
	@OrderColumn(name="dep_seq")
	@OrderBy("depSeq")
	public List<Task> getDependants() {
		return this.dependants;
	}

	public void setDependants(List<Task> dependencies) {
		this.dependants = dependencies;
	}	
	
	private Integer depSeq;

	public Integer getDepSeq() {
		return depSeq;
	}

	public void setDepSeq(Integer depSeq) {
		this.depSeq = depSeq;
	}	
	
	private Task taskParent;

	@ManyToOne
	public Task getTaskParent() {
		return this.taskParent;
	}

	public void setTaskParent(Task taskParent) {
		this.taskParent = taskParent;
	}

	private String taskUri;

	public void setTaskUri(String value) {
		this.taskUri = value;
	}
	public String getTaskUri() {
		return this.taskUri;
	}

	private Person assignedTo;
	
	@OneToOne
	public Person getAssignedTo() {
		return assignedTo;
	}

	public void setAssignedTo(Person assignedTo) {
		this.assignedTo = assignedTo;
	}
	
	private TaskDetails taskDetails;	
	
	@OneToOne(mappedBy="task")
	public TaskDetails getTaskDetails() {
		return taskDetails;
	}

	public void setTaskDetails(TaskDetails taskDetails) {
		this.taskDetails = taskDetails;
	}	
	
	private Task auditTask;	
	
	@OneToOne(mappedBy="auditedTask", cascade = CascadeType.ALL, orphanRemoval = true)
	public Task getAuditTask() {
		return auditTask;
	}	
	
	public void setAuditTask(Task audit) {
		this.auditTask = audit;
	}	
	
	// The task related to the audit
	private Task auditedTask;	
	
	@OneToOne
	public Task getAuditedTask() {
		return auditedTask;
	}	
	
	public void setAuditedTask(Task value) {
		this.auditedTask = value;
	}		

	private Quote quote;
	
	@OneToOne(mappedBy="task", cascade = CascadeType.ALL, optional = true, orphanRemoval = true)	
	public Quote getQuote() {
		return quote;
	}

	public void setQuote(Quote quote) {
		this.quote = quote;
	}
	
	private Date scheduledFinish;	
	
	public Date getScheduledFinish() {
		return scheduledFinish;
	}

	public void setScheduledFinish(Date scheduledFinish) {
		this.scheduledFinish = scheduledFinish;
	}
	
	@OneToOne	
	public Task getAlternateTask() {
		return alternateTask;
	}

	public void setAlternateTask(Task alternateTask) {
		this.alternateTask = alternateTask;
	}

	// The task related to the audit
	private Task alternateTask;
	
	private Person ownedBy;

	@ManyToOne
	public Person getOwnedBy() {
		return this.ownedBy;
	}

	public void setOwnedBy(Person ownedBy) {
		this.ownedBy = ownedBy;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	@XorRead(property="ItemList")
	public Object retrieveItemList(@XorInput Object object) {
		return null;
	}

	@XorUpdate(property="ItemList", phase= ExtendedProperty.Phase.PRE)
	public void populateItemList(@XorInput Object object) {
		int a = 1;
	}
}
