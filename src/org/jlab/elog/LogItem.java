package org.jlab.elog;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;
import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author ryans
 */
abstract class LogItem {

    public static final String LOG_ENTRY_SCHEMA_URL;
    public static final String COMMENT_SCHEMA_URL;
    public static final String SUBMIT_URL;
    public static final String QUEUE_PATH;

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        LOG_ENTRY_SCHEMA_URL = bundle.getString("LOG_ENTRY_SCHEMA_URL");
        COMMENT_SCHEMA_URL = bundle.getString("COMMENT_SCHEMA_URL");
        SUBMIT_URL = bundle.getString("SUBMIT_URL");
        QUEUE_PATH = bundle.getString("QUEUE_PATH");
    }

    public enum ContentType {

        TEXT, HTML
    };
    protected Document doc;
    protected Element root;
    protected DatatypeFactory typeFactory;
    protected DocumentBuilder builder;
    protected XPath xpath;
    protected XPathExpression lognumberExpression;
    protected XPathExpression lognumberTextExpression;
    protected XPathExpression createdExpression;

    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LogException("Unable to obtain XML document builder.", e);
        }

        try {
            typeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new LogException("Unable to obtain XML datatype factory.", e);
        }

        XPathFactory xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();

        try {
            lognumberExpression = xpath.compile("/Logentry/lognumber");
            lognumberTextExpression = xpath.compile("/Logentry/lognumber/text()");
            createdExpression = xpath.compile("/Logentry/created");
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to construct XML XPath query", e);
        }
    }

    public LogItem() throws LogException {
    }

    public LogItem(String rootTagName) throws LogException {

        doc = builder.newDocument();

        root = doc.createElement(rootTagName);
        doc.appendChild(root);

        appendElementWithText(root, "created", toXMLFormat(new GregorianCalendar()));
    }

    protected final String toXMLFormat(GregorianCalendar calendar) {
        return typeFactory.newXMLGregorianCalendar(calendar).normalize().toXMLFormat();
    }

    protected final GregorianCalendar toGregorianCalendar(String xmlDateTime) {
        return typeFactory.newXMLGregorianCalendar(xmlDateTime).normalize().toGregorianCalendar();
    }

    protected final void appendElementWithText(Element parent, String tagName, String text) {
        Element child = doc.createElement(tagName);
        parent.appendChild(child);
        child.setTextContent(text);
        //Text textNode = doc.createTextNode(text);
        //child.appendChild(textNode);
    }

    protected final void appendCommaDelimitedElementsWithText(Element parent, String tagName, String list) {
        String[] tokens = list.split(",");

        for (String token : tokens) {
            appendElementWithText(parent, tagName, token.trim());
        }
    }

    protected final void appendCommaDelimitedElementsWithGrandchildAndText(Element parent, String childTagName, String grandchildTagName, String list) {
        String[] tokens = list.split(",");

        for (String token : tokens) {
            Element child = doc.createElement(childTagName);
            parent.appendChild(child);
            appendElementWithText(child, grandchildTagName, token.trim());
        }
    }

    protected final String buildCommaDelimitedFromText(NodeList nodes) {
        StringBuilder csvBuilder = new StringBuilder();

        for (int i = 0; i < nodes.getLength(); i++) {
            csvBuilder.append(nodes.item(i).getTextContent());
            csvBuilder.append(",");
        }

        // Remove trailing comma
        csvBuilder.deleteCharAt(csvBuilder.length() - 1);

        return csvBuilder.toString();
    }

    protected final void removeChildren(Element parent) {
        while (parent.hasChildNodes()) {
            parent.removeChild(parent.getFirstChild());
        }
    }

    public void addAttachment(String filename) throws LogException {
    }

    public void addAttachment(String filename, String caption, String mimeType) throws LogException {
    }

    public void setLogNumber(Long lognumber) throws LogException {
        try {
            Element lognumberElement = (Element) lognumberExpression.evaluate(doc, XPathConstants.NODE);

            if (lognumberElement == null) {
                lognumberElement = doc.createElement("lognumber");
                root.appendChild(lognumberElement);
            }

            lognumberElement.setTextContent(lognumber.toString());
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }
    }

    public Long getLogNumber() throws LogException {
        Long lognumber = null;

        try {
            String lognumberStr = (String) lognumberTextExpression.evaluate(doc, XPathConstants.STRING);

            if (lognumberStr != null && !lognumberStr.isEmpty()) { // TODO: C++ impl throws exception if null
                try {
                    lognumber = Long.parseLong(lognumberStr);
                } catch (NumberFormatException e) {
                    throw new LogException("Unable to obtain log number due to non-numeric format.", e);
                }
            }
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }

        return lognumber;
    }

    public void setCreated(GregorianCalendar created) throws LogException {
        try {
            Element createdElement = (Element) createdExpression.evaluate(doc, XPathConstants.NODE);
            createdElement.setTextContent(toXMLFormat(created));
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }
    }

    public GregorianCalendar getCreated() throws LogException {
        GregorianCalendar created = null;

        try {
            Element createdElement = (Element) createdExpression.evaluate(doc, XPathConstants.NODE);
            String createdStr = createdElement.getTextContent();
            created = toGregorianCalendar(createdStr);
        } catch (XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }

        return created;
    }

    public String getXML() throws LogException {
        String xml = null;
        TransformerFactory factory = TransformerFactory.newInstance();

        try {
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no"); // Ignored?
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            DOMSource source = new DOMSource(doc);
            transformer.transform(source, result);
            xml = writer.toString();
        } catch (TransformerConfigurationException e) {
            throw new LogException("Unable to obtain XML document transformer.", e);
        } catch (TransformerException e) {
            throw new LogException("Unable to transform XML document.", e);
        }

        return xml;
    }

    public boolean validate() throws LogException {
        boolean obtainedSchema = false;
        Schema schema = null;

        try {
            URL schemaURL = new URL(LOG_ENTRY_SCHEMA_URL); // TODO: may be comment!
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = factory.newSchema(schemaURL);
            obtainedSchema = true;
        } catch (MalformedURLException e) {
            //throw new LogException("Schema URL malformed.", e);
            e.printStackTrace();

        } catch (SAXException e) {
            //throw new LogException("Unable to parse schema.", e);
            e.printStackTrace();
        }

        if (obtainedSchema) {
            Validator validator = schema.newValidator();

            DOMSource source = new DOMSource(doc);

            try {
                validator.validate(source);
            } catch (SAXException e) {
                throw new LogException("Invalid XML.", e);
            } catch (IOException e) {
                throw new LogException("Unable to validate XML.", e);
            }

        }

        return obtainedSchema;
    }

    public Long submit() {
        return null;
    }

    public Long submit(String certificatePath) {
        return null;
    }

    /**
     * Attempt to obtain the process ID of the running JVM. Java has no official
     * way to do this and this method will return null if unable to obtain the
     * id.
     *
     * The approach used is to check the RuntimeMXBean name field, which often
     * is in the form {pid}
     *
     * @{hostname}, but isn't required to be.
     *
     * @return The pid of the running JVM, or null if unable to obtain it.
     */
    public Integer getJVMProcessId() {
        Integer id = null;

        String name = ManagementFactory.getRuntimeMXBean().getName();

        String[] tokens = name.split("@");

        if (tokens.length > 0) {
            try {
                id = Integer.parseInt(tokens[0]);
            } catch (NumberFormatException e) {
                // Oh well, we tried
            }
        }

        return id;
    }

    public String getHostname() {
        String hostname = null;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // Oh well, we tried;
        }

        return hostname;
    }

    protected String generateFilename() {
        StringBuilder filenameBuilder = new StringBuilder();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HHmmss_");

        String date = formatter.format(new Date());

        Integer pid = getJVMProcessId();

        if (pid == null) {
            pid = 0;
        }

        String hostname = getHostname();

        if (hostname == null) {
            hostname = "unknown";
        }

        Double random = Math.random() * 1000;

        filenameBuilder.append(date);
        filenameBuilder.append(pid);
        filenameBuilder.append("_");
        filenameBuilder.append(hostname);
        filenameBuilder.append("_");
        filenameBuilder.append(random);

        return filenameBuilder.toString();
    }

    protected void queue() throws LogException {
        validate(); // Ignore return value indicating could not obtain schema

        String xml = getXML();

        String filename = generateFilename();
        FileWriter writer = null;

        try {
            writer = new FileWriter(QUEUE_PATH + filename);

            writer.write(xml);
        } catch (IOException e) {
            throw new LogException("Unable to write XML file.", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Unable to close XML file.");
                }
            }
        }
    }
}
