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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import tools.xor.db.base.Identity;
import tools.xor.db.base.Manager;

@Entity
public class Project extends Identity {	
	
	private FinancialSummary financialSummary;
	
	@OneToOne(mappedBy="project", cascade = CascadeType.ALL)	// example of a bi-directional OneToOne relationship
	public FinancialSummary getFinancialSummary() {
		return financialSummary;
	}

	public void setFinancialSummary(FinancialSummary financialSummary) {
		this.financialSummary = financialSummary;
	}		

	private Task rootTask;

	@OneToOne(cascade = CascadeType.ALL, mappedBy="project")
	public Task getRootTask() {
		return this.rootTask;
	}

	public void setRootTask(Task root) {
		this.rootTask = root;
	}

	private Set<Manager> managers;

	@ManyToMany(mappedBy="projects", cascade = CascadeType.ALL )
	public Set<Manager> getManagers() {
		return this.managers;
	}

	public void setManagers(Set<Manager> managers) {
		this.managers = managers;
	}

	private List<Task> tasks;

	@OneToMany(cascade = CascadeType.ALL )
	public List<Task> getTasks() {
		return this.tasks;
	}
	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}
	
	private Map<String, Project> subProjects;

	@OneToMany(cascade = CascadeType.ALL )
	@MapKey(name="name")
	public Map<String, Project> getSubProjects() {
		return this.subProjects;
	}

	public void setSubProjects(Map<String, Project> subProjects) {
		this.subProjects = subProjects;
	}	

	private Set<Project> dependents;

	@OneToMany(mappedBy="dependentUpon")
	public Set<Project> getDependents() {
		return dependents;
	}

	public void setDependents(Set<Project> dependents) {
		this.dependents = dependents;
	}
	
	private Project dependentUpon;

	@ManyToOne
	public Project getDependentUpon() {
		return dependentUpon;
	}

	public void setDependentUpon(Project dependentUpon) {
		this.dependentUpon = dependentUpon;
	}

	private Set<Project> linkedProjects;

	@OneToMany(mappedBy="linkedTo")
	public Set<Project> getLinkedProjects() {
		return linkedProjects;
	}

	public void setLinkedProjects(Set<Project> linkedProjects) {
		this.linkedProjects = linkedProjects;
	}

	private Project linkedTo;

	@ManyToOne
	public Project getLinkedTo() {
		return linkedTo;
	}

	public void setLinkedTo(Project linkedTo) {
		this.linkedTo = linkedTo;
	}
}
