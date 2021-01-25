package org.annotation.brat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class BratSentence {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<BratEntity> entities = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<BratBRelation> relations = new ArrayList<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<BratEvent> events = new ArrayList<>();

    private String text;


    private int startPosition;

    private int endPosition;


    private String id;
}