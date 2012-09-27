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

    private static final String LOG_ENTRY_SCHEMA_URL;    
    
    final XPathExpression titleExpression;
    final XPathExpression logbooksExpression;
    final XPathExpression logbookListExpression;
    final XPathExpression entrymakersExpression;
    final XPathExpression usernameListExpression;
    
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

        XMLUtil.appendElementWithText(doc, root, "title", title);

        Element logbooks = doc.createElement("Logbooks");
        root.appendChild(logbooks);
        XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooks, "logbook", books);
    }

    public LogEntry(long id) throws LogException {
        super("Logentry");

        throw new UnsupportedOperationException();
    }

    public LogEntry(String filePath) throws SchemaUnavailableException, InvalidXMLException, LogException {
        try {
            doc = builder.parse(filePath);
        } catch (SAXException e) {
            throw new LogException("File is not well formed XML.", e);
        } catch (IOException e) {
            throw new LogException("Unable to parse XML file.", e);
        }
        
        validate(); // Alternatively we could call builder.setSchema() and it would be a validating parser
    }

    public void addLogbooks(String books) throws LogException {
        try {
            Element logbooksElement = (Element)logbooksExpression.evaluate(doc, XPathConstants.NODE);
            XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooksElement, "logbook", books);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }        
    }

    public void setLogbooks(String books) throws LogException {
        try {
            Element logbooksElement = (Element)logbooksExpression.evaluate(doc, XPathConstants.NODE);
            XMLUtil.removeChildren(logbooksElement);
            XMLUtil.appendCommaDelimitedElementsWithText(doc, logbooksElement, "logbook", books);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }           
    }
    
    public String getLogbooks() throws LogException {
        String logbooks = null;
        
        try {
            NodeList logbookElements = (NodeList)logbookListExpression.evaluate(doc, XPathConstants.NODESET);
            logbooks = XMLUtil.buildCommaDelimitedFromText(logbookElements);
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
            
            if(entrymakersElement == null) {
                entrymakersElement = doc.createElement("Entrymakers");
                root.appendChild(entrymakersElement);
            }   
            
            XMLUtil.appendCommaDelimitedElementsWithGrandchildAndText(doc, entrymakersElement, "Entrymaker", "username", entrymakers);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }          
    }
    
    public void setEntryMakers(String entrymakers) throws LogException {
        try {
            Element entrymakersElement = (Element)entrymakersExpression.evaluate(doc, XPathConstants.NODE);
            
            if(entrymakersElement == null) {
                entrymakersElement = doc.createElement("Entrymakers");
                root.appendChild(entrymakersElement);
            } else {
                XMLUtil.removeChildren(entrymakersElement);
            }
            
            XMLUtil.appendCommaDelimitedElementsWithGrandchildAndText(doc, entrymakersElement, "Entrymaker", "username", entrymakers);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }          
    }      
    
    public String getEntryMakers() throws LogException {
        String entrymakers = null;
        
        try {
            NodeList usernameElements = (NodeList)usernameListExpression.evaluate(doc, XPathConstants.NODESET);
            entrymakers = XMLUtil.buildCommaDelimitedFromText(usernameElements);
        }
        catch(XPathExpressionException e) {
            throw new LogException("Unable to traverse XML DOM.", e);
        }   
        
        return entrymakers;
    }    

    @Override
    String getSchemaURL() {
        return LOG_ENTRY_SCHEMA_URL;
    }
}
