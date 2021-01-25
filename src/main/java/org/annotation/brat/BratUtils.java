package org.annotation.brat;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public final class BratUtils {

    public static final String ANNO_CONFIG_FILE = "annotation.conf";

    public static final String ANNO_FILE_EXTENSION = ".ann";

    public static final String TEXT_FILE_EXTENSION = ".txt";

    public static final FileFilter TEXT_FILE_FILTER = pathname -> pathname.getName().endsWith(TEXT_FILE_EXTENSION);

    public static final FileFilter ANNO_FILE_FILTER = pathname -> pathname.getName().endsWith(ANNO_FILE_EXTENSION);

    private static final String DEFAULT_SENTENCE_DELIMITER = "\n";

    private static final String DEFAULT_ENCODING = "utf8";

    public static List<BratDocument> saveAnnotation(@NonNull final List<BratSentence> bratSentences,
                                                    final String namePrefix,
                                                    final String lineDelimiter,
                                                    final int docSize,
                                                    @NonNull final File outputDir,
                                                    final boolean saveConfig) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        List<BratDocument> bratDocuments = new ArrayList<>();

        for (int i = 0; i <= bratSentences.size() / docSize; i++) {
            int start = docSize * i;
            int end = Math.min(docSize * (i + 1), bratSentences.size());
            if (start >= end) {
                break;
            }
            bratDocuments.add(
                    BratUtils.writeAnnotation(bratSentences.subList(start, end), lineDelimiter,
                            (StringUtils.isEmpty(namePrefix) ? "" : namePrefix + "_") + (start + 1) + "-" + end,
                            outputDir));
        }
        if (saveConfig) {
            writeAnnotationConfig(bratDocuments, outputDir);
        }
        return bratDocuments;
    }

    public static BratDocument writeAnnotation(@NonNull final Collection<? extends BratSentence> sentences,
                                               @NonNull final String delimiter,
                                               @NonNull final String docId,
                                               @NonNull final File outputDir) throws IOException {
        BratDocument document = BratUtils.generateDocument(sentences, docId, delimiter);
        writeAnnotation(document, docId, outputDir);
        return document;
    }

    public static void writeAnnotation(@NonNull final BratDocument document,
                                       @NonNull final String docId,
                                       @NonNull final File outputDir) throws IOException {


        File textFile = new File(outputDir, docId + BratUtils.TEXT_FILE_EXTENSION);
        File annoFile = new File(outputDir, docId + BratUtils.ANNO_FILE_EXTENSION);

        FileUtils.write(textFile, document.getText() + "\n", "utf-8");
        FileUtils.write(annoFile, document.toBratString() + "\n", "utf8");
    }

    public static void writeAnnotationConfig(@NonNull final Collection<? extends BratDocument> collection,
                                             @NonNull final File outputDir) throws IOException {

        StringBuilder sb = new StringBuilder();

        //get event infos
        Map<String, Map<String, Set<String>>> eventTypeMap = new HashMap<>();
        collection.stream().map(BratDocument::getAllEvents).flatMap(Collection::stream).forEach(event -> {
            Map<String, Set<String>> argMap = eventTypeMap.computeIfAbsent(event.getType(), k -> new HashMap<>());
            event.getArgument()
                    .forEach((key, value) -> argMap.computeIfAbsent(key, k -> new HashSet<>()).add(value.getType()));
        });


        //add entity
        sb.append("\n\n[entities]\n\n");
        Set<String> entityTypes = getEntityTypes(collection);
        entityTypes.removeAll(eventTypeMap.keySet());
        entityTypes.stream().sorted().forEach(t -> sb.append(t).append("\n"));

        sb.append("\n\n[events]\n\n");
        eventTypeMap.forEach((key, value) -> {
            sb.append(key).append("\t")
                    .append(value.entrySet()
                            .stream()
                            .map(e -> e.getKey() + "?:"
                                    + e.getValue().stream().sorted().collect(Collectors.joining("|")))
                            .collect(Collectors.joining(", ")))
                    .append("\n");
        });

        sb.append("\n\n[relations]\n\n");
        Set<String> relationTypes = getRelationTypes(collection);
        for (String relationType : relationTypes) {
            sb.append(relationType).append("\t");
            List<Set<String>> argumentTypes =
                    collection.stream()
                            .map(BratDocument::getAllRelations)
                            .flatMap(Collection::stream)
                            .filter(relation -> relationType.equalsIgnoreCase(relation.getType()))
                            .map(r -> r.getArguments()
                                    .stream()
                                    .map(e -> Set.of(e.getType()))
                                    .collect(Collectors.toList()))
                            .reduce(BratUtils::mergeTypes).orElse(List.of());
            sb.append(IntStream.range(0, argumentTypes.size())
                    .mapToObj(i -> BratBRelation.DEFAULT_ARG + i + ":" +
                            argumentTypes.get(i).stream().sorted().collect(Collectors.joining("|")))
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
        }


        sb.append("\n\n[attributes]\n\n");
        //TODO: add attribute

        FileUtils.write(new File(outputDir, ANNO_CONFIG_FILE), sb.toString(), "utf-8");

    }

    public static List<BratDocument> readBratDirectoryR(@NonNull final File inputDir, final String lineDilimiter) throws IOException {
        List<BratDocument> docs = new ArrayList<>(readBratDirectory(inputDir, lineDilimiter));
        File[] subDirs = inputDir.listFiles(File::isDirectory);
        if (null != subDirs) {
            for (File subDir : subDirs) {
                docs.addAll(readBratDirectory(subDir, lineDilimiter));
            }
        }
        return docs;
    }

    public static List<BratDocument> readBratDirectory(@NonNull final File inputDir, final String lineDilimiter) throws IOException {
        List<BratDocument> docs = new ArrayList<>();
        for (File f : inputDir.listFiles(ANNO_FILE_FILTER)) {
            String docId = getBaseFilename(f, ANNO_FILE_EXTENSION);
            File textF = new File(inputDir, docId + BratUtils.TEXT_FILE_EXTENSION);
            if (!textF.exists()) {
                log.warn("File " + textF.getName() + " does not exist!");
                continue;
            }
            log.debug("Reading from " + f.getName());

            BratDocument bratDocument = BratUtils.readAnnotation(textF, f, lineDilimiter);
            if (!bratDocument.getEvents().isEmpty() || !bratDocument.getEntities().isEmpty()) {
                log.warn("There are events or nes among sentences!");
                continue;
            }
            log.debug("Get " + bratDocument.getSentences().size() + " sentences.");
            docs.add(bratDocument);
        }
        return docs;
    }

    public static BratDocument readAnnotation(@NonNull final File textFile,
                                              @NonNull final File annoFile) throws IOException {
        return readAnnotation(textFile, annoFile, DEFAULT_SENTENCE_DELIMITER);
    }

    public static BratDocument readAnnotation(@NonNull final File textFile, @NonNull final File annoFile,
                                              final String delimiter) throws IOException {
        String text = FileUtils.readFileToString(textFile, DEFAULT_ENCODING);
        List<String> annoLines = FileUtils.readLines(annoFile, DEFAULT_ENCODING);

        return readAnnotation(getBaseFilename(textFile,TEXT_FILE_EXTENSION), text, annoLines, delimiter);
    }

    public static BratDocument readAnnotation(@NonNull final String id, @NonNull final String textFile,
                                              @NonNull final String annoFile) throws IOException {

        return readAnnotation(id, textFile, annoFile, DEFAULT_SENTENCE_DELIMITER);
    }

    public static BratDocument readAnnotation(@NonNull final String id, @NonNull final String textFile,
                                              @NonNull final String annoFile,
                                              @NonNull final String delimiter) throws IOException {
        return readAnnotation(id, org.utils.IOUtils.openStream(textFile),
                org.utils.IOUtils.openStream(annoFile), delimiter);
    }

    public static BratDocument readAnnotation(@NonNull final String id, @NonNull final InputStream textInput,
                                              @NonNull final InputStream annoInput) throws IOException {

        return readAnnotation(id, textInput, annoInput, DEFAULT_SENTENCE_DELIMITER);
    }

    public static BratDocument readAnnotation(@NonNull final String id, @NonNull final InputStream textInput,
                                              @NonNull final InputStream annoInput,
                                              @NonNull final String delimiter) throws IOException {

        String text = IOUtils.toString(textInput, DEFAULT_ENCODING);
        List<String> annoLines = IOUtils.readLines(annoInput, DEFAULT_ENCODING);
        return readAnnotation(id, text, annoLines, delimiter);
    }

    public static BratDocument readAnnotation(@NonNull final String id, @NonNull final String text,
                                              @NonNull final List<String> annoLines, final String delimiter) {
        BratDocument document = new BratDocument(id, text);

        Map<String, BratEntity> entities = readEntities(annoLines);
        document.getEntities().addAll(entities.values());

        List<BratEvent> events = readEvents(annoLines, entities);
        events.forEach(e -> e.setDocId(id));
        document.getEvents().addAll(events);

        List<BratBRelation> relations = readRelations(annoLines, entities);
        document.getRelations().addAll(relations);

        Map<String, Set<String>> attributes = readAttributes(annoLines);
        document.getEvents().stream().filter(e -> attributes.containsKey(e.getId()))
                .forEach(e -> e.getAttribute().addAll(attributes.get(e.getId())));

        //generate sentences
        if (null != delimiter) {
            List<BratSentence> sentences = splitSentence(text, delimiter);
            sentences.forEach(s -> s.setId(id + "_" + s.getStartPosition()));
            document.setSentences(sentences);

            //mapped annotation to sentences
            distributeAnnotation(document, sentences);
            if (sentences.stream().mapToInt(s -> s.getEntities().size()).sum() != entities.size()) {
                log.warn("There are some entities can not be mapped into sentences!");
            }
        }
//        return sentences;
        return document;
    }

    public static void distributeAnnotation(@NonNull final BratDocument document, @NonNull final Collection<BratSentence> sentences) {
        sentences.forEach(sentence -> distributeAnnotation(document, sentence));
    }

    public static void distributeAnnotation(@NonNull final BratDocument document, @NonNull final BratSentence sent) {
        document.getEntities().stream().filter(e -> isIn(e, sent)).forEach(sent.getEntities()::add);
        document.getEvents().stream().filter(e -> isIn(e, sent)).forEach(sent.getEvents()::add);
        document.getRelations().stream().filter(r -> isIn(r, sent)).forEach(sent.getRelations()::add);

        document.getEntities().removeAll(sent.getEntities());
        document.getRelations().removeAll(sent.getRelations());
        document.getEvents().removeAll(sent.getEvents());
    }

    public static List<BratEntity> readEntities(File file) throws IOException {
        //TODO-2: to deal with the text with "\n"
        return new ArrayList<>(readEntities(FileUtils.readLines(file, DEFAULT_ENCODING)).values());
    }

    public static String getAnnoFileName(@NonNull final String textFileName) {
        String fileName = textFileName.substring(0, textFileName.length() - BratUtils.TEXT_FILE_EXTENSION.length());
        return fileName + BratUtils.ANNO_FILE_EXTENSION;
    }


    public static BratDocument generateDocument(@NonNull final Collection<? extends BratSentence> sentences,
                                                @NonNull final String docId,
                                                @NonNull final String lineDelimiter) {

        int startPos = 0, entityN = 1, relationN = 1, eventN = 1;


        List<String> attributeList = new ArrayList<>();

        for (BratSentence sentence : sentences) {

            for (BratEntity entity : sentence.getEntities()) {
                entity.setIdNumber(entityN);
                entityN++;
                entity.setStartPosition(entity.getStartPosition() - sentence.getStartPosition() + startPos);
                entity.setEndPosition(entity.getEndPosition() - sentence.getStartPosition() + startPos);
            }

            for (BratBRelation relation : sentence.getRelations()) {
                relation.setIdNumber(relationN);
                relationN++;
            }
            for (BratEvent event : sentence.getEvents()) {
                event.setIdNumber(eventN);
                eventN++;
                event.getAttribute().forEach(a -> attributeList.add(a + " " + event.getId()));
            }

//            sentence.setStartPosition(startPos);
//            sentence.setEndPosition(startPos + sentence.getText().length());

            startPos += sentence.getText().length() + lineDelimiter.length();
        }

        BratDocument document = new BratDocument(docId, sentences.stream()
                .map(BratSentence::getText)
                .collect(Collectors.joining(lineDelimiter)));
//        document.getSentences().addAll(sentences);
        sentences.forEach(s -> document.getEntities().addAll(s.getEntities()));
        sentences.forEach(s -> document.getEvents().addAll(s.getEvents()));
        sentences.forEach(s -> document.getRelations().addAll(s.getRelations()));

        return document;
    }

    private static Map<String, Set<String>> readAttributes(@NonNull final List<String> annoLines) {
        Map<String, Set<String>> map = new HashMap<>();

        annoLines.stream()
                .filter(StringUtils::isNoneEmpty)
                .filter(l -> 'A' == l.charAt(0))
                .map(BratUtils::readAttribute)
                .filter(Objects::nonNull)
                .forEach(a -> map.computeIfAbsent(a[1], k -> new HashSet<>()).add(a[0]));
        return map;
    }

    private static String[] readAttribute(@NonNull final String line) {
        String[] tokens1 = line.split("\t");
        if (tokens1.length != 2) {
            log.warn("Illegal brat attribute format1: \"" + line + "\"");
            return null;
        }

        String[] tokens2 = tokens1[1].split(" ");
        if (tokens2.length != 2) {
            log.warn("Illegal brat attribute format2: \"" + line + "\"");
            return null;
        }

        return tokens2;
    }

    private static List<BratBRelation> readRelations(@NonNull final List<String> annoLines,
                                                     @NonNull final Map<String, BratEntity> entities) {
        List<BratBRelation> results = new ArrayList<>();

        annoLines.stream()
                .filter(StringUtils::isNoneEmpty)
                .filter(l -> 'R' == l.charAt(0))
                .map(l -> BratUtils.readRelation1(l, entities))
                .filter(Objects::nonNull)
                .forEach(results::add);
        IntStream.range(0, annoLines.size())
                .filter(i -> StringUtils.isNoneEmpty(annoLines.get(i)) && '*' == annoLines.get(i).charAt(0))
                .mapToObj(i -> BratUtils.readRelation2(i, annoLines.get(i), entities))
                .filter(Objects::nonNull)
                .forEach(results::add);

        return results;

    }

    private static BratBRelation readRelation1(@NonNull final String line,
                                               @NonNull final Map<String, BratEntity> entities) {
        String[] tokens1 = line.split("\t");
        if (tokens1.length != 2) {
            log.warn("Illegal brat relation * format1: \"" + line + "\"");
            return null;
        }
        String[] tokens2 = tokens1[1].split(" ");
        if (3 != tokens2.length || !tokens2[1].startsWith("Arg1:") || !tokens2[2].startsWith("Arg2:")) {
            log.warn("Illegal brat relation * format2: \"" + line + "\"");
            return null;
        }
        String key1 = tokens2[1].substring(5);
        String key2 = tokens2[2].substring(5);
        if (!entities.containsKey(key1) || !entities.containsKey(key2)) {
            log.warn("Illegal brat relation * argument: \"" + line + "\"");
            return null;
        }
        BratBRelation relation = new BratBRelation(tokens2[0]);
        relation.setId(tokens1[0]);
        relation.getArguments().addAll(List.of(entities.get(key1), entities.get(key2)));
        return relation;
    }

    private static BratBRelation readRelation2(final int lineN, @NonNull final String line,
                                               @NonNull final Map<String, BratEntity> entities) {
        String[] tokens1 = line.split("\t");
        if (tokens1.length != 2) {
            log.warn("Illegal brat relation * format1: \"" + line + "\"");
            return null;
        }
        String[] tokens2 = tokens1[1].split(" ");
        if (tokens2.length < 3) {
            log.warn("Illegal brat relation * format2: \"" + line + "\"");
            return null;
        }

        BratBRelation relation = new BratBRelation(tokens2[0]);
//        relation.setId("*:" + lineN);
        relation.setId("*");
        for (int i = 1; i < tokens2.length; i++) {
            if (!entities.containsKey(tokens2[i])) {
//                log.warn("Illegal brat relation * argument: \"" + line + "\"");
                continue;
            }
            relation.getArguments().add(entities.get(tokens2[i]));
        }
        if (relation.getArguments().size() < 2) {
            log.warn("Illegal brat relation * argument: \"" + line + "\"");
            return null;
        }
        return relation;
    }

    private static List<BratEvent> readEvents(@NonNull final List<String> annoLines,
                                              @NonNull final Map<String, BratEntity> entities) {
        return annoLines.stream()
                .filter(StringUtils::isNoneEmpty)
                .filter(l -> 'E' == l.charAt(0))
                .map(l -> BratUtils.readEvent(l, entities))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }

    private static BratEvent readEvent(@NonNull final String line, @NonNull final Map<String, BratEntity> entities) {
        String[] tokens1 = line.split("\t");
        if (tokens1.length != 2) {
            log.warn("Illegal brat event format1: \"" + line + "\"");
            return null;
        }

        String[] args = tokens1[1].split(" ");
        if (args.length < 1) {
            log.warn("Illegal brat event format2: \"" + line + "\"");
            return null;
        }

        String[] trigger = readEventArgument(args[0]);
        if (null == trigger || !entities.containsKey(trigger[1])) {
            log.warn("Illegal brat event format (illegal trigger!) \"" + line + "\"");
            return null;
        }

        BratEvent bratEvent = new BratEvent(trigger[0], entities.get(trigger[1]));
        bratEvent.setId(tokens1[0]);
        IntStream.range(1, args.length)
                .mapToObj(i -> readEventArgument(args[i]))
                .filter(Objects::nonNull)
                .filter(a -> entities.containsKey(a[1]))
                .forEach(a -> bratEvent.getArgument().put(a[0], entities.get(a[1])));
        return bratEvent;
    }

    private static String[] readEventArgument(@NonNull final String string) {
        String[] array = string.split(":");
        return 2 == array.length ? array : null;

    }

    private static Map<String, BratEntity> readEntities(@NonNull final Collection<? extends String> annoLines) {
        return annoLines.stream()
                .filter(StringUtils::isNoneEmpty)
                .filter(s -> 'T' == s.charAt(0))
                .map(BratUtils::readEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(BratEntity::getId, Function.identity()));
    }

    private static BratEntity readEntity(@NonNull final String line) {
//        if ('T' != line.charAt(0)) {
//            //not a entity
//            return null;
//        }
        String[] tokens = line.split("\t");
        if (tokens.length != 3) {
            log.warn("Illegal brat entity format:\"" + line + "\"");
            return null;
        }


        String[] tokens2 = tokens[1].split(" ");
        if (tokens2.length < 3) {
            // there are some special cases and the line looks like:
            // T1	entityType 3804 3829;3831 3845  XXXX
            log.warn("Illegal brat entity type/pos format." + line);
            return null;
        }
        BratEntity be = new BratEntity(tokens2[0], tokens[2]);
        be.setId(tokens[0]);
        be.setStartPosition(Integer.parseInt(tokens2[1]));
        be.setEndPosition(Integer.parseInt(tokens2[tokens2.length - 1]));
        return be;
    }

    private static List<BratSentence> splitSentence(@NonNull final File file,
                                                    @NonNull final String delimiter) throws IOException {
        return splitSentence(FileUtils.readFileToString(file, DEFAULT_ENCODING), delimiter);
    }

    private static List<BratSentence> splitSentence(@NonNull final String text, @NonNull final String delimiter) {
        List<BratSentence> sentences = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int next = text.indexOf(delimiter, index);
            if (next < 0) {
                if (index == 0) {
                    //found no delimiter in the text. No sentences will be added
                    return sentences;
                }
                sentences.add(new BratSentence(text.substring(index), index, text.length(), ""));
                break;
            } else {
                sentences.add(new BratSentence(text.substring(index, next), index, next, ""));
                index = next + delimiter.length();
            }
        }
        return sentences;
    }

    private static boolean isIn(@NonNull final BratEvent event, @NonNull final BratSentence sentence) {
        return isIn(event.getStartPosition(), event.getEndPosition(), sentence.getStartPosition(),
                sentence.getEndPosition());
    }

    private static boolean isIn(@NonNull final BratBRelation relation, @NonNull final BratSentence sentence) {
        return relation.getArguments().stream().allMatch(a -> isIn(a, sentence));
    }

    private static boolean isIn(@NonNull final BratEntity entity, @NonNull final BratSentence sentence) {
        return isIn(entity.getStartPosition(), entity.getEndPosition(), sentence.getStartPosition(),
                sentence.getEndPosition());
    }

    private static boolean isIn(final int eStart, final int eEnd, final int sStart, final int sEnd) {
        return eStart >= sStart && eEnd <= sEnd;
    }

    private static String getBaseFilename(@NonNull final File file, @NonNull final String extension){

        return getBaseFilename(file.getName(),extension);
    }
    private static String getBaseFilename(@NonNull final String fileName, @NonNull final String extension){
        return fileName.substring(0, fileName.length() -extension.length());
    }

    private static final String getColor() {
        Random random = new Random();
        int r = random.nextInt(255 - 130) + 131;
        int g = random.nextInt(255 - 130) + 131;
        int b = random.nextInt(255 - 130) + 131;
        return "#" + toHexString(r) + toHexString(g) + toHexString(b);
    }

    private static String toHexString(int i) {
        return (i < 16 ? "0" : "") + Integer.toHexString(i);
    }

    private static Set<String> getEntityTypes(@NonNull final Collection<? extends BratDocument> collection) {
        return collection.stream()
                .map(BratDocument::getAllEntities)
                .flatMap(Collection::stream)
                .map(BratEntity::getType).collect(Collectors.toSet());
    }

    private static Set<String> getRelationTypes(Collection<? extends BratDocument> collection) {
        return collection.stream()
                .map(BratDocument::getAllRelations)
                .flatMap(Collection::stream)
                .map(BratBRelation::getType).collect(Collectors.toSet());
    }

    private static List<Set<String>> mergeTypes(@NonNull final List<Set<String>> list1,
                                                @NonNull final List<Set<String>> list2) {
        final int intersection = Math.min(list1.size(), list2.size());
        List<Set<String>> result =
                IntStream.range(0, intersection)
                        .mapToObj(i -> Stream.concat(list1.get(i).stream(), list2.get(i).stream())
                                .collect(Collectors.toSet()))
                        .collect(Collectors.toList());
        if (list1.size() == list2.size()) {
            return result;
        }
        List<Set<String>> l = list1.size() > list2.size() ? list1 : list2;
        IntStream.range(intersection, l.size()).forEach(i -> result.add(new HashSet<>(l.get(i))));
        return result;
    }
}

    