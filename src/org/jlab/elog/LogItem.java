package org.jlab.elog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;
import javax.net.ssl.HttpsURLConnection;
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
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author ryans
 */
abstract class LogItem {

    private static final String SUBMIT_URL;
    private static final String QUEUE_PATH;
    private static final String PEM_FILE_NAME = ".elogcert";

    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        SUBMIT_URL = bundle.getString("SUBMIT_URL");
        QUEUE_PATH = bundle.getString("QUEUE_PATH");
    }
    
    Document doc;
    Element root;
    DatatypeFactory typeFactory;
    DocumentBuilder builder;
    XPath xpath;
    XPathExpression lognumberExpression;
    XPathExpression lognumberTextExpression;
    XPathExpression createdExpression;
    XPathExpression bodyExpression;
    XPathExpression attachmentsExpression;
    XPathExpression authorTextExpression;

    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new LogRuntimeException("Unable to obtain XML document builder.", e);
        }

        try {
            typeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new LogRuntimeException("Unable to obtain XML datatype factory.", e);
        }

        XPathFactory xpathFactory = XPathFactory.newInstance();
        xpath = xpathFactory.newXPath();

        try {
            lognumberExpression = xpath.compile("/*/lognumber");
            lognumberTextExpression = xpath.compile("/*/lognumber/text()");
            createdExpression = xpath.compile("/*/created");
            bodyExpression = xpath.compile("/*/body");
            attachmentsExpression = xpath.compile("/*/Attachments");
            authorTextExpression = xpath.compile("/*/Author/username/text()");
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to construct XML XPath query", e);
        }
    }

    public LogItem() throws LogRuntimeException {
    }

    public LogItem(String rootTagName) throws LogRuntimeException {

        doc = builder.newDocument();

        root = doc.createElement(rootTagName);
        doc.appendChild(root);

        XMLUtil.appendElementWithText(doc, root, "created", XMLUtil.toXMLFormat(new GregorianCalendar()));
        
        Element authorElement = doc.createElement("Author");
        root.appendChild(authorElement);
        XMLUtil.appendElementWithText(doc, authorElement, "username", System.getProperty("user.name"));        
    }

    public void addAttachment(String filepath) throws LogException, LogRuntimeException {
        addAttachment(filepath, "", "");
    }

    public void addAttachment(String filepath, String caption, String mimeType) throws LogException, LogRuntimeException {

        try {
            File file = new File(filepath);
            String data = XMLUtil.encodeBase64(IOUtil.fileToBytes(file));
            Element attachmentsElement = (Element) attachmentsExpression.evaluate(doc, XPathConstants.NODE);

            Element attachmentElement = doc.createElement("Attachment");
            attachmentsElement.appendChild(attachmentElement);
            XMLUtil.appendElementWithText(doc, attachmentElement, "caption", caption);
            XMLUtil.appendElementWithText(doc, attachmentElement, "filename", file.getName());
            XMLUtil.appendElementWithText(doc, attachmentElement, "type", mimeType);
            Element dataElement = XMLUtil.appendElementWithText(doc, attachmentElement, "data", data);
            dataElement.setAttribute("encoding", "base64");
        } catch (IOException e) {
            throw new LogException("Unable to access attachment file.", e);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM via XPath.", e);
        }
    }

    public Attachment[] getAttachments() throws LogRuntimeException {
        List<Attachment> attachments = new ArrayList<Attachment>();

        try {
            Element attachmentsElement = (Element) attachmentsExpression.evaluate(doc, XPathConstants.NODE);
            NodeList children = attachmentsElement.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                attachments.add(new Attachment((Element) children.item(i)));
            }

        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM via XPath.", e);
        } catch (ClassCastException e) {
            throw new LogRuntimeException("Unexpected Node type in XML DOM.", e);
        }

        return attachments.toArray(new Attachment[]{});
    }

    public String getAuthor() throws LogRuntimeException {
        String author = null;

        try {
            author = (String) authorTextExpression.evaluate(doc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        }

        return author;        
    }
    
    public void setLogNumber(Long lognumber) throws LogRuntimeException {
        try {
            Element lognumberElement = (Element) lognumberExpression.evaluate(doc, XPathConstants.NODE);

            if (lognumberElement == null) {
                lognumberElement = doc.createElement("lognumber");
                root.appendChild(lognumberElement);
            }

            lognumberElement.setTextContent(lognumber.toString());
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        }
    }

    public Long getLogNumber() throws LogRuntimeException {
        Long lognumber = null;

        try {
            String lognumberStr = (String) lognumberTextExpression.evaluate(doc, XPathConstants.STRING);

            if (lognumberStr != null && !lognumberStr.isEmpty()) { // TODO: C++ impl throws exception if null
                try {
                    lognumber = Long.parseLong(lognumberStr);
                } catch (NumberFormatException e) {
                    throw new LogRuntimeException("Unable to obtain log number due to non-numeric format.", e);
                }
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        }

        return lognumber;
    }

    public void setCreated(GregorianCalendar created) throws LogRuntimeException {
        try {
            Element createdElement = (Element) createdExpression.evaluate(doc, XPathConstants.NODE);
            createdElement.setTextContent(XMLUtil.toXMLFormat(created));
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        }
    }

    public GregorianCalendar getCreated() throws LogRuntimeException {
        GregorianCalendar created = null;

        try {
            Element createdElement = (Element) createdExpression.evaluate(doc, XPathConstants.NODE);
            String createdStr = createdElement.getTextContent();
            created = XMLUtil.toGregorianCalendar(createdStr);
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        }

        return created;
    }

    public Body getBody() throws LogRuntimeException {
        Body body = null;

        try {
            Element bodyElement = (Element) bodyExpression.evaluate(doc, XPathConstants.NODE);

            if (bodyElement != null) {
                String content = bodyElement.getTextContent();
                String typeStr = bodyElement.getAttribute("type");
                Body.ContentType type = Body.ContentType.TEXT;

                if (typeStr != null) {
                    type = Body.ContentType.valueOf(typeStr.toUpperCase());
                }

                body = new Body(type, content);
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        } catch (IllegalArgumentException e) {
            throw new LogRuntimeException("Unexpected ContentType in XML body", e);
        }

        return body;
    }

    public void setBody(Body body) throws LogRuntimeException {

        try {
            Element bodyElement = (Element) bodyExpression.evaluate(doc, XPathConstants.NODE);

            if (bodyElement != null) {
                root.removeChild(bodyElement);
            }

            if (body.getContent() != null && !body.getContent().isEmpty()) {
                bodyElement = doc.createElement("body");
                root.appendChild(bodyElement);

                if (body.getType() == Body.ContentType.HTML) {
                    bodyElement.setAttribute("type", "html");
                }

                CDATASection data = doc.createCDATASection(body.getContent());
                bodyElement.appendChild(data);
            }
        } catch (XPathExpressionException e) {
            throw new LogRuntimeException("Unable to traverse XML DOM.", e);
        }

    }

    public String getXML() throws LogRuntimeException {
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
            throw new LogRuntimeException("Unable to obtain XML document transformer.", e);
        } catch (TransformerException e) {
            throw new LogRuntimeException("Unable to transform XML document.", e);
        }

        return xml;
    }

    abstract String getSchemaURL();

    public boolean validate() throws LogException {
        boolean obtainedSchema = false;
        Schema schema = null;

        try {
            URL schemaURL = new URL(getSchemaURL());
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
                //throw new LogException("Unable to validate XML.", e);
                e.printStackTrace();
                obtainedSchema = false;
            }

        }

        return obtainedSchema;
    }

    public Long submit() throws LogException {
        String pemFilePath = new File(System.getProperty("user.home"), PEM_FILE_NAME).getAbsolutePath();
        return submit(pemFilePath);
    }    
    
    public Long submit(String pemFilePath) throws LogException {
        Long id = null;

        String xml = getXML();

        HttpsURLConnection con;
        OutputStreamWriter writer = null;
        InputStreamReader reader = null;

        try {
            URL url = new URL(SUBMIT_URL);
            con = (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(SecurityUtil.getClientCertSocketFactoryPEM(pemFilePath, true));
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.connect();
            writer = new OutputStreamWriter(con.getOutputStream());
            writer.write(xml);
            reader = new InputStreamReader(con.getInputStream());
            // TODO: read response       
        } catch (MalformedURLException e) {
            throw new LogException("Invalid submission URL: check config file.", e);
        } catch (IOException e) {
            throw new LogException("Unable to write to ELOG server.", e);
        } catch (ClassCastException e) {
            throw new LogException("Expected HTTP URL; check config file.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new LogException("Invalid SSL certificate algorithm.", e);
        } catch (CertificateException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (KeyStoreException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (KeyManagementException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (UnrecoverableKeyException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } catch (InvalidKeySpecException e) {
            throw new LogException("Unable to obtain SSL connection due to certificate error.", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return id;
    }

    String generateXMLFilename() {
        StringBuilder filenameBuilder = new StringBuilder();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HHmmss_");

        String date = formatter.format(new Date());

        Integer pid = SystemUtil.getJVMProcessId();

        if (pid == null) {
            pid = 0;
        }

        String hostname = SystemUtil.getHostname();

        if (hostname == null) {
            hostname = "unknown";
        }

        int random = (int) (Math.random() * 10000);

        filenameBuilder.append(date);
        filenameBuilder.append(pid);
        filenameBuilder.append("_");
        filenameBuilder.append(hostname);
        filenameBuilder.append("_");
        filenameBuilder.append(random);
        filenameBuilder.append(".xml");

        return filenameBuilder.toString();
    }

    void queue() throws LogException {
        String filename = generateXMLFilename();
        String filepath = new File(QUEUE_PATH, filename).getAbsolutePath();
        queue(filepath);
    }
    
    void queue(String filepath) throws LogException {
        validate(); // Ignore return value indicating could not obtain schema

        String xml = getXML();

        FileWriter writer = null;

        try {
            writer = new FileWriter(filepath);

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
