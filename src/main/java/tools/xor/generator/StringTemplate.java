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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import tools.xor.HierarchyGenerator;
import tools.xor.Property;
import tools.xor.SharedCounterGenerator;
import tools.xor.util.graph.StateGraph;

public class StringTemplate extends DefaultGenerator implements GeneratorRecipient
{
    private Generator generator;

    public StringTemplate (String[] arguments)
    {
        super(arguments);
    }
    
    /**
     * Convenience constructor. Represents a single value for the argument
     * @param argument single value
     */
    public StringTemplate (String argument)
    {
        super(new String[] {argument});
    }    

    @Override public void accept (Generator generator)
    {
        this.generator = generator;
    }

    public static interface TokenEvaluator {
        public String evaluate(String input, Generator generator, StateGraph.ObjectGenerationVisitor visitor);
    }

    public static String[] getTokens() {
        String[] fixed = new String[] {
            ENTITY_SIZE,
            THREAD_NO,
            GLOBAL_SEQ,
            VISITOR_CONTEXT,
            GENERATOR,
            GENERATOR_HIER_ID,
            GENERATOR_PARENT_ID
        };
        
        String[] all = new String[fixed.length+MAX_VISITOR_CONTEXT];
        
        int i = 0;
        for(String token: fixed) {
            all[i++] = token;
        }
        for(int j = 0; j < MAX_VISITOR_CONTEXT; j++) {
            final String contextName = getContextWithIndex(j);
            all[i++] = contextName;
        }
        
        return all;
    }
    
    private static String getContextWithIndex(int i) {
        return VISITOR_CONTEXT.substring(0, VISITOR_CONTEXT.length()-1) + "_" + i + "]";
    }

    private static final Map<String, TokenEvaluator> evaluators = new HashMap<>();
    static {
        evaluators.put(
            THREAD_NO, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  Generator generator,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    // String replace method currently is not performant
                    // will be improved in a later version
                    return StringUtils.replace(input,
                        THREAD_NO,
                        new Long(Thread.currentThread().getId()).toString());
                }
            });

        evaluators.put(
            ENTITY_SIZE, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  Generator generator,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    return visitor.getSettings() != null ? StringUtils.replace(input,
                        ENTITY_SIZE,
                        visitor.getSettings().getEntitySize().name()) : input;
                }
            });

        evaluators.put(
            GLOBAL_SEQ, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  Generator generator,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    return visitor.getSettings() != null ? StringUtils.replace(input,
                        GLOBAL_SEQ,
                        new Long(visitor.getSettings().getAndIncrGlobalSeq()).toString()) : input;
                }
            });

        evaluators.put(
            VISITOR_CONTEXT, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  Generator generator,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    return visitor.getContext() != null ? StringUtils.replace(input,
                        VISITOR_CONTEXT,
                        visitor.getContext().toString()) : input;
                }
            });
        
        for(int i = 0; i < MAX_VISITOR_CONTEXT; i++) {
            final String contextName = getContextWithIndex(i);
            final int contextIndex = i; 
            evaluators.put(
                    contextName, new TokenEvaluator()
                    {
                        @Override public String evaluate (String input,
                                                          Generator generator,
                                                          StateGraph.ObjectGenerationVisitor visitor)
                        {
                            return visitor.getContext(contextIndex) != null ? StringUtils.replace(input,
                                contextName,
                                visitor.getContext(contextIndex).toString()) : input;
                        }
                    });        
        }

        evaluators.put(
            GENERATOR, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  Generator generator,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    String value = null;
                    if(generator != null) {
                        value = generator.getCurrentValue(visitor);
                        if (value == null) {
                            return null;
                        }
                    }

                    return generator != null ? StringUtils.replace(input, GENERATOR, value) : input;
                }
            });

        evaluators.put(
            GENERATOR_HIER_ID, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  Generator generator,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    String value = null;
                    if(generator != null) {
                        SharedCounterGenerator idGen = ((HierarchyGenerator) generator).getCurrentIdGenerator();
                        value = idGen.getCurrentValue(visitor);
                        if (value == null) {
                            return null;
                        }
                    }

                    return generator != null ? StringUtils.replace(input, GENERATOR_HIER_ID, value) : input;
                }
            });

        evaluators.put(
            GENERATOR_PARENT_ID, new TokenEvaluator()
            {
                @Override public String evaluate (String input,
                                                  Generator generator,
                                                  StateGraph.ObjectGenerationVisitor visitor)
                {
                    String value = null;
                    if(generator != null) {
                        HierarchyGenerator parentGen = ((HierarchyGenerator) generator).getCurrentParent();
                        SharedCounterGenerator parentIdGen = (parentGen == null) ? null : parentGen.getIdGenerator();

                        if(parentIdGen != null) {
                            value = parentIdGen.getCurrentValue(visitor);
                        }
                        if (value == null) {
                            return null;
                        }
                    }

                    return generator != null ? StringUtils.replace(input, GENERATOR_PARENT_ID, value) : input;
                }
            });
    }

    private List<TokenEvaluator> relevantEvaluators;

    public String resolve(String input, Generator generator, StateGraph.ObjectGenerationVisitor visitor) {
        if(this.relevantEvaluators == null) {

            this.relevantEvaluators = new ArrayList<>();
            for (String tokenName : getTokens()) {
                if(input.contains(tokenName)) {
                    relevantEvaluators.add(evaluators.get(tokenName));
                }
            }
        }

        for (TokenEvaluator evaluator: relevantEvaluators) {
            input = evaluator.evaluate(input, generator, visitor);
        }

        return input;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return resolve(getValues()[0], this.generator, visitor);
    }
}
