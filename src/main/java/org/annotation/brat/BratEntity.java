package org.annotation.brat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.NonNull;

@Data
public class BratEntity implements BratAnnotation {

    @NonNull
    private  String type;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String text;

    private String id;

    private int startPosition;


    private int endPosition;

    @Override
    public String toBratString() {
        return id + "\t" + type + " " + startPosition + " " + endPosition + "\t" + text;
    }

    @JsonIgnore
    public int getIdNumber() {
        return Integer.parseInt(getId().substring(ENTITY_ID_PREFIX.length()));
    }

    public void setIdNumber(int i) {
        setId(ENTITY_ID_PREFIX + i);
    }

    @Override
    public JsonNode getEmbedJson(){
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(getId());
        arrayNode.add(getType());
        arrayNode.add(mapper.createArrayNode().add(mapper.createArrayNode().add(startPosition).add(endPosition)));
        return arrayNode;
    }
}