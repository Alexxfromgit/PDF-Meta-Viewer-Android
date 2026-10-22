package com.example.pdfmetaviewerandroid.extractor;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import static org.junit.Assert.*;

public class XmpMetadataTest {
    @Test public void resolvesNamespacesInsteadOfAssumingPrefixesAndKeepsMultipleAuthors() throws Exception {
        String packet = "<r:RDF xmlns:r='http://www.w3.org/1999/02/22-rdf-syntax-ns#' "
                + "xmlns:custom='http://purl.org/dc/elements/1.1/'>"
                + "<r:Description><custom:creator><r:Seq><r:li>Alice</r:li><r:li>Bob</r:li>"
                + "</r:Seq></custom:creator></r:Description></r:RDF>";
        Map<String, String> values = new TreeMap<>();
        XmpMetadata.read(packet.getBytes(StandardCharsets.UTF_8), values);
        assertEquals("Alice, Bob", values.get("xmp:dc:creator"));
        assertEquals("Alice, Bob", values.get("dc:creator"));
    }
    @Test public void minutesOnlyTimestampsPreserveTheirAuthoredInstant() throws Exception {
        String packet = "<r:RDF xmlns:r='http://www.w3.org/1999/02/22-rdf-syntax-ns#' "
                + "xmlns:x='http://ns.adobe.com/xap/1.0/'><r:Description>"
                + "<x:ModifyDate>2017-03-16T15:02+02:00</x:ModifyDate>"
                + "</r:Description></r:RDF>";
        Map<String, String> values = new TreeMap<>();
        XmpMetadata.read(packet.getBytes(StandardCharsets.UTF_8), values);
        assertEquals("2017-03-16T13:02:00Z", values.get("xmp:ModifyDate"));
    }
    @Test public void externalEntitiesAreNeverRead() {
        String packet = "<!DOCTYPE x [<!ENTITY leak SYSTEM 'file:///etc/passwd'>]><x>&leak;</x>";
        assertThrows(Exception.class, () -> XmpMetadata.read(packet.getBytes(StandardCharsets.UTF_8), new TreeMap<>()));
    }
}
