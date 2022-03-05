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

package tools.xor.logic;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.Settings;
import tools.xor.service.AggregateManager;
import tools.xor.service.DynamicShape;
import tools.xor.view.View;

public class DefaultQueryShape {
    @Autowired
    protected AggregateManager aggregateManager;
    
    @BeforeEach
    public void setup() {
        // Data created will be rolled back, so no need for an explicit teardown
        // TODO: populate/generate data using domain type shape
        
        // This logic should be in the actual provider test class
        // e.g., JPAQueryShapeTest.java
    }

    @Test
    public void queryTaskSimpleOQL() {
        DynamicShape shape = new DynamicShape("TaskModel", null, aggregateManager.getDataModel());
        shape.process("QueryShapeViews.xml", null, null);
        aggregateManager.getDataModel().addShape(shape);
        aggregateManager.getDataModel().setActive(shape);
        
        // read the person object using a DataObject
        Settings settings = new Settings();
        View view = shape.getView("TaskSimpleOQL");
        view = view.copy();
        settings.setView(view);
        List<?> toList = aggregateManager.query(new Object(), settings);        
    }
}
