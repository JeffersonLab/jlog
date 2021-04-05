package org.jlab.jlog.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML Utilities.
 *
 * @author ryans
 */
public final class XMLUtil {

    private static final DatatypeFactory typeFactory;

    static {
        try {
            typeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(
                    "Unable to initialize a DatatypeFactory.", e);
        }
    }

    private XMLUtil() {
        // Can't instantiate publicly
    }

    /**
     * Generates a formated DateTime String appropriate for insertion into an
     * XML document.
     *
     * @param calendar The calendar to format
     * @return The formatted String
     */
    public static String toXMLFormat(GregorianCalendar calendar) {
        return typeFactory.newXMLGregorianCalendar(calendar).normalize().
                toXMLFormat();
    }

    /**
     * Generates a calendar from an XML DateTime String.
     *
     * @param xmlDateTime The DateTime String
     * @return The calendar
     */
    public static GregorianCalendar toGregorianCalendar(String xmlDateTime) {
        return typeFactory.newXMLGregorianCalendar(xmlDateTime).normalize().
                toGregorianCalendar();
    }

    /**
     * Create a new element with child text node and append it to the specified
     * parent.
     *
     * @param doc The Document.
     * @param parent The parent
     * @param tagName The new element tag name
     * @param text The child text node value
     * @return The newly created Element
     */
    public static Element appendElementWithText(Document doc, Element parent,
            String tagName, String text) {
        Element child = doc.createElement(tagName);
        parent.appendChild(child);
        child.setTextContent(text);
        //Text textNode = doc.createTextNode(text);
        //child.appendChild(textNode);
        return child;
    }

    /**
     * Creates a new element with child text for a comma separated list of
     * values and appends them to the specified parent.
     *
     * @param doc The Document.
     * @param parent The parent.
     * @param tagName The tag name to use for the new Elements
     * @param list The comma separated values
     */
    public static void appendCommaDelimitedElementsWithText(Document doc,
            Element parent, String tagName, String list) {
        String[] tokens = IOUtil.csvToArray(list);

        for (String token : tokens) {
            appendElementWithText(doc, parent, tagName, token);
        }
    }

    /**
     * Creates a new element with child and grandchild with text from a comma
     * separated list of values and appends them to the specified parent.
     *
     * @param doc The Document.
     * @param parent The parent.
     * @param childTagName The child tag name.
     * @param grandchildTagName The grandchild tag name.
     * @param list The comma separated values.
     */
    public static void appendCommaDelimitedElementsWithGrandchildAndText(
            Document doc, Element parent, String childTagName,
            String grandchildTagName, String list) {
        String[] tokens = IOUtil.csvToArray(list);

        for (String token : tokens) {
            Element child = doc.createElement(childTagName);
            parent.appendChild(child);
            appendElementWithText(doc, child, grandchildTagName, token);
        }
    }

    /**
     * Builds a comma separated list of text values from a NodeList of Elements
     * with Text children.
     *
     * @param nodes The Elements with Text children
     * @return The comma separated values
     */
    public static String buildCommaDelimitedFromText(NodeList nodes) {
        StringBuilder csvBuilder = new StringBuilder();

        if (nodes.getLength() > 0) {
            for (int i = 0; i < nodes.getLength(); i++) {
                csvBuilder.append(nodes.item(i).getTextContent());
                csvBuilder.append(",");
            }

            // Remove trailing comma
            csvBuilder.deleteCharAt(csvBuilder.length() - 1);
        }

        return csvBuilder.toString();
    }

    /**
     * Builds an array of String text values from a NodeList of Elements with
     * Text children.
     *
     * @param nodes The Elements with Text children
     * @return The array of values
     */
    public static String[] buildArrayFromText(NodeList nodes) {
        ArrayList<String> values = new ArrayList<String>();

        if (nodes.getLength() > 0) {
            for (int i = 0; i < nodes.getLength(); i++) {
                values.add(nodes.item(i).getTextContent());
            }
        }

        return values.toArray(new String[]{});
    }

    /**
     * Removes all children from a parent Element.
     *
     * @param parent The parent
     */
    public static void removeChildren(Element parent) {
        while (parent.hasChildNodes()) {
            parent.removeChild(parent.getFirstChild());
        }
    }

    /**
     * Returns the first occurrence of a child element with the specified tag
     * name. Note: only immediate children are candidates (not all descendents).
     *
     * @param parent The parent node
     * @param tagName The child tag name
     * @return The child Element or null if none found
     */
    public static Element getChildElementByName(Node parent, String tagName) {
        Element child = null;

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element candidate = (Element) children.item(i);
                if (candidate.getTagName().equals(tagName)) {
                    child = candidate;
                    break;
                }
            }
        }

        return child;
    }

    /**
     * Convert a Document (DOM) into an XML String.
     *
     * @param doc The Document
     * @return The XML String
     * @throws TransformerConfigurationException If there is a configuration
     * issue
     * @throws TransformerException If unable to transform the Document
     */
    public static String getXML(Document doc) throws
            TransformerConfigurationException, TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();

        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        return writer.toString();
    }
}
