package com.github.justincranford.jcutils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XmlUtil {
	private static final Logger LOG = Logger.getLogger(XmlUtil.class.getName());

	private XmlUtil() {
		// prevent instantiation of this class
	}

	// Cache DocumentBuilderFactory (static) and DocumentBuilder (ThreadLocal) for faster XML parsing performance when calling DocumentBuilder.parse(InputStream).
	/*package*/ static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();	// slow
	static {
		DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
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