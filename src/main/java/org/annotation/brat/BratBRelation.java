package org.annotation.brat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class ...
 */
@Data
public class BratBRelation implements BratAnnotation {
    public static final String DEFAULT_ARG = "Arg";

    private final String type;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<BratEntity> arguments = new ArrayList<>();

    private String id;

    @JsonIgnore
    public static String getArgName(int i) {
        return DEFAULT_ARG + (i + 1);
    }

    @JsonIgnore
    public int getIdNumber() {
        return Integer.parseInt(getId().substring(BRELATION_ID_PREFIX.length()));
    }

    public void setIdNumber(int i) {
        setId(BRELATION_ID_PREFIX + i);
    }

    @Override
    public String toBratString() {
        return id.charAt(0) == '*' ?
                "*" + "\t" + type + " " + arguments.stream()
                                                   .map(BratEntity::getId)
                                                   .collect(Collectors.joining(" "))
                :
                getId() + "\t" + type + " " + IntStream.rangeClosed(1, arguments.size())
                                                       .mapToObj(i -> DEFAULT_ARG + i + ":" + arguments.get(i - 1)
                                                                                                       .getId())
                                                       .collect(Collectors.joining(" "));
    }

    public JsonNode getEmbedJson() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(getId());
        arrayNode.add(getType());
        ArrayNode args = mapper.createArrayNode();
        arrayNode.add(args);
        for (int i = 0; i < arguments.size(); i++) {
            args.add(mapper.createArrayNode().add(getArgName(i)).add(arguments.get(i).getId()));
        }
        return arrayNode;
    }


}
