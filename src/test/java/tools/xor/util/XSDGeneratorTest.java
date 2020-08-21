package tools.xor.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import tools.xor.Type;
import tools.xor.db.base.Person;
import tools.xor.db.pm.Task;
import tools.xor.service.AggregateManager;
import tools.xor.service.DataModel;
import tools.xor.util.xpath.XPathLexer;
import tools.xor.util.xpath.XPathParser;
import tools.xor.util.xsd.XSDGenerator;
import tools.xor.util.xsd.XSDVisitor;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:/spring-jpa-test.xml"})
@Transactional
public class XSDGeneratorTest {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
	
	@Autowired(required = true)
	protected AggregateManager aggregateManager;
	
	@Test
	public void testHeader() throws Exception {
		DataModel das = aggregateManager.getDataModel(); 

		Type personType = das.getShape().getType(Person.class);
		logger.info("Task XSD document: " + (new XSDGenerator()).generate(new XSDVisitor(), personType));
	}

	@Test
	public void testTask() throws Exception {
		DataModel das = aggregateManager.getDataModel(); 

		Type taskType = das.getShape().getType(Task.class);
		logger.info("Task XSD document: " + (new XSDGenerator()).generate(new XSDVisitor(), taskType));
	}

	/**
	 * Commenting out this test as it has an issue with Blob
	 * @throws IOException
	 * @throws JAXBException
	 */
	//@Test
	public void testDOM() throws IOException, JAXBException {

		final List results = new ArrayList();
		
		// generate the schema
		JAXBContext context = JAXBContext.newInstance(Task.class);
	    context.generateSchema(
	            // need to define a SchemaOutputResolver to store to
	            new SchemaOutputResolver()
	            {
	                @Override
	                public Result createOutput( String ns, String file )
	                        throws IOException
	                {
	                    // save the schema to the list
	                    DOMResult result = new DOMResult();
	                    result.setSystemId( file );
	                    results.add( result );
	                    return result;
	                }
	            } );
		
	
	 // output schema via System.out
	    DOMResult domResult = (DOMResult) results.get( 0 );
	    Document doc = (Document) domResult.getNode();
	    OutputFormat format = new OutputFormat( doc );
	    format.setIndenting( true );
	    XMLSerializer serializer = new XMLSerializer( System.out, format );
	    serializer.serialize( doc );
	}
	
	
    //Pretty-print a tree. Inspired by CommonTree's docs:
    //http://www.antlr3.org/api/Java/org/antlr/runtime/tree/CommonTree.html
    private static void showTree(CommonTree tree, int indent) {
        for (int i = 0; i < indent; i++)
            System.out.print(' ');
        logger.info(tree);
        if (tree.getChildren() == null)
            return;
        
        logger.debug("Number of children: " + tree.getChildren());
        for (Object childObj : tree.getChildren()) {
            try {
                showTree((CommonTree) childObj, indent + 2);
            } catch (Exception e) {
                e.printStackTrace(); //And resume from next child!
            }
        }
    }

    private static void showTree(String src) throws RecognitionException {
    	XPathLexer lexer = new XPathLexer(new ANTLRStringStream(src));
    	XPathParser parser = new XPathParser(new CommonTokenStream(lexer));

    	XPathParser.main_return r = parser.main();
    	CommonTree tree = (CommonTree)r.getTree();

    	logger.info(tree.toStringTree());
    	showTree(tree, 2);
    }
	
	/**
	 * Parse XPATH, uses the examples in the following link:
	 * http://msdn.microsoft.com/en-us/library/ms256471(v=vs.110).aspx
	 * 
	 * Create unit tests for each of the examples and create the sample XML document to work on.
	 * @throws RecognitionException 
	 */
	@Test
	public void parseXPATH1() throws RecognitionException {
		//String src = "//author";
		//String src = "book[/bookstore/@specialty=@style]";
		String src = "//title[@lang]";
		showTree(src);
	}
	
	@Test
	public void parseXPATH2() throws RecognitionException {
		//String src = "//author";
		//String src = "book[/bookstore/@specialty=@style]";
		String src = "ancestor::book[author][1] | (book/author)[last()]";
		showTree(src);
	}
	
	@Test
	public void parseXPATH3() throws RecognitionException {
		String src = "//.";
		showTree(src);
	}
}
