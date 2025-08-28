package org.jlab.jlog;

/**
 * The body of a log book item, such as log entry or comment.
 *
 * @author ryans
 */
public class Body {

  /** The content type enumeration. */
  public enum ContentType {

    /** Plain Text Body */
    TEXT,
    /** HTML Body */
    HTML
  }

  private final ContentType type;
  private final String content;

  /**
   * Construct a new Body with the specified type and content.
   *
   * @param type The type
   * @param content The content
   */
  public Body(ContentType type, String content) {
    this.type = type;
    this.content = content;
  }

  /**
   * Return the content type.
   *
   * @return The type
   */
  public ContentType getType() {
    return type;
  }

  /**
   * Return the content.
   *
   * @return The content
   */
  public String getContent() {
    return content;
  }

  /**
   * Returns a String representation.
   *
   * @return The String representation
   */
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
