package tools.xor.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import tools.xor.service.Shape;
import java.util.LinkedList;
import java.util.List;

public class GraphQLRequest
{
    private Shape shape;
    private List<OperationDefinition> operations;
    private List<FragmentDefinition> fragments;

    public GraphQLRequest (Shape shape) {
        this.shape = shape;
    }

    // { __type(name: \"User\") { name fields { name type { name } } } }
    public void execute(String documentText)
    {
        CodePointCharStream charStream = CharStreams.fromString(documentText);
        GraphQLLexer lexer = new GraphQLLexer(charStream);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GraphQLParser parser = new GraphQLParser(tokens);
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        parser.addParseListener(new ParseTreeListener()
        {
            @Override
            public void visitTerminal (TerminalNode terminalNode)
            {

            }

            @Override
            public void visitErrorNode (ErrorNode errorNode)
            {

            }

            @Override
            public void enterEveryRule (ParserRuleContext parserRuleContext)
            {

            }

            @Override
            public void exitEveryRule (ParserRuleContext parserRuleContext)
            {

            }
        });

        GraphQLParser.DocumentContext documentContext = parser.document();
        createDocument(documentContext);
    }

    private void createDocument (GraphQLParser.DocumentContext documentContext)
    {
        List<GraphQLParser.DefinitionContext> defns = documentContext.definition();

        /* XOR supports only OperationDefinition as the type information is extracted
         * from XOR. It is the single source of truth and the user is not allowed to create
         * the types.
         * Introspection is still supported since XOR manages the type system and this
         * can be exposed to the user.
         * Variables are not defined in the type system, but are defined in the view framework.
         * So views in addition to types can be exposed.
         */
        this.operations = new LinkedList();
        this.fragments = new LinkedList<>();
        for (GraphQLParser.DefinitionContext ctx : defns) {
            GraphQLParser.ExecutableDefinitionContext execDefn = ctx.executableDefinition();
            if (execDefn != null) {
                if(execDefn.operationDefinition() != null) {
                    this.operations.add(new OperationDefinition(execDefn.operationDefinition(), this.shape));
                } else if(execDefn.fragmentDefinition() != null) {
                    this.fragments.add(new FragmentDefinition(execDefn.fragmentDefinition(), this.shape));
                }
            }
        }

        if (this.operations.size() == 0) {
            throw new RuntimeException("Atleast one operation definition needs to be provided!");
        }
    }

}
