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

public class ByteType extends SimpleType {

	private byte min = Byte.MIN_VALUE;
	private byte max = Byte.MAX_VALUE;	

	public byte getMin() {
		return min;
	}

	public void setMin(byte min) {
		this.min = min;
	}

	public byte getMax() {
		return max;
	}

	public void setMax(byte max) {
		this.max = max;
	}

	public ByteType(Class<?> clazz) {
		super(clazz);
	}
	
	public Object generate(Settings settings, Property property) {
		long range = getMax() - getMin();
		return (byte) (getMin() + (Math.random() * range));
	}		
}
