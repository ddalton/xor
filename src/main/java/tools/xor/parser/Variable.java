package tools.xor.parser;

import tools.xor.Type;
import tools.xor.service.Shape;

/**
 * http://spec.graphql.org/June2018/#sec-Language.Variables
 *
 * Variable :
 *   $Name
 * VariableDefinitions
 *   (VariableDefinition)
 * VariableDefinition
 *   Variable : Type Default
 * DefaultValue
 *   =Value
 */
public class Variable
{
    private String name;
    private Type type;          // Mapped to an XOR type. This means a Shape instance needs to be active first
    private Value defaultValue; // optional

    public Variable (GraphQLParser.VariableDefinitionContext ctx, Shape shape) {
        // Create a ValueHelper class, that will create instances of value objects based on their type

        this.name = ctx.variable().name().getText();
        this.type = shape.getType(ctx.type_().getText());
        if(this.type == null) {
            throw new RuntimeException(String.format("Unable to find type with name: %s", ctx.type_().getText()));
        }

        if(ctx.defaultValue() != null) {
            this.defaultValue = ValueHelper.createValue(ctx.defaultValue().value());
        }
    }
}
