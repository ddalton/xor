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
import tools.xor.util.graph.StateGraph;

import java.math.BigInteger;
import java.util.List;

public class BigIntegerType extends SimpleType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private BigInteger min = BigInteger.ONE;
	private BigInteger max = new BigInteger( (new Long(Integer.MAX_VALUE)).toString() );

	public BigInteger getMin() {
		return min;
	}

	public void setMin(BigInteger min) {
		this.min = min;
	}

	public BigInteger getMax() {
		return max;
	}

	public void setMax(BigInteger max) {
		this.max = max;
	}

	public BigIntegerType(Class<?> clazz) {
		super(clazz);
	}	

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {
		BigInteger minimum = this.min;
		BigInteger maximum = this.max;

		ExtendedProperty ep = (ExtendedProperty) property;
		if(ep.getGenerator() != null) {
			return ep.getGenerator().getBigInteger();
		}

		long range = maximum.longValue() - minimum.longValue();
		return new BigInteger((new Long((long) (minimum.longValue() + (Math.random() * range)))).toString());
	}		
}
