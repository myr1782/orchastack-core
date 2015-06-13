package orchastack.core.jpa.container.jpa;

import javax.xml.validation.Schema;

import org.xml.sax.SAXException;

/**
 * A convenience mechanism for finding the version of the schema to validate with
 */
public class EarlyParserReturn extends SAXException
{
  /** This class is serializable */
  private static final long serialVersionUID = 6173561765417524327L;
  /** The schema to use */
  private final Schema schema;
  /** The value of the version attribute in the xml */
  private final String jpaVersion;

  /**
   * @return The schema that was used in the xml document
   */
  public Schema getSchema()
  {
    return schema;
  }
  
  /**
   * @return The version of the JPA schema used
   */
  public String getVersion()
  {
    return jpaVersion;
  }

  /**
   * @param s  The schema used
   * @param version The version of the schema used
   */
  public EarlyParserReturn(Schema s, String version)
  {
    schema = s;
    jpaVersion = version;
  }
}
