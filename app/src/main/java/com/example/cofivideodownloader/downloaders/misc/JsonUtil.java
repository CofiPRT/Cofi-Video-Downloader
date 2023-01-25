package com.example.cofivideodownloader.downloaders.misc;

import com.example.cofivideodownloader.MainActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Function;

public class JsonUtil {
    private static final String TAG = "JsonUtil";

    private JsonUtil() { }

    public static <T> T get(JsonElement json, String path, Function<JsonElement, T> mapper) throws JsonPathException {
        // split the path by dots, but also by square brackets with numbers inside
        String[] parts = path.split("\\.|(?=\\[\\d+])");

        JsonElement current = json;
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            // if the part is an array index, get the element at that index
            if (part.matches("\\[\\d+]")) {
                int index = Integer.parseInt(part.substring(1, part.length() - 1));

                if (!current.isJsonArray())
                    throw new JsonPathException(String.format("Path '%s' is not an array", currentPath));

                JsonArray array = current.getAsJsonArray();

                if (index >= array.size())
                    throw new JsonPathException(String.format(
                        "Path '%s' is out of bounds (index %d, size %d)", currentPath, index, array.size()
                    ));

                current = array.get(index);
                currentPath.append(part);
            } else {
                // otherwise, get the element with that key
                current = current.getAsJsonObject().get(part);

                if (currentPath.length() > 0)
                    currentPath.append(".");

                currentPath.append(part);
            }

            if (current == null)
                throw new JsonPathException(String.format("Path '%s' does not exist", currentPath));
        }

        try {
            return mapper.apply(current);
        } catch (Exception e) {
            throw new JsonPathException(String.format("Path '%s' is not of the expected type", currentPath));
        }
    }

    public static String getString(JsonElement json, String path) throws JsonPathException {
        return get(json, path, JsonElement::getAsString);
    }

    public static Integer getInt(JsonElement json, String path) throws JsonPathException {
        return get(json, path, JsonElement::getAsInt);
    }

    public static Boolean getBoolean(JsonElement json, String path) throws JsonPathException {
        return get(json, path, JsonElement::getAsBoolean);
    }

    public static JsonObject getObject(JsonElement json, String path) throws JsonPathException {
        return get(json, path, JsonElement::getAsJsonObject);
    }

    public static JsonArray getArray(JsonElement json, String path) throws JsonPathException {
        return get(json, path, JsonElement::getAsJsonArray);
    }

    public static JsonElement getElement(JsonElement json, String path) throws JsonPathException {
        return get(json, path, Function.identity());
    }

    public static boolean hasElement(JsonElement json, String path) throws JsonPathException {
        return getElement(json, path) != null;
    }

    public static boolean hasPath(JsonElement json, String path) {
        try {
            return hasElement(json, path);
        } catch (JsonPathException e) {
            return false;
        }
    }

    public static JsonElement parseJSON(HttpsURLConnection connection, MainActivity activity) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append(System.lineSeparator());

            return JsonParser.parseString(builder.toString());
        } catch (IOException e) {
            activity.logToast(TAG, "IO Exception", e);
        }

        return null;
    }

}
