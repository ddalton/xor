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

package tools.xor;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import tools.xor.generator.DateRange;
import tools.xor.generator.Range;

import java.math.BigDecimal;

public class BigDecimalType extends SimpleType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private BigDecimal min = BigDecimal.ONE;
	private BigDecimal max = new BigDecimal((new Long(Long.MAX_VALUE)).toString());

	public BigDecimal getMin() {
		return min;
	}

	public void setMin(BigDecimal min) {
		this.min = min;
	}

	public BigDecimal getMax() {
		return max;
	}

	public void setMax(BigDecimal max) {
		this.max = max;
	}

	public BigDecimalType(Class<?> clazz) {
		super(clazz);
	}	
	
	public Object generate(Settings settings, Property property) {
		BigDecimal minimum = this.min;
		BigDecimal maximum = this.max;

		ExtendedProperty ep = (ExtendedProperty) property;
		if(ep.getGenerator() != null) {
			return ep.getGenerator().getBigDecimal();
		}

		return maximum.subtract(minimum).multiply( new BigDecimal( (new Double(Math.random())).toString() ) );
	}		
}
