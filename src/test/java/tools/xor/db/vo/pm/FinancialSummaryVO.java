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

import java.math.BigDecimal;

import tools.xor.db.vo.base.IdVO;

public class FinancialSummaryVO extends IdVO {	

	private BigDecimal plannedCost;

	public BigDecimal getPlannedCost() {
		return plannedCost;
	}

	public void setPlannedCost(BigDecimal price) {
		this.plannedCost = price;
	}

	private ProjectVO project;
	
	public ProjectVO getProject() {
		return project;
	}

	public void setProject(ProjectVO project) {
		this.project = project;
	}
	
}
