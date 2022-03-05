/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2017, Dilip Dalton
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.OpenType;
import tools.xor.providers.jdbc.JDBCDataModel;

public class DomainShape extends AbstractShape
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    protected Shape jdbcShape; // refers to the JDBC shape type system if available

    public DomainShape(String name, Shape parent, DataModel das) {
        super(name, parent, das);
    }
    
    public DomainShape(String name, Shape parent, DataModel das, Inheritance typeInheritance) {
        super(name, parent, das, typeInheritance);
    }    
    
    /**
     * A simple mechanism to signal an event that the type structure has changed.
     * Currently supported only for JDBC to signal that a temporary table has been added.
     */
    public void signalEvent () {
        assert this.das != null : "Shape is not specific to a DAS. Create a child shape with the DAS populated.";
        
        if(das instanceof JDBCDataModel) {
            ((JDBCDataModel)this.das).addNewTypes(this);
        }
    }

    public void setJDBCShape(Shape shape) {
        assert this.das != null : "Shape is not specific to a DAS. Create a child shape with the DAS populated.";
        
        if(this.das instanceof JDBCDataModel) {
            throw new IllegalStateException("Setting the JDBC shape is not allowed on a JDBC shape, but only on an ORM based shape");
        }
        this.jdbcShape = shape;
    }

    public boolean hasTable(String tableName) {
        assert this.das != null : "Shape is not specific to a DAS. Create a child shape with the DAS populated.";
        
        Shape shape = (this.das instanceof JDBCDataModel) ? this : this.jdbcShape;

        if(shape == null) {
            throw new RuntimeException("hasTable needs jdbcShape to be initialized or be invoked on a JDBC shape");
        }

        return shape.getType(tableName) != null;
    }

    public void addOpenType(OpenType type) {
        if(types.containsKey(type.getName())) {
            throw new RuntimeException("A type with the same name exists, please choose a different name for the open type: " + type.getName());
        }

        type.setShape(this);
        type.setProperty();
        addType(type.getName(), type);
    }
}
