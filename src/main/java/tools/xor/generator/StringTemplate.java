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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.HierarchyGenerator;
import tools.xor.Property;
import tools.xor.SharedCounterGenerator;
import tools.xor.util.graph.StateGraph;

public class StringTemplate extends DefaultGenerator implements GeneratorRecipient
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    
    private static final Set<String> visitorTokens = new HashSet<>();
    
    static {
        visitorTokens.add(VISITOR_CONTEXT);
        
        for(int i = 0; i < MAX_VISITOR_CONTEXT; i++) {
            visitorTokens.add(getContextWithIndex(i));
        }
    }
    
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
    
    public static class QueryVisitor {
        private Map<String, List<Integer>> bindPositions = new HashMap<>();

        private void addPosition(String token, int pos) {
            List<Integer> positions = bindPositions.get(token);
            if (positions == null) {
                positions = new ArrayList<>();
                bindPositions.put(token, positions);
            }

            positions.add(pos);
        }

        public String process(String sql) {

            int startIndex = 0;
            int endIndex = 0;
            int previousEndIndex = endIndex;
            int position = 1;
            String pattern = getContextPrefix();
            StringBuilder modifiedSql = new StringBuilder();

            // is end of token found
            while (endIndex != -1 && startIndex != -1) {
                startIndex = sql.indexOf(pattern, endIndex);
                if (startIndex != -1) {
                    endIndex = sql.indexOf(TOKEN_END, startIndex + pattern.length() - 1);
                    if (endIndex != -1) {
                        // adjust for length of closing bracket
                        endIndex += 1;

                        String token = sql.substring(startIndex, endIndex);
                        if (visitorTokens.contains(token)) {
                            addPosition(token, position++);
                            modifiedSql.append(sql.substring(previousEndIndex, startIndex)).append("?");
                            startIndex = endIndex;
                        }

                        previousEndIndex = endIndex;
                    }
                }
            }
            modifiedSql.append(sql.substring(previousEndIndex));

            return modifiedSql.toString();
        }

        public Map<String, List<Integer>> getBindPositions() {
            return this.bindPositions;
        }
        
        public void bindPositions(PreparedStatement ps, StateGraph.ObjectGenerationVisitor visitor) throws SQLException {
            if(bindPositions.size() > 0) {
                for(Map.Entry<String, List<Integer>> entry: bindPositions.entrySet()) {
                    if(entry.getKey().equals(VISITOR_CONTEXT)) {
                        setBindValues(ps, visitor.getContext(), entry.getValue());
                    } else {
                        int contextIndex = getIndexFromContext(entry.getKey());
                        setBindValues(ps, visitor.getContext(contextIndex), entry.getValue());
                    }
                }
            }
        }
    }
    
    private static void setBindValues(PreparedStatement ps, Object obj, List<Integer> positions) throws SQLException {
        for(Integer pos: positions) {
            ps.setObject(pos, obj);
        }
    }
    
    /**
     * Function used to create a PreparedStatement object after processing all visitor tokens in the SQL.
     * The values are retrieved from the visitor object and bound to the prepared statement.
     * 
     * @param sql containing visitor tokens (if any)
     * @param connection used to create the PreparedStatement object
     * @param visitor object containing the token values
     * @return processed PreparedStatement object
     * @throws SQLException any error in SQL processing
     */
    public static PreparedStatement getStatement(String sql, Connection connection, StateGraph.ObjectGenerationVisitor visitor) throws SQLException {
        QueryVisitor qv = new QueryVisitor();
        sql = qv.process(sql);
        PreparedStatement ps = connection.prepareStatement(sql);
        
        qv.bindPositions(ps, visitor);        
        return ps;
    }
    
    private static String getContextPrefix() {
        return VISITOR_CONTEXT.substring(0, VISITOR_CONTEXT.length()-1) ;
    }
    
    private static Integer getIndexFromContext(String context) {
        int startIndex = (getContextPrefix() + "_").length();
        int endIndex = context.indexOf(TOKEN_END);
        return Integer.valueOf(context.substring(startIndex, endIndex));
    }    
    
    private static String getContextWithIndex(int i) {
        return getContextPrefix() + "_" + i + TOKEN_END;
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
                    logger.info(String.format("Found evaluator for token %s for input %s", tokenName, input));
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
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return getIntValue(visitor).byteValue();
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return getIntValue(visitor).shortValue();
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getShortValue(visitor).shortValue();
    }

    
    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        String intStr = getStringValue(null, visitor);
        checkIfEmpty(intStr, visitor);
        
        return Integer.parseInt(intStr);
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        String intStr = getStringValue(null, visitor);
        checkIfEmpty(intStr, visitor);
        
        return Long.parseLong(intStr);
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        return new Date(getLongValue(visitor));
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.parseDouble(getStringValue(null, visitor));
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Float.parseFloat(getStringValue(null, visitor));
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigDecimal(getStringValue(null, visitor));
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigInteger(getStringValue(null, visitor));
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return resolve(getValues()[0], this.generator, visitor);
    }  
    
    private void checkIfEmpty(String strVal, StateGraph.ObjectGenerationVisitor visitor) {
        if(strVal == null || "".equals(strVal.trim())) {
            String propertyInfo = "";
            if(visitor.getProperty() != null) {
                propertyInfo = "for property " + visitor.getProperty().getName();
            }
            throw new RuntimeException(String.format("Found an empty value while evaluating %s for %s", getValues()[0], propertyInfo));
        }        
    }
}
