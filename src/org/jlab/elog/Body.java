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
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("ContentType: ");
        builder.append(getType().name());
        builder.append(", Content: ");
        builder.append(getContent());
        return builder.toString();
    }
}
