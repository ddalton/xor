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

package tools.xor.view.expression;

import java.util.List;

/**
 * Support alternative name for a query property, thus allowing the same entity
 * to appear multiple times in the query result depending on different filter
 * criteria for each alias. A property can have more than one alias.
 * 
 * For user provided query, the alias can be more flexible i.e., the result
 * object graph can be flattened through aliases and doesn't have to mirror the
 * domain object graph
 * 
 * 
 * Arguments:
 * argument 0 - original property path
 * argument 1 - If present, then argument 0 is resolved using this type - argument1.getProperty(argument0)
 *              else argument 1 is resolved using the root type.
 *              if argument 0 is null, then the alias is not based on an existing type, but is dynamically specified
 * argument 2 - view anchored in this alias. Alias does not have to refer to just an entity type but can also refer to a view
 *              For a dynamic alias (not anchored in an existing type), the view has to be a dynamic view
 *              If a view is specified, then argument1(type) should either be OBJECT or LIST and
 *              if LIST, the elementType is OBJECT (element EntityName is the view name)
 *              If view is not specified, then additional alias entries need to be present to define the object
 * argument 3 - identifies the role, optional value. Can take values of IDENTIFIER, VERSION, OWNERID (needed for linking with parent objects)
 * argument 4 - If argument 1 is of type LIST, then the elementType value needs to be specified
 * 
 * Note: A QueryType can exist without a basedOn type. But then all its properties should be expressible using aliases.
 * 
 * Even if there is a single alias where argument 1 is provided, it most likely is a custom query (user specified), unless
 * the JOIN condition to this type can be inferred.
 * 
 * In most cases, we need to create QueryType instances when we are working with aliases and custom queries.
 *
 */
public class AliasHandler extends FunctionHandler
{
    private String type;
    private String viewName;  // EntityName or Element EntityName
    private String role;      // Identifier, composite_#  where # - 1..n
    private String elementType;

    @Override
    public void init(List<String> args) {
        // name is the alias name and the argument is the attribute path
        normalizedNames.put(args.get(0), null);

        if(args.size() > 1) {
            type = args.get(1);
        }
        if(args.size() > 2) {
            viewName = args.get(2);
        }
    }

    public String getType() {
        return this.type;
    }

    public String getViewName() {
        return this.viewName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }
}
