package org.annotation.brat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class ...
 */
@Data
public class BratEvent implements BratAnnotation {

    @NonNull
    private  String type;

    private final BratEntity trigger;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, BratEntity> argument = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Set<String> attribute = new HashSet<>();


    private String id;

    private String docId;

    private String annotator;

    @JsonIgnore
    public int getStartPosition() {
        return Stream.concat(argument.values().stream(), Stream.of(trigger))
                     .mapToInt(BratEntity::getStartPosition)
                     .min()
                     .orElse(-1);
    }

    @JsonIgnore
    public int getEndPosition() {
        return Stream.concat(argument.values().stream(), Stream.of(trigger))
                     .mapToInt(BratEntity::getEndPosition)
                     .max()
                     .orElse(-1);
    }

    @JsonIgnore
    public int getIdNumber() {
        return Integer.parseInt(getId().substring(EVENT_ID_PREFIX.length()));
    }

    public void setIdNumber(int i) {
        setId(EVENT_ID_PREFIX + i);
    }

    @Override
    public String toBratString() {

        return getId() + "\t" + type + ":" + trigger.getId() + " "
                + getArgument().entrySet()
                               .stream().map(entry -> entry.getKey() + ":" + entry.getValue().getId())
                               .collect(Collectors.joining(" "));
    }

    @Override
    public JsonNode getEmbedJson(){
        //TODO:
        return null;
    }

}
