package orchastack.core.jpa.container.jpa;

/**
 * This Exception will be thrown when there was an error parsing a PersistenceDescriptor
 * It will use the standard chaining mechanism to wrap the Exception thrown by the parser. 
 */
public class PersistenceDescriptorParserException extends Exception {

  /**
   * Construct a PersistenceDescriptorException
   * @param string 
   * @param e the exception to wrap
   */
  public PersistenceDescriptorParserException(String string, Exception e) {
    super(string, e);
  }

  /**
   * Construct a PersistenceDescriptorException
   * @param string 
   */
  public PersistenceDescriptorParserException(String string) {
    super(string);
  }
  
  /**
   * For Serialization
   */
  private static final long serialVersionUID = -8960763303021136544L;

}
