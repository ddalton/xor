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

package tools.xor.generator;

import tools.xor.Property;
import tools.xor.util.graph.StateGraph;

import java.util.HashMap;
import java.util.Map;

public class StringTemplate extends DefaultGenerator
{
    public StringTemplate (String[] arguments)
    {
        super(arguments);
    }

    public static interface TokenEvaluator {
        public String evaluate(String input, StateGraph.ObjectGenerationVisitor visitor);
    }

    public static String[] getTokens() {
        return new String[] {
            ENTITY_SIZE,
            THREAD_NO,
            GLOBAL_SEQ,
            VISITOR_CONTEXT
        };
    }

    private static final Map<String, TokenEvaluator> evaluators = new HashMap<>();
    static {
        evaluators.put(
            THREAD_NO, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    return input.replace(
                        THREAD_NO,
                        new Long(Thread.currentThread().getId()).toString());
                }
            });

        evaluators.put(
            ENTITY_SIZE, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    return visitor.getSettings() != null ? input.replace(
                        ENTITY_SIZE,
                        visitor.getSettings().getEntitySize().name()) : input;
                }
            });

        evaluators.put(
            GLOBAL_SEQ, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    return visitor.getSettings() != null ? input.replace(
                        GLOBAL_SEQ,
                        new Integer(visitor.getSettings().getGlobalSeq()).toString()) : input;
                }
            });

        evaluators.put(
            VISITOR_CONTEXT, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    return visitor.getContext() != null ? input.replace(
                        VISITOR_CONTEXT,
                        visitor.getContext().toString()) : input;
                }
            });
    }

    public static String resolve(String input, StateGraph.ObjectGenerationVisitor visitor) {
        for(String tokenName: getTokens()) {
            input = evaluators.get(tokenName).evaluate(input, visitor);
        }

        return input;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return resolve(getValues()[0], visitor);
    }
}
