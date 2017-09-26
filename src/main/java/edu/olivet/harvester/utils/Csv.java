package edu.olivet.harvester.utils;

import com.google.inject.Singleton;
import edu.olivet.foundations.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class Csv {
    private static final Logger LOGGER = LoggerFactory.getLogger(Csv.class);

    protected List<String> rows = new ArrayList<>();


    public void clear() {
        this.rows.clear();
    }

    public void addRowFromList(List<String> lineElements,String delimiter) {
        String row = lineElements.stream()
                .map(value -> value.replaceAll("\"", "\"\""))
                .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
                .collect(Collectors.joining(delimiter));

        this.rows.add(row);
    }

    public void addRowFromList(List<String> lineElements) {
        String delimiter =  "\t";

        String row =  lineElements.stream()
                .map(value -> value.replaceAll("\"", "\"\""))
                .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
                .collect(Collectors.joining(delimiter));

        this.rows.add(row);
    }

    public void addRowFromArray(String[] lineElements,String delimiter) {
        String row = Stream.of(lineElements)
                .map(value -> value.replaceAll("\"", "\"\""))
                .map(value -> Stream.of("\"", ",").anyMatch(value::contains) ? "\"" + value + "\"" : value)
                .collect(Collectors.joining(delimiter));

        this.rows.add(row);
    }

    public String toCSVString() {

        String csvString = rows.stream()
                .collect(Collectors.joining(System.getProperty("line.separator")));

        return  csvString;
    }

    public File saveToFile(File file) {

        Tools.writeStringToFile(file, this.toCSVString());

        return file;
    }
}

