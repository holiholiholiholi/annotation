package org.annotation.brat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class ...
 */
@Data
public class BratDocument implements BratAnnotation {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<BratEntity> entities = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<BratBRelation> relations = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<BratEvent> events = new ArrayList<>();

    //TODO: get all attributes!

    @NonNull
    private final String id;

    @NonNull
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String text;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<BratSentence> sentences;

    @JsonIgnore
    public List<BratEntity> getAllEntities() {
        return sentences == null ? new ArrayList<>(entities) :
                Stream.concat(getSentences().stream().map(BratSentence::getEntities).flatMap(Collection::stream),
                              getEntities().stream())
                      .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<BratEvent> getAllEvents() {
        return sentences == null ? new ArrayList<>(events) : Stream.concat(
                getSentences().stream().map(BratSentence::getEvents).flatMap(Collection::stream),
                getEvents().stream()).collect(Collectors.toList());
    }

    @JsonIgnore
    public List<BratBRelation> getAllRelations() {
        return sentences == null ? new ArrayList<>(relations) : Stream.concat(
                getSentences().stream().map(BratSentence::getRelations).flatMap(Collection::stream),
                getRelations().stream()).collect(Collectors.toList());
    }

    public JsonNode getEmbedJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("text", getText());

        ArrayNode entityNode = mapper.createArrayNode();
        getAllEntities().forEach(e -> entityNode.add(e.getEmbedJson()));
        node.set("entities", entityNode);

        ArrayNode relationNode = mapper.createArrayNode();
        getAllRelations().forEach(r -> relationNode.add(r.getEmbedJson()));
        node.set("relations", relationNode);

        //TODO: added json for events and attributes. added test cases!
        return node;
    }

    @Override
    public String toBratString() {
        List<String> attributeList = getAllEvents().stream()
                                                   .flatMap(e -> e.getAttribute().stream()
                                                                  .map(a -> a + " " + e.getId()))
                                                   .sorted()
                                                   .collect(Collectors.toList());

        return Stream.of(getAllEntities().stream().map(BratEntity::toBratString),
                         getAllEvents().stream().map(BratEvent::toBratString),
                         getAllRelations().stream().map(BratBRelation::toBratString),
                         IntStream.rangeClosed(1, attributeList.size())
                                  .mapToObj(i -> BratAnnotation.ATTRIBUTE_ID_PREFIX
                                          + i + "\t" + attributeList.get(i - 1)))
                     .flatMap(s -> s)
                     .collect(Collectors.joining("\n"));
    }
}
