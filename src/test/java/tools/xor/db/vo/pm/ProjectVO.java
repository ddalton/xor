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

import java.util.Map;
import java.util.Set;

import tools.xor.db.vo.base.IdentityVO;
import tools.xor.db.vo.base.ManagerVO;

public class ProjectVO extends IdentityVO {	
	
	private FinancialSummaryVO financialSummary;
	
	public FinancialSummaryVO getFinancialSummary() {
		return financialSummary;
	}

	public void setFinancialSummary(FinancialSummaryVO financialSummary) {
		this.financialSummary = financialSummary;
	}		

	private TaskVO rootTask;

	public TaskVO getRootTask() {
		return this.rootTask;
	}

	public void setRootTask(TaskVO root) {
		this.rootTask = root;
	}

	private Set<ManagerVO> managers;

	public Set<ManagerVO> getManagers() {
		return this.managers;
	}

	public void setManagers(Set<ManagerVO> managers) {
		this.managers = managers;
	}
	
	private Map<String, ProjectVO> subProjects;

	public Map<String, ProjectVO> getSubProjects() {
		return this.subProjects;
	}

	public void setSubProjects(Map<String, ProjectVO> subProjects) {
		this.subProjects = subProjects;
	}	
	
}
