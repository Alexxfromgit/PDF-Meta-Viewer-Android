package com.example.pdfmetaviewerandroid.extractor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Read-only, namespace-aware XMP projection. No external entities or document types. */
final class XmpMetadata {
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private XmpMetadata() {}

    static void read(byte[] packet, Map<String, String> values) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Android doesn't implement every JAXP feature. Each is attempted independently;
        // the rejecting EntityResolver and explicit DOCTYPE check remain in force.
        feature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        feature(factory, "http://xml.org/sax/features/external-general-entities", false);
        feature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        feature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> { throw new SAXException("External entities are not allowed."); });
        Document document = builder.parse(new ByteArrayInputStream(packet));
        if (document.getDoctype() != null) throw new IOException("XMP document types are not allowed.");
        NodeList descriptions = document.getElementsByTagNameNS(RDF, "Description");
        for (int i = 0; i < descriptions.getLength(); i++) {
            Element description = (Element) descriptions.item(i);
            NamedNodeMap attributes = description.getAttributes();
            for (int a = 0; a < attributes.getLength(); a++) {
                Node attribute = attributes.item(a);
                String key = key(attribute);
                if (key != null) add(values, key, attribute.getNodeValue());
            }
            for (Node child = description.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (!(child instanceof Element)) continue;
                String key = key(child);
                if (key == null) continue;
                List<String> text = new ArrayList<>();
                leaves(child, text);
                add(values, key, String.join(", ", text));
                if ("xmp:dc:title".equals(key) || "xmp:dc:description".equals(key)) {
                    NodeList alternatives = ((Element) child).getElementsByTagNameNS(RDF, "li");
                    for (int a = 0; a < alternatives.getLength(); a++) {
                        Element item = (Element) alternatives.item(a);
                        String language = item.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang");
                        if (!language.isEmpty()) add(values, key + ":" + language, item.getTextContent().trim());
                    }
                }
            }
        }
        fallback(values, "dc:title", "xmp:dc:title");
        fallback(values, "dc:creator", "xmp:dc:creator");
        fallback(values, "dc:description", "xmp:dc:description");
        fallback(values, "dc:title:x-default", "xmp:dc:title:x-default");
    }

    private static void feature(DocumentBuilderFactory factory, String feature, boolean value) {
        try { factory.setFeature(feature, value); } catch (Exception ignored) { /* optional on Android */ }
    }
    private static String key(Node node) {
        String namespace = node.getNamespaceURI();
        String local = node.getLocalName();
        if (namespace == null || local == null || RDF.equals(namespace)
                || namespace.equals("http://www.w3.org/2000/xmlns/")) return null;
        switch (namespace) {
            case "http://ns.adobe.com/xap/1.0/": return "xmp:" + local;
            case "http://ns.adobe.com/xap/1.0/mm/": return "xmpMM:" + local;
            case "http://ns.adobe.com/xap/1.0/t/pg/": return "xmpTPg:" + local;
            case "http://purl.org/dc/elements/1.1/": return "xmp:dc:" + local;
            case "http://ns.adobe.com/pdf/1.3/": return "xmp:pdf:" + local;
            default:
                String prefix = node.getPrefix();
                return prefix == null ? null : "xmp:" + prefix + ":" + local;
        }
    }
    private static void leaves(Node node, List<String> values) {
        boolean hasElements = false;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) { hasElements = true; leaves(child, values); }
        }
        if (!hasElements) {
            String value = node.getTextContent().trim();
            if (!value.isEmpty()) values.add(value);
        }
    }
    private static void add(Map<String, String> values, String key, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (key.equals("xmp:CreateDate") || key.equals("xmp:ModifyDate") || key.equals("xmp:MetadataDate")) {
            try {
                value = java.time.format.DateTimeFormatter.ISO_INSTANT.format(
                        java.time.OffsetDateTime.parse(value).toInstant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
            } catch (java.time.format.DateTimeParseException ignored) { /* Keep unrecognized authored values. */ }
        }
        String previous = values.get(key);
        values.put(key, previous == null || previous.equals(value) ? value : previous + ", " + value);
    }
    private static void fallback(Map<String, String> values, String key, String source) {
        if (!values.containsKey(key) && values.containsKey(source)) values.put(key, values.get(source));
    }
}
