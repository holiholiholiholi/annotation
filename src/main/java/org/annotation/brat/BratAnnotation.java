package org.annotation.brat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This class ...
 */
public interface BratAnnotation {
    String EVENT_ID_PREFIX = "E";
    String ENTITY_ID_PREFIX = "T";
    String BRELATION_ID_PREFIX = "R";
    String ATTRIBUTE_ID_PREFIX = "A";

    @JsonIgnore
    String toBratString();

    @JsonIgnore
    JsonNode getEmbedJson();
}
