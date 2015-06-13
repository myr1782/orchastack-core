package orchastack.core.jpa.container.jpa;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import orchastack.core.jpa.container.ParsedPersistenceUnit;
import orchastack.core.jpa.container.PersistenceDescriptor;

import org.osgi.framework.Bundle;

/**
 * This class may be used to parse JPA persistence descriptors. The parser
 * validates using the relevant version of the persistence schema as defined by
 * the xml file.
 */
public class PersistenceDescriptorParser {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.aries.jpa.container.parsing.impl.PersistenceDescriptorParser
	 * #parse(org.osgi.framework.Bundle,
	 * org.apache.aries.jpa.container.parsing.PersistenceDescriptor)
	 */
	public Collection<? extends ParsedPersistenceUnit> parse(Bundle b,	PersistenceDescriptor descriptor )
			throws PersistenceDescriptorParserException {
		Collection<ParsedPersistenceUnit> persistenceUnits = new ArrayList<ParsedPersistenceUnit>();
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		parserFactory.setNamespaceAware(true);
		RememberingInputStream is = null;
		boolean schemaFound = false;
		try {
			// Buffer the InputStream so we can mark it, though we'll be in
			// trouble if we have to read more than 8192 characters before
			// finding
			// the schema!
			
			is = new RememberingInputStream(descriptor.getInputStream());
			is.mark(Integer.MAX_VALUE);
			SAXParser parser = parserFactory.newSAXParser();
			try {
				parser.parse(is, new SchemaLocatingHandler());
			} catch (EarlyParserReturn epr) {
				// This is not really an exception, but a way to work out which
				// version of the persistence schema to use in validation
				Schema s = epr.getSchema();

				if (s != null) {
					schemaFound = true;
					parserFactory.setSchema(s);
					parserFactory.setNamespaceAware(true);
					parser = parserFactory.newSAXParser();

					// Get back to the beginning of the stream
					is.reset();

					JPAHandler handler = new JPAHandler(b, epr.getVersion());
					parser.parse(is, handler);
					persistenceUnits.addAll(handler.getPersistenceUnits());
				}
			}
		} catch (Exception e) {
			throw new PersistenceDescriptorParserException(
					"persistence.description.parse.error, bundle - "
							+ b.getSymbolicName(), e);
		} finally {
			if (is != null)
				try {
					is.closeUnderlying();
				} catch (IOException e) {
					// No logging necessary, just consume
				}
		}
		if (!!!schemaFound) {
			throw new PersistenceDescriptorParserException(
					"persistence.descriptor.schema.not.found, bundle - "
							+ b.getSymbolicName());
		}
		return persistenceUnits;
	}
}
