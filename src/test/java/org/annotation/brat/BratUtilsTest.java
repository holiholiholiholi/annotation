package org.annotation.brat;


import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class ...
 */

public class BratUtilsTest {


    @Test
    public void testBratReader() throws IOException {
        String textFile = "org/annotation/brat/acquisition1.txt";
        String annoFile = "org/annotation/brat/acquisition1.ann";

        BratDocument document = BratUtils.readAnnotation("test", textFile, annoFile);
        assertEquals(document.getSentences().size(), 5);
//        assertEquals(document.getEntities().size(),32);
//        assertEquals(document.getEvents().size(),5);
//        assertEquals(document.getRelations().size(),4);
        assertEquals(document.getEvents().size(), 1);
        assertTrue(document.getRelations().isEmpty());
        assertTrue(document.getEntities().isEmpty());

        assertEquals(document.getAllEntities().size(), 32);
        assertEquals(document.getAllEvents().size(), 5);
        assertEquals(document.getAllRelations().size(), 4);

//        document.getAllEvents().forEach(System.out::println);
        document.getSentences().forEach(System.out::println);
//        document.getAllRelations().forEach(System.out::println);

        JsonUtils.writeList(new File("target/bratSentences.jsons"), document.getSentences());
    }

    @Test
    public void testBratReader2() throws IOException {
        String textFile = "org/annotation/brat/acquisition3.txt";
        String annoFile = "org/annotation/brat/acquisition3.ann";
        BratDocument document = BratUtils.readAnnotation("test", textFile, annoFile);
        assertEquals(document.getSentences().size(), 3);
        document.getSentences().forEach(System.out::println);
    }



    @Test
    public void testBratString() throws IOException {
        String textFile = "org/annotation/brat/acquisition1.txt";
        String annoFile = "org/annotation/brat/acquisition1.ann";

        List<String> originalAnno = IOUtils.readLines(org.utils.IOUtils.openStream(annoFile), "utf8");
        originalAnno.removeIf(StringUtils::isBlank);
        originalAnno.sort(String::compareTo);

        BratDocument document = BratUtils.readAnnotation("test", textFile, annoFile);
        List<String> convertAnno = Arrays.stream(document.toBratString().split("\n"))
                                         .filter(Objects::nonNull)
                                         .sorted()
                                         .collect(Collectors.toList());

        assertEquals(originalAnno.size(), convertAnno.size());

        BratDocument document2 = BratUtils.readAnnotation(document.getId(),
                                                          org.utils.IOUtils.openStream(textFile),
                                                          IOUtils.toInputStream(document.toBratString(),"utf8"));
        assertEquals(document, document2);
//        System.out.println(document.toBratString());
    }


}
