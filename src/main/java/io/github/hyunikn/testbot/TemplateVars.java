package io.github.hyunikn.testbot;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.TreeMap;
import java.util.LinkedList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TemplateVars {

    //-------------------------------------------------------------
    // Package
    //-------------------------------------------------------------

    TemplateVars() {
        this.varToVal = new TreeMap<>();
    }

    public static boolean hasTemplateVars(String str) {
        return templateVarPattern.matcher(str).find();
    }

    public String replaceTemplateVars(String str) {
        if (!templateVarPattern.matcher(str).find()) {
            return str;
        }

        String result = str;
        for (String var: varToVal.keySet()) {
            String val = varToVal.get(var);
            assert val != null;
            result = result.replaceAll("%" + var + "%", val);
        }

        Matcher m = templateVarPattern.matcher(result);
        if (m.find()) {
            throw new Error("spec has undefined template variable " + m.group());
        }

        return result;
    }

    public void setTemplateVars(String varPrefix, Object val) {

        String var;
        String oldVal;

        if (val == null) {
            throw new Error("tried to set null to " + varPrefix);
        }

        if (val instanceof JSONArray) {
            JSONArray jarr = (JSONArray) val;
            int len = jarr.length();
            for (int i = 0; i < len; i++) {
                var = varPrefix + "." + i;
                val = jarr.get(i);
                if (val == null) {
                    throw new Error("tried to set null to " + var);
                }
                oldVal = varToVal.get(var);
                if (oldVal != null) {
                    throw new Error("conflicting template variables of an identical name " + var);
                }
                varToVal.put(var, val.toString());
            }
        } else {
            var = varPrefix;    // varPrefix is just var in this case
            oldVal = varToVal.get(var);
            if (oldVal != null) {
                throw new Error("conflicting template variables of an identical name " + var);
            }
            varToVal.put(var, val.toString());
        }
    }

    public void unsetTemplateVars(String varPrefix, Object val) {

        String var;
        String oldVal;

        if (val instanceof JSONArray) {
            JSONArray jarr = (JSONArray) val;
            int len = jarr.length();
            for (int i = 0; i < len; i++) {
                val = jarr.get(i);
                var = varPrefix + "." + i;
                oldVal = varToVal.get(var);
                assert oldVal.equals(val.toString());
                varToVal.remove(var);
            }
        } else {
            var = varPrefix;    // varPrefix is just var in this case
            oldVal = varToVal.get(var);
            assert oldVal.equals(val.toString());
            varToVal.remove(var);
        }
    }

    public boolean isEmpty() {
        return varToVal.isEmpty();
    }

    public TemplateVars copy() {
        return new TemplateVars(varToVal);
    }

    //-------------------------------------------------------------
    // Private
    //-------------------------------------------------------------

    private static final Pattern templateVarPattern = Pattern.compile("%[^%]+%");

    private final TreeMap<String, String> varToVal;

    private TemplateVars(TreeMap<String, String> varToVal) {
        this.varToVal = (TreeMap<String, String>) varToVal.clone();
    }

}
