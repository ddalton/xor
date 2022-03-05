package tools.xor.util.xsd;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import tools.xor.Settings;
import tools.xor.Type;

public class XSDVisitor {
	
	public final static String XSD_PREFIX = "xs:";
	
	private Document document;
	private Element  rootElement;
	public Element getRootElement() {
		return rootElement;
	}

	public void setRootElement(Element rootElement) {
		this.rootElement = rootElement;
	}

	private int      apiVersion;
	private Map<Class<?>, String> xsdTypes;
	private Map<Class<?>, String> complexTypes;
	private Stack<Element> callStack;
	
	private void initBuiltinTypes() {
		// Pre-populate known JAVA classes with the corresponding XSD types;
		xsdTypes = new HashMap<Class<?>, String>();
		complexTypes = new HashMap<Class<?>, String>();
		
		xsdTypes.put(String.class, "string");
		xsdTypes.put(BigInteger.class, "integer");
		xsdTypes.put(BigDecimal.class, "decimal");
		xsdTypes.put(int.class, "int");
		xsdTypes.put(long.class, "long");
		xsdTypes.put(short.class, "short");
		xsdTypes.put(float.class, "float");
		xsdTypes.put(double.class, "double");
		xsdTypes.put(boolean.class, "boolean");
		xsdTypes.put(byte.class, "byte");
		xsdTypes.put(char.class, "unsignedShort");
		xsdTypes.put(Integer.class, "int");
		xsdTypes.put(Long.class, "long");
		xsdTypes.put(Short.class, "short");
		xsdTypes.put(Float.class, "float");
		xsdTypes.put(Double.class, "double");
		xsdTypes.put(Boolean.class, "boolean");
		xsdTypes.put(Byte.class, "byte");
		xsdTypes.put(Character.class, "unsignedShort");
		xsdTypes.put(java.util.Date.class, "dateTime");
		
		// TODO: how to handle multiple XSD mappings to the same java type?
		xsdTypes.put(byte[].class, "hexBinary");  // test this
		xsdTypes.put(byte[].class, "base64Binary");
	}
	
	public XSDVisitor() {
		this(Settings.CURRENT_API_VERSION);
	}
	
	public XSDVisitor(int apiVersion) {
		try {
			initBuiltinTypes();
			this.setApiVersion(apiVersion);
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			docFactory.setNamespaceAware(true);
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
			document = docBuilder.newDocument();
			document.setXmlVersion("1.0");
			document.setXmlStandalone(true);
			
			rootElement = document.createElement(XSD_PREFIX + "schema");
			document.appendChild(rootElement);
			rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/","xmlns:xs","http://www.w3.org/2001/XMLSchema");
			
			callStack = new Stack<Element>();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
	
	public String build() {
		return buildHeader();
	}
	
	private String buildHeader()  {

		StringWriter writer;
		try {
			
			DOMSource domSource = new DOMSource(document);
			writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
			transformer.transform(domSource, result);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return writer.toString();
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}
	
	/**
	 * Push the root element into the stack if the subsequent element needs to be added
	 * to the root element
	 */
	public void addToRoot() {
		callStack.push(rootElement);
	}
	
	public void push(Element element) {
		if(!callStack.empty()) {
			callStack.peek().appendChild(element);
		} else {
			rootElement.appendChild(element);
		}
		
		callStack.push(element);
	}
	
	public void pop() {
		callStack.pop();
	}

	public int getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(int apiVersion) {
		this.apiVersion = apiVersion;
	}
	
	public boolean hasType(Type type) {
		return xsdTypes.containsKey(type.getInstanceClass()) || complexTypes.containsKey(type.getInstanceClass());
	}
	
	public String getType(Type type) {
		if(complexTypes.containsKey(type.getInstanceClass())) {
			return complexTypes.get(type.getInstanceClass());
		}
		return XSD_PREFIX + xsdTypes.get(type.getInstanceClass());
	}

	public void setType(Type type, String typeName) {
		
		// Save the type in the mapping before drill down
		complexTypes.put(type.getInstanceClass(), typeName);

	}

}
