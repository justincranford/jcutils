package com.github.justincranford.jcutils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Cache DocumentBuilderFactory (static) and DocumentBuilder (ThreadLocal)
 * for faster XML parser performance when calling DocumentBuilder.parse(InputStream).
 */
@SuppressWarnings("synthetic-access")
public class XmlUtil {
	private static final Logger LOG = Logger.getLogger(XmlUtil.class.getName());

	private XmlUtil() {
		// prevent instantiation of this class
	}

	/*package*/ static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();	// slow
	static {
		try {
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXP_DocumentBuilderFactory.2C_SAXParserFactory_and_DOM4J
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			// This is the PRIMARY defense. If DTDs (doctypes) are disallowed, almost all XML entity attacks are prevented
			// Xerces 2 only - http://xerces.apache.org/xerces2-j/features.html#disallow-doctype-decl
			DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

			// If you can't completely disable DTDs, then at least do the following:
			// Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-general-entities
			// Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-general-entities
			DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-general-entities", true);

			// Xerces 1 - http://xerces.apache.org/xerces-j/features.html#external-parameter-entities
			// Xerces 2 - http://xerces.apache.org/xerces2-j/features.html#external-parameter-entities
			// JDK7+ - http://xml.org/sax/features/external-parameter-entities    
			DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

			// Disable external DTDs as well
			DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

			// and these as well, per Timothy Morgan's 2014 paper: "XML Schema, DTD, and Entity Attacks"
			DOCUMENT_BUILDER_FACTORY.setXIncludeAware(false);
			DOCUMENT_BUILDER_FACTORY.setExpandEntityReferences(false);

			// And, per Timothy Morgan: "If for some reason support for inline DOCTYPEs are a requirement, then 
			// ensure the entity settings are disabled (as shown above) and beware that SSRF attacks
			// (http://cwe.mitre.org/data/definitions/918.html) and denial 
			// of service attacks (such as billion laughs or decompression bombs via "jar:") are a risk."

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// EDC Security Assurance - https://sottconfluence.corporate.datacard.com/display/secassur/Avoiding+XML-Based+Attacks
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			// From sample code. This enforces certain limits on resource usage.
			DOCUMENT_BUILDER_FACTORY.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);	// "http://javax.xml.XMLConstants/feature/secure-processing"

			// From sample code. No explanation given.
			DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);

			//////////////////////
			// Additional settings
			//////////////////////

			DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/namespaces", false);
			DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/validation", false);
		} catch (ParserConfigurationException pce) {
			throw new RuntimeException("Unexpected exception. Cannot initialize securely.", pce);
		}
	}

	/*package*/ static final ThreadLocal<DocumentBuilder> DOC_DOM_BUILDER = new ThreadLocal<DocumentBuilder>() {	// NOSONAR Remove this unused "DOC_DOM_BUILDER" private field.
		@Override
		public DocumentBuilder initialValue() {
			try {
				return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				LOG.log(Level.SEVERE, "Unexpected exception", e);
				return null;
			}
		}
		@Override
		public DocumentBuilder get() {
			final DocumentBuilder documentBuilder = super.get();
			documentBuilder.reset();
			return documentBuilder;
		}
	};

	public static final DocumentBuilder get() {
		return DOC_DOM_BUILDER.get();
	}

	public static final void remove() {
		DOC_DOM_BUILDER.remove();
	}
}