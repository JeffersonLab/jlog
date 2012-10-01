package org.jlab.elog;

import org.jlab.elog.exception.LogException;
import org.jlab.elog.exception.LogRuntimeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.w3c.dom.Element;

/**
 *
 * @author ryans
 */
public class Attachment {

    private final Element attachmentElement;

    Attachment(Element attachmentElement) {
        this.attachmentElement = attachmentElement;
    }

    public String getCaption() {
        Element capElement = XMLUtil.getChildElementByName(attachmentElement,
                "caption");

        if (capElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment caption element missing.");
        }

        return capElement.getTextContent();
    }

    public String getFileName() {
        Element nameElement = XMLUtil.getChildElementByName(attachmentElement,
                "filename");

        if (nameElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment filename element missing.");
        }

        return nameElement.getTextContent();
    }

    public String getMimeType() {
        Element mimeElement = XMLUtil.getChildElementByName(attachmentElement,
                "type");

        if (mimeElement == null) {
            throw new LogRuntimeException("Unexpected XML DOM structure; "
                    + "Attachment type element missing.");
        }

        return mimeElement.getTextContent();
    }

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

    public InputStream getData() throws LogException {
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
                URL url = new URL(urlStr);
                is = url.openStream();
            } catch (MalformedURLException e) {
                throw new LogException("Unable to open input stream.", e);
            } catch(IOException e) {
                throw new LogException("Unable to open input stream.", e);
            }
        } else {
            String dataStr = dataElement.getTextContent();
            byte[] data = XMLUtil.decodeBase64(dataStr);
            is = new ByteArrayInputStream(data);
        }

        return is;
    }
}
