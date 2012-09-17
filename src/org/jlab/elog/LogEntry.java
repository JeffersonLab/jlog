package org.jlab.elog;

import java.io.IOException;
import java.util.ResourceBundle;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author ryans
 */
public class LogEntry extends LogItem {

    public static final String LOG_ENTRY_SCHEMA_URL;    
    
    protected final XPathExpression titleExpression;
    protected final XPathExpression logbooksExpression;
    protected final XPathExpression logbookListExpression;
    protected final XPathExpression entrymakersExpression;
    protected final XPathExpression usernameListExpression;
    
    static {
        ResourceBundle bundle = ResourceBundle.getBundle("org.jlab.elog.elog");
        LOG_ENTRY_SCHEMA_URL = bundle.getString("LOG_ENTRY_SCHEMA_URL");        
    }
    
    {
        try {
            titleExpression = xpath.compile("/Logentry/title");  
            logbooksExpression = xpath.compile("/Logentry/Logbooks");
            logbookListExpression = xpath.compile("/Logentry/Logbooks/logbook");
            entrymakersExpression = xpath.compile("/Logentry/Entrymakers");
            usernameListExpression = xpath.compile("/Logentry/Entrymakers/Entrymaker/username");
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to construct XML XPath query", e);
        }
            
    }
    
    public LogEntry(String title, String books) throws LogException {
        super("Logentry");

        appendElementWithText(root, "title", title);

        Element logbooks = doc.createElement("Logbooks");
        root.appendChild(logbooks);
        appendCommaDelimitedElementsWithText(logbooks, "logbook", books);

        Element entrymakers = doc.createElement("Entrymakers");
        root.appendChild(entrymakers);
        Element entrymaker = doc.createElement("Entrymaker");
        entrymakers.appendChild(entrymaker);
        appendElementWithText(entrymaker, "username", System.getProperty("user.name"));
    }

    public LogEntry(long id) throws LogException {
        super("Logentry");

        throw new UnsupportedOperationException();
    }

    public LogEntry(String filePath) throws LogException {
        try {
            doc = builder.parse(filePath);
        } catch (SAXException e) {
            throw new LogException("File is not well formed XML.", e);
        } catch (IOException e) {
            throw new LogException("Unable to parse XML file.", e);
        }
        
        if(!validate()) { // Alternatively we could call builder.setSchema() and it would be a validating parser
            throw new LogException("Unable to validate XML because schema cannot be obtained.");
        }
    }

    public void addLogbooks(String books) throws LogException {
        try {
            Element logbooksElement = (Element)logbooksExpression.evaluate(doc, XPathConstants.NODE);
            appendCommaDelimitedElementsWithText(logbooksElement, "logbook", books);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }        
    }

    public void setLogbooks(String books) throws LogException {
        try {
            Element logbooksElement = (Element)logbooksExpression.evaluate(doc, XPathConstants.NODE);
            removeChildren(logbooksElement);
            appendCommaDelimitedElementsWithText(logbooksElement, "logbook", books);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }           
    }
    
    public String getLogbooks() throws LogException {
        String logbooks = null;
        
        try {
            NodeList logbookElements = (NodeList)logbookListExpression.evaluate(doc, XPathConstants.NODESET);
            logbooks = buildCommaDelimitedFromText(logbookElements);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }   
        
        return logbooks;
    }
    
    public void setTitle(String title) throws LogException {
        try {
            Element titleElement = (Element)titleExpression.evaluate(doc, XPathConstants.NODE);
            titleElement.setTextContent(title);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }
    }
    
    public String getTitle() throws LogException {
        String title = null;
        
        try {
            Element titleElement = (Element)titleExpression.evaluate(doc, XPathConstants.NODE);
            title = titleElement.getTextContent();
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }        
        
        return title; 
    }
    
    public void addEntryMakers(String entrymakers) throws LogException {
        try {
            Element entrymakersElement = (Element)entrymakersExpression.evaluate(doc, XPathConstants.NODE);
            appendCommaDelimitedElementsWithGrandchildAndText(entrymakersElement, "Entrymaker", "username", entrymakers);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }          
    }
    
    public void setEntryMakers(String entrymakers) throws LogException {
        try {
            Element entrymakersElement = (Element)entrymakersExpression.evaluate(doc, XPathConstants.NODE);
            removeChildren(entrymakersElement);
            appendCommaDelimitedElementsWithGrandchildAndText(entrymakersElement, "Entrymaker", "username", entrymakers);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }          
    }      
    
    public String getEntryMakers() throws LogException {
        String entrymakers = null;
        
        try {
            NodeList usernameElements = (NodeList)usernameListExpression.evaluate(doc, XPathConstants.NODESET);
            entrymakers = buildCommaDelimitedFromText(usernameElements);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }   
        
        return entrymakers;
    }    

    @Override
    protected String getSchemaURL() {
        return LOG_ENTRY_SCHEMA_URL;
    }
}
