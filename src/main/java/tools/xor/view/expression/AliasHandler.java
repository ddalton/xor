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
 * to appear multiple times in the query result depending on different filter criteria
 * for each alias.
 * A property can have more than one alias.
 *
 */
public class AliasHandler extends FunctionHandler
{
    private String subclassName;
    private String viewName;
    private boolean interQuery;

    @Override
    public void init(List<String> args) {
        // name is the alias name and the argument is the attribute path
        normalizedNames.put(args.get(0), null);

        if(args.size() > 1) {
            subclassName = args.get(1);
        }
        if(args.size() > 2) {
            viewName = args.get(2);
        }
        if(args.size() > 3) {
            interQuery = Boolean.parseBoolean(args.get(3));
        }
    }

    public String getSubclassName() {
        return this.subclassName;
    }

    public String getViewName() {
        return this.viewName;
    }

    public boolean isInterQuery() {
        return this.interQuery;
    }
}
