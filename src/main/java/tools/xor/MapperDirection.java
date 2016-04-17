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

public enum MapperDirection {
    EXTERNALTODOMAIN, DOMAINTOEXTERNAL, EXTERNALTOEXTERNAL, DOMAINTODOMAIN;
    
    public MapperDirection toDomain() {
    	if(this == EXTERNALTOEXTERNAL)
    		return EXTERNALTODOMAIN;
    	else if(this == DOMAINTOEXTERNAL)
    		return DOMAINTODOMAIN;
    	else
    		return this;
    }    
    
    public boolean isExternalSource() {
    	return this == EXTERNALTODOMAIN || this == EXTERNALTOEXTERNAL;
    }
    
    public boolean isDomainSource() {
    	return this == DOMAINTOEXTERNAL || this == DOMAINTODOMAIN;
    }    
    
    public boolean isExternalTarget() {
    	return this == DOMAINTOEXTERNAL || this == EXTERNALTOEXTERNAL;
    }
    
    public boolean isDomainTarget() {
    	return this == EXTERNALTODOMAIN || this == DOMAINTODOMAIN;
    }     
}
