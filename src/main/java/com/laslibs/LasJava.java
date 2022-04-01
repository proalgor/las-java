package com.laslibs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class LasJava
{
    private final String lasString;

    public LasJava(String fileSource, Boolean loadFile){
        if(loadFile){
            File file = new File(fileSource);
            try {
                this.lasString = new String(Files.readAllBytes(file.toPath()));
            } catch (IOException exception){
                throw new IllegalArgumentException("Pass a valid file" + exception.getMessage());
            }
            return;
        }
        this.lasString = fileSource;
    }

    private <T> T[] getSliceOfArray(T[] arr, int startIndex, int endIndex) {
        return Arrays
                .copyOfRange (
                        arr,
                        startIndex,
                        endIndex);
    }
    private String removeComment(String input){
        return Arrays.stream(
                input
                .trim()
                .split("\n"))
                .map(val -> val.replaceAll("^\\s+", ""))
                .filter(f -> !(f.charAt(0) == '#'))
                .collect(Collectors.joining("\n"));
    }

    private String[] metaData(){
        String metaPart = this.lasString.split("~V(?:\\w*\\s*)*\\n\\s*")[1]
                .split("~")[0];
        metaPart = removeComment(metaPart);
        List<String[]> refinedMeta = Arrays.stream(metaPart.split("\n"))
                .map(m -> getSliceOfArray(m.split("\s{2,}|\s*:"),0,2 ))
                .collect(Collectors.toList());
        return refinedMeta.stream().map(r -> r[1]).toArray(String[]::new);
    }

    public Boolean getWrap(){
        String[] meta = metaData();
        return meta[1].equalsIgnoreCase("yes");
    }

    public Double getVersion(){
        String[] meta = metaData();
        return Double.parseDouble(meta[0]);
    }

    private Map<String, Map<String, String>> getProperty(String type) {
        // Set regex for the different properties
        HashMap<String, String> props = new HashMap<>() {{
            put("curve", "~C(?:\\w*\\s*)*\\n\\s*");
            put("param", "~P(?:\\w*\\s*)*\\n\\s*");
            put("well", "~W(?:\\w*\\s*)*\\n\\s*");
        }};
        String[] ls = lasString.split(props.get(type));
        String wp = "";
        if (ls.length > 0) {
            wp = ls[1].split("~")[0];
            wp = removeComment(wp);
        }
        Map<String, Map<String, String>> wellProps = new HashMap<>();
        if (!(wp.length() > 0)) {
            throw new LasException("The property " + type + " does not exist");
        }
        Arrays.stream(wp.split("\n")).forEach((str) -> {
            String obj = str.replaceAll("\\s *[.]\\s +", "   none   ");
            String title = obj.split("[.]|\\s +")[0];
            String unit = obj.trim().split("^\\w+\\s*[.]*s*")[1]
                    .split("\\s +")[0];
            String description = obj.split("[:]")[1].trim().isEmpty() ? "none" : obj.split("[:]")[1].trim();
            String[] valPrep = obj.split("[:]")[0].split("\\s{2,}\\w*\\s{2,}");
            String value = valPrep.length > 2 && (valPrep[valPrep.length - 1].isEmpty() || valPrep[valPrep.length - 1] == null) ? valPrep[valPrep.length - 2] : valPrep[valPrep.length - 1];
            String finalValue = value.length() > 0 ? value.trim() : value;
            wellProps.put(title, new HashMap<>() {{
                put("unit", unit);
                put("value", finalValue);
                put("description", description);
            }});
        });
        return wellProps;
    }

    public Map<String, Map<String, String>> getCurveParams(){
        return getProperty("curve");
    }

    public Map<String, Map<String, String>> getWellParams(){
        return getProperty("well");
    }
}
