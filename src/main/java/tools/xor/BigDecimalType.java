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
import org.json.JSONObject;
import tools.xor.generator.DateRange;
import tools.xor.generator.Range;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

public class BigDecimalType extends SimpleType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private BigDecimal min = BigDecimal.ONE;
	private BigDecimal max = new BigDecimal((new Long(Integer.MAX_VALUE)).toString());

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

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {
		BigDecimal minimum = this.min;
		BigDecimal maximum = this.max;

		int scale = 0;
		ExtendedProperty ep = (ExtendedProperty) property;
		if(ep.getGenerator() != null) {
			return ep.getGenerator().getBigDecimal();
		} else {
			if (ep.getConstraints().containsKey(Constants.XOR.CONS_SCALE)) {
				scale = (int)ep.getConstraints().get(Constants.XOR.CONS_SCALE);
			}
			if (ep.getConstraints().containsKey(Constants.XOR.CONS_PRECISION)) {
				int precision = (int)ep.getConstraints().get(Constants.XOR.CONS_PRECISION);
				maximum = new BigDecimal(BigInteger.TEN.pow(precision));
			}
		}

		BigDecimal result = maximum.subtract(minimum).multiply( new BigDecimal( (new Double(Math.random())).toString() ) );
		result = result.setScale(scale, RoundingMode.HALF_UP);

		return result;
	}		
}
