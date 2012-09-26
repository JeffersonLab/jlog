package org.jlab.elog;

/**
 *
 * @author ryans
 */
public class Body {

    public enum ContentType {

        TEXT, HTML
    };
    private final ContentType type;
    private final String content;
    
    public Body(ContentType type, String content) {
        this.type = type;
        this.content = content;
    }

    public ContentType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
