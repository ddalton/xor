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

package tools.xor.service;

public class EmptyQueryCapability extends AbstractQueryCapability {

	@Override
	public String getTypeMechanism(String queryAlias) {
		return null;
	}

	@Override
	public String getMapKeyMechanism(String queryAlias) {
		return null;
	}

	@Override
	public String getMapValueMechanism(String queryAlias) {
		return null;
	}

	@Override
	public String getListIndexMechanism(String queryAlias) {
		return null;
	}	
}