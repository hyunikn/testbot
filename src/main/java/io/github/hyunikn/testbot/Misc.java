package io.github.hyunikn.testbot;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import io.github.getify.minify.Minify;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;

import java.util.Map;
import java.util.List;
import java.util.TreeMap;

public class Misc {
    static class Path {
        static final int CRP_ERROR_EMPTY = 1;
        static final int CRP_ERROR_SPACE = 2;
        static final int CRP_ERROR_TWO_SLASHES = 3;
        static final int CRP_ERROR_LEADING_SLASHES = 4;
        static final int CRP_ERROR_TRAILING_SLASHES = 5;

        static int checkRelativePath(String path) {
            if (path.length() == 0) {
                return CRP_ERROR_EMPTY;
            }
            if (path.indexOf(' ') >= 0) {
                return CRP_ERROR_SPACE;
            }
            if (path.indexOf("//") >= 0) {
                return CRP_ERROR_TWO_SLASHES;
            }
            if (path.charAt(0) == '/') {
                return CRP_ERROR_LEADING_SLASHES;
            }
            if (path.charAt(path.length() - 1) == '/') {
                return CRP_ERROR_TRAILING_SLASHES;
            }

            return 0;
        }
    }

    public static class JSON {
        // Why redefine?
        //  : corresponding methods of JSONObject return given default value even when the value type mismatches

        public static int optInt(JSONObject params, String key, int dfltVal) {
            if (params.has(key)) {
                return params.getInt(key);
            } else {
                return dfltVal;
            }
        }
        public static boolean optBoolean(JSONObject params, String key, boolean dfltVal) {
            if (params.has(key)) {
                return params.getBoolean(key);
            } else {
                return dfltVal;
            }
        }
        public static String optString(JSONObject params, String key, String dfltVal) {
            if (params.has(key)) {
                return params.getString(key);
            } else {
                return dfltVal;
            }
        }
        public static <E extends Enum<E>> E optEnum(JSONObject params, Class<E> cls, String key, E dfltVal) {
            if (params.has(key)) {
                return params.getEnum(cls, key);
            } else {
                return dfltVal;
            }
        }
        public static JSONArray optJSONArray(JSONObject params, String key, JSONArray dfltVal) {
            if (params.has(key)) {
                return params.getJSONArray(key);
            } else {
                return dfltVal;
            }
        }
        public static JSONObject optJSONObject(JSONObject params, String key, JSONObject dfltVal) {
            if (params.has(key)) {
                return params.getJSONObject(key);
            } else {
                return dfltVal;
            }
        }
    }

    static Object readAndParseJSON(String jsonPath, boolean isArray) throws IOException, UnsupportedEncodingException {

        FileInputStream in = null;
        try {
            in = new FileInputStream(jsonPath);
            int len = in.available();
            byte [] buf = new byte[len];
            in.read(buf);

            String txt = Minify.minify(new String(buf, "US-ASCII"));
            return (isArray ? new JSONArray(txt) : new JSONObject(txt));

        } catch (JSONException e) {
            throw new Error("cannot parse the JSON file '" + jsonPath + "': " + e.getMessage());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private static final Map<Integer, String> indents = new TreeMap<>();
    public static synchronized String getIndent(int depth) {

        String ret = indents.get(depth);
        if (ret == null) {
            StringBuffer buf = new StringBuffer("!");
            for (int i = 0; i < depth; i++) {
                buf.append("    ");
            }
            ret = buf.toString();
            indents.put(depth, ret);
        }

        return ret;
    }

}
