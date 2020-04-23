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

package tools.xor.jpa;

import java.util.List;

import javax.annotation.Resource;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.db.base.Person;
import tools.xor.logic.DefaultQueryOperation;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring-jpa-swagger-test.xml" })
@TransactionConfiguration(defaultRollback = true)
@Transactional
public class SwaggerDataModelTest extends DefaultQueryOperation {

    @Resource(name = "amSwagger")
    protected AggregateManager amSwagger;
    
    final String NAME = "GEORGE_WASHINGTON_4";
    final String DISPLAY_NAME = "George Washington";
    final String DESCRIPTION = "First President of the United States of America";
    final String USER_NAME = "gwashington";    

    @Test
    public void buildModel() {
        DataModel model = amSwagger.getModel();
        System.out.println("It was successful!");

        // create person
        Person person = new Person();
        person.setName(NAME);
        person.setDisplayName(DISPLAY_NAME);
        person.setDescription(DESCRIPTION);
        person.setUserName(USER_NAME);
        
        // add a date field, big decimal field, boolean field

        aggregateService.create(person, new Settings());

        // read the person object using a DataObject
        Settings settings = new Settings();
        JSONObject p = new JSONObject();
        p.put("id", person.getId());
        settings.setView(amSwagger.getView("BASICINFO_OQL"));
        Type personType = model.getShape().getType("Person");
        settings.setEntityType(personType);
        settings.setPreFlush(true);
        List<?> toList = amSwagger.query(p, settings);

        assert (toList.size() == 1);
        JSONObject result = null;
        if (JSONObject.class.isAssignableFrom(toList.get(0).getClass()))
            result = (JSONObject) toList.get(0);

        assert (result != null);
        assert (result.get("name").equals(NAME));
        assert (result.get("displayName").equals(DISPLAY_NAME));
        assert (result.get("description").equals(DESCRIPTION));
    }
}
