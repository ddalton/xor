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

public enum ProcessingStage {
	CREATE,              // This stage is responsible for creating all the new objects
	UPDATE,              // This stage can be invoked multiple times, if migration needs to be performed. See Schema Migration.docx for more details.
	                     //   Step 1. This stage is responsible for creating the update actions
                         //   Step 2. The actions are run at the end of the stage 
	POSTLOGIC,           // Any post action methods are processed here	
}
