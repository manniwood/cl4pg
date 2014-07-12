package com.manniwood.basicproj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ResourceUtil {

    public static String slurpFileFromClasspath(String path) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(ResourceUtil.class.getClassLoader().getResourceAsStream(path)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            throw new MPJWException("Could not read file " + path, e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new MPJWException("Could not close file " + path, e);
            }
        }
        return sb.toString();
    }
}
