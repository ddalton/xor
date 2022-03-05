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

package tools.xor.view.expression;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a way to specify custom condition clauses for both OQL and SQL.
 *
 */
public class FreestyleHandler extends FunctionHandler
{
    private static final String NAMED_PARAM = "(\\[\\s*([^\\s]*)\\s*\\]+|\\s*:([\\w_]*)\\s*)+";
    private static final String POSITIONAL_PARAM = "([^?]+(\\?))";

    protected String expression;
    protected int paramCount;

    @Override
    public String getQueryString() {
        String queryString = expression;

        // replace with normalized names
        for(Map.Entry<String, String> entry: normalizedNames.entrySet()) {
            queryString = queryString.replaceAll("\\[" + entry.getKey() + "\\]", entry.getValue() );
        }
        return queryString;
    }

    @Override
    public void init(List<String> args) {
        this.expression = args.get(0);

        final Pattern pattern1 = Pattern.compile( NAMED_PARAM );
        final Matcher matcher1 = pattern1.matcher(this.expression);

        while (matcher1.find()) {
            //System.out.println("Full match: " + matcher1.group(0));
            if(matcher1.group(2) != null) {
                normalizedNames.put(matcher1.group(2), null);
            } else if(matcher1.group(3) != null) {
                parameterName.add(matcher1.group(3));
            }
        }

        final Pattern pattern2 = Pattern.compile(POSITIONAL_PARAM);
        final Matcher matcher2 = pattern2.matcher(this.expression);

        while (matcher2.find()) {
            //System.out.println("Full match: " + matcher.group(0));
            //for (int i = 1; i <= matcher.groupCount(); i++) {
            //    System.out.println("Group " + i + ": " + matcher.group(i));
            //}
            paramCount++;
        }
    }

    public int getPositionalParamCount() {

        return paramCount;
    }
}
