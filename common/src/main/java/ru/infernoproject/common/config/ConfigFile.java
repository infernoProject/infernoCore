package ru.infernoproject.common.config;

import ru.infernoproject.common.utils.HexBin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigFile {

    private Map<String, String> configData;
    private static Logger logger = LoggerFactory.getLogger(ConfigFile.class);

    private static final Pattern sectionPattern = Pattern.compile(
        "^\\s*\\[(?<section>[^\\]]+)\\]\\s*$"
    );
    private static final Pattern configPattern = Pattern.compile(
        "^\\s*(?<key>[^#\\s]+)\\s*=\\s*(?<value>([^#\'\"]+)|('[^']+')|(\"[^\"]+\"))\\s*(?<comment>#\\s*(.*?))?$"
    );
    private static final Pattern commentPattern = Pattern.compile(
        "^\\s*(#\\s*(?<comment>.*?))$"
    );

    private ConfigFile(Map<String, String> config) {
        configData = config;
    }

    public static ConfigFile readConfig(File configFile) throws IOException {
        Map<String, String> config = new HashMap<>();

        try (BufferedReader configReader = Files.newBufferedReader(configFile.toPath())) {

            String configLine;
            String sectionName = "DEFAULT";

            int lineNumber = 1;
            while ((configLine = configReader.readLine()) != null) {
                Matcher sectionMatcher = sectionPattern.matcher(configLine);
                Matcher configMatcher = configPattern.matcher(configLine);
                Matcher commentMatcher = commentPattern.matcher(configLine);

                if (sectionMatcher.matches()) {
                    sectionName = sectionMatcher.group("section");
                } else if (configMatcher.matches()) {
                    String keyName = configMatcher.group("key");
                    String keyValue = configMatcher.group("value");

                    config.put(sectionName.toLowerCase() + "." + keyName.toLowerCase(), keyValue);
                } else if (!commentMatcher.matches() && (configLine.length() > 0)) {
                    logger.warn("Invalid configuration found on {}:{}", configFile.getName(), lineNumber);
                }

                lineNumber++;
            }
        }

        return new ConfigFile(config);
    }

    public String getString(String key, String defaultValue) {
        return System.getProperty(key.toLowerCase(), configData.getOrDefault(key.toLowerCase(), defaultValue));
    }

    public byte[] getHexBytes(String key, byte[] defaultValue) {
        String hexString = System.getProperty(key.toLowerCase(), configData.get(key.toLowerCase()));
        return (hexString != null) ? HexBin.decode(hexString) : defaultValue;
    }

    public Integer getInt(String key, Integer defaultValue) {
        return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
    }

    public Long getLong(String key, Long defaultValue) {
        return Long.parseLong(getString(key, String.valueOf(defaultValue)));
    }

    public Float getFloat(String key, Float defaultValue) {
        return Float.parseFloat(getString(key, String.valueOf(defaultValue)));
    }

    public Double getDouble(String key, Double defaultValue) {
        return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
    }

    public BigInteger getBigInt(String key, BigInteger defaultValue) {
        return new BigInteger(getString(key, defaultValue.toString()));
    }

    public <T> List<T> getList(String key, Class<T> valueType) {
        List<T> result = new ArrayList<>();
        String configValue = getString(key, null);

        if (configValue != null) {
            for (String valuePart: configValue.split("\\s*,\\s*")) {
                result.add(valueType.cast(valuePart));
            }
        }

        return result;
    }

    public List<String> getKeys(String keyPattern) {
        List<String> keys = new ArrayList<>();

        configData.keySet().stream()
            .filter(key -> key.startsWith(keyPattern))
            .filter(key -> !keys.contains(key))
            .forEach(keys::add);

        System.getProperties().keySet().stream()
            .map(key -> (String) key)
            .filter(key -> key.startsWith(keyPattern))
            .filter(key -> !keys.contains(key))
            .forEach(keys::add);

        return keys;
    }

    public boolean hasKey(String key) {
        return configData.containsKey(key) || System.getProperties().containsKey(key);
    }
}
