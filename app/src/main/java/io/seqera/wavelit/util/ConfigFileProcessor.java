package io.seqera.wavelit.util;

import io.seqera.wave.api.ContainerConfig;
import io.seqera.wavelit.json.JsonHelper;

import java.io.*;
import java.net.URL;

/**
 * This implements parsing and assiging the value from json config file to ContainerConfig object
 *
 * @author Munish Chouhan <munishcohn@gmail.com>
 */
public class ConfigFileProcessor {

    public static ContainerConfig process(String configFile) throws IOException {
        String content = loadFile(configFile);
        ContainerConfig containerConfig = JsonHelper.fromJson(content, ContainerConfig.class);
        return containerConfig;
    }

    private static String loadFile(String configFile) throws IOException {
        InputStream inputStream;
        if(configFile.startsWith("http")){
            URL url = new URL(configFile);
            inputStream = url.openStream();
        } else {
            File inputFile = new File(configFile);
            inputStream = new FileInputStream(inputFile);
        }
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
        } finally {
            inputStream.close();
        }
        return contentBuilder.toString();
    }
}
