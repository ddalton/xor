/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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

package tools.xor.view;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tools.xor.FunctionScope;
import tools.xor.FunctionType;
import tools.xor.Settings;
import tools.xor.providers.jdbc.DBTranslator;

public class QueryStringHelper
{
    /**
     * The only function types supported are
     *   ASC
     *   DESC
     *   FREESTYLE
     * @return list of functions
     */
    private static List<Function> getFreestyleFunctions (Settings settings,
                                                         List<Function> functions) {
        // Consolidate the user supplied filters and the filters
        // defined on the view
        List<Function> temp = new LinkedList<>();
        for(Function function : settings.getAdditionalFunctions()) {
            Function narrowedFunction = function.copy();
            temp.add(narrowedFunction);
        }

        if(functions != null) {
            for (Function function : functions) {
                if (function.type == FunctionType.FREESTYLE) {
                    temp.add(function);
                }
            }
        }

        Collections.sort(temp);

        return temp;
    }

    public static String getFilterClause (Settings settings, List<Function> functions)
    {

        StringBuilder result = new StringBuilder("");
        List<Function> consolidatedFunctions = getFreestyleFunctions(settings, functions);

        for (Function function : consolidatedFunctions) {
            if(!Function.doProcess(function, settings)) {
                continue;
            }
            result.append(function.getQueryString());
        }

        return result.toString();
    }

    public static String getFilterClause (Settings settings, boolean isRoot, List<Function> functions, List<BindParameter> binds, List<BindParameter> relevantParams)
    {

        if(binds == null) {
            binds = new ArrayList<>();
        }
        StringBuilder result = new StringBuilder("");

        Map<String, Object> userParams = settings.getParams();
        List<Function> consolidatedFunctions = getFreestyleFunctions(settings, functions);

        int currentBindPos = 0;
        outer: for (Function function : consolidatedFunctions) {

            // Check if the function is applicable based on root criteria
            if( (function.getScope() == FunctionScope.ROOT && !isRoot) ||
                (function.getScope() == FunctionScope.NOTROOT && isRoot) ) {
                continue;
            }
            if(!Function.doProcess(function, settings)) {
                continue;
            }

            int numBinds = function.getPositionalParamCount();

            // Validation check
            if(currentBindPos+numBinds > binds.size()) {
                throw new IllegalStateException("Not all bind parameters are defined for the native query");
            }

            // We need to check if the bind parameters are going to be satisfied
            // by the user parameters
            int startPos = currentBindPos;
            if(numBinds > 0) {
                for(int i = currentBindPos; i < currentBindPos+numBinds; i++) {

                    // This particular bind parameter does not have a value
                    // so skip this whole function
                    // make sure to update the currentBindPos before doing so
                    if(shouldSkip(isRoot, userParams, binds.get(i).name, function, settings)) {
                    //if(!userParams.containsKey(binds.get(i).name) && !(!settings.isDenormalized() && QueryFragment.systemFields.contains(binds.get(i).name))) {

                        currentBindPos = currentBindPos+numBinds;
                        continue outer;
                    }
                }
                currentBindPos = currentBindPos+numBinds;
            }

            // record the actual bind parameters that need to be honored
            for(int i = startPos; i < currentBindPos; i++) {
                relevantParams.add(binds.get(i));
            }

            result.append(function.getQueryString());
        }

        return result.toString();
    }

    private static boolean shouldSkip(boolean isRoot, Map<String, Object> userParams, String paramName, Function function, Settings settings) {
        return (!userParams.containsKey(paramName) &&                                           // Has the user NOT provided the value for this parameter and
            !(!settings.isDenormalized() && QueryFragment.systemFields.contains(paramName)));   // Is this NOT a system parameter (Denormalized query currently not supported)
    }

    public static List<Function> getQueryTreeFunctions (Settings settings, QueryTree queryTree) {
        // Consolidate the user supplied filters and the filters
        // defined on the view
        List<Function> temp = new LinkedList<>();
        for(Function function : settings.getAdditionalFunctions()) {
            Function narrowedFunction = function.copy();
            temp.add(narrowedFunction);
        }
        if(queryTree.getView() != null && queryTree.getView().getFunction() != null) {
            temp.addAll(queryTree.getView().getFunction());
        }

        // We populate only those filters for while all the attributes can be found in
        // the QueryTree
        List<Function> consolidatedFunctions = new LinkedList<>();
        for(Function function : temp) {
            if(function.normalize(queryTree, settings.getDataStore())) {
                consolidatedFunctions.add(function);
            }
        }
        Collections.sort(consolidatedFunctions);

        return consolidatedFunctions;
    }

    public static void initPositionalParamMap (Map<String, List<BindParameter>> positionByName, List<BindParameter> paramList, boolean preferAttr) {
        int position = 1; // JDBC starts at 1

        positionByName.clear();
        if (paramList != null) {
            for (BindParameter param : paramList) {
                param.position = position++;

                String attrName = param.attribute;
                if(!preferAttr || attrName == null) {
                    attrName = param.name;
                }

                List<BindParameter> params = positionByName.get(attrName);
                if(params == null) {
                    params = new ArrayList<>();
                    positionByName.put(attrName, params);
                }
                params.add(param);
            }
        }
    }

    public static void initPositionalParamMap (Map<String, List<BindParameter>> positionByName, List<BindParameter> paramList) {
        initPositionalParamMap(positionByName, paramList, false);
    }

    public static void setParameters (Settings settings,
                                  PreparedStatement statement,
                                  Map<String, List<BindParameter>> positionByName,
                                  Map<String, Object> paramValues)
    {
        DBTranslator translator = DBTranslator.getTranslator(statement);
        if (positionByName != null) {
            for (Map.Entry<String, List<BindParameter>> entry : positionByName.entrySet()) {
                String paramName = entry.getKey();
                if (!paramValues.containsKey(paramName)) {
                    throw new RuntimeException(
                        "Unable to find param value with key: " + paramName);
                }

                List<BindParameter> params = entry.getValue();
                Object value = paramValues.get(paramName);
                for(BindParameter bindParam: params) {
                    if (bindParam.type != null) {
                        int timestampType = BindParameter.getType(bindParam.type);
                        if (timestampType == Types.TIMESTAMP
                            || timestampType == Types.TIMESTAMP_WITH_TIMEZONE) {
                            bindParam.setDateFormat(settings.getDateFormat());
                        }
                    }
                    // bind by position
                    bindParam.setValue(statement, translator, value);
                }
            }
        }
    }
}
