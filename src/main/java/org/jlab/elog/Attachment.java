package org.jlab.elog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.jlab.elog.exception.LogIOException;
import org.jlab.elog.exception.LogRuntimeException;
import org.jlab.elog.util.XMLUtil;
import org.w3c.dom.Element;

/**
 * A file attachment.
 * 
 * @author ryans
 */
public class Attachment {

    private final Element attachmentElement;

    Attachment(Element attachmentElement) {
        this.attachmentElement = attachmentElement;
    }

    /**
     * Return the attachment caption.
     * 
     * @return The caption 
     */
    public String getCaption() {
        Element capElement = XMLUtil.getChildElementByName(attachmentElement,
                "caption");

        if (capElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment caption element missing.");
        }

        return capElement.getTextContent();
    }

    /**
     * Return the attachment file name.
     * 
     * @return The file name
     */
    public String getFileName() {
        Element nameElement = XMLUtil.getChildElementByName(attachmentElement,
                "filename");

        if (nameElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment filename element missing.");
        }

        return nameElement.getTextContent();
    }

    /**
     * Return the attachment mime type.
     * 
     * @return The mime type
     */
    public String getMimeType() {
        Element mimeElement = XMLUtil.getChildElementByName(attachmentElement,
                "type");

        if (mimeElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment type element missing.");
        }

        return mimeElement.getTextContent();
    }

    /**
     * Return the attachment URL or null if unavailable.  The URL is unavailable
     * when the attachment has not been submitted to the server yet.
     * 
     * @return The URL or null
     */
    public String getURL() {
        String url = null;

        Element dataElement = XMLUtil.getChildElementByName(attachmentElement,
                "data");

        if (dataElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment data element missing.");
        }

        if (dataElement.getAttribute("encoding").equals("url")) {
            url = dataElement.getTextContent();
        }

        return url;
    }
    
    /**
     * Return true if the attachment is accessed via URL or false if local only 
     * (has not been submitted yet).
     * 
     * Note: you could just call getURL() and check if the result is null.
     * 
     * @return true if the attachment is accessed via URL
     */
    public boolean isURL() {
        return (getURL() != null);
    }

    /**
     * Return the attachment data via an InputStream.  The user should close
     * the InputStream when done.
     * 
     * @return The attachment data
     * @throws LogIOException If unable to obtain the InputStream
     */
    public InputStream getData() throws LogIOException {
        InputStream is = null;

        Element dataElement = XMLUtil.getChildElementByName(attachmentElement,
                "data");

        if (dataElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment data element missing.");
        }

        if (dataElement.getAttribute("encoding").equals("url")) {
            String urlStr = dataElement.getTextContent();

            try {
                java.net.URL url = new URL(urlStr);
                is = url.openStream();
            } catch (MalformedURLException e) {
                throw new LogIOException("Unable to open input stream.", e);
            } catch(IOException e) {
                throw new LogIOException("Unable to open input stream.", e);
            }
        } else {
            String dataStr = dataElement.getTextContent();
            byte[] data = XMLUtil.decodeBase64(dataStr);
            is = new ByteArrayInputStream(data);
        }

        return is;
    }
}
