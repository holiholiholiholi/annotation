package org.annotation.brat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BratIOTest {
    final static String bratDir = "org/annotation/brat/brat_directory_examples";
    final static String line_delimiter = "\n-\n";
    /**
     * Examples for how to read the manually annotation
     **/
    @Test
    public void ioTest() throws IOException, URISyntaxException {


        final boolean readSentenceId = true;

        final File inputDir = new File(BratIOTest.class.getClassLoader().getResource(bratDir).toURI());
        List<BratDocument> bratDocuments = BratUtils.readBratDirectory(inputDir, line_delimiter);

        List<BratSentence> bratSentences = bratDocuments.stream()
                .map(BratDocument::getSentences)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        log.info("Read sentence " + bratSentences.size());

        File outputDir = new File("target/brat_output");
        outputDir.mkdirs();

        BratUtils.saveAnnotation(bratSentences,null,line_delimiter,10,outputDir,true);
    }
}
