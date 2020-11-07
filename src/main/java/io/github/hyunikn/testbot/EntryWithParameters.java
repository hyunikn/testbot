package io.github.hyunikn.testbot;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

abstract class EntryWithJSONParam extends ScenarioEntry<JSONObject> {

    //---------------------------------------------------------------------
    // Protected
    //---------------------------------------------------------------------

    protected final JSONObject params;

    protected ImmutableSet<String> getParameterNames() {
        return emptyStrSet;
    }

    protected String [] getExcludedSpecMembers() {
        return emptyStrArray;
    }

    //----------------------------------------------------------------------
    // Package
    //----------------------------------------------------------------------

    abstract String classNamePrefix();

    EntryWithJSONParam(Env env, JSONObject spec, Options options) {
        super(null, spec);  // null: entry name is set below

        Set<String> paramsInSpec = spec.keySet();
        if (!this.getParameterNames().containsAll(paramsInSpec)) {
            paramsInSpec.removeAll(this.getParameterNames());
            throw new Error("illegal parameters " + paramsInSpec);
        }

        this.params = getParamsFromSpec(env, spec);

        // set entry name
        String className = this.getClass().getName();
        String prefix = this.classNamePrefix();
        assert className.startsWith(prefix);
        this.entryName = className.substring(prefix.length());

        // NOTE: params cannot be set in this contructor because
        // params can be updated for every run for controls reflecting the environment of each run.
    }

    //----------------------------------------------------------------------
    // Private
    //----------------------------------------------------------------------

    private static final String[] emptyStrArray = new String[0];
    private static final ImmutableSet<String> emptyStrSet = ImmutableSet.<String>of();

    private JSONObject getParamsFromSpec(Env env, JSONObject spec) {
        String s;
        String[] excluded = getExcludedSpecMembers();
        if (excluded.length == 0) {
            s = env.tmplVars.replaceTemplateVars(spec.toString());
        } else {
            JSONObject p = new JSONObject(spec.toString());
            for (String m: excluded) {
                p.remove(m);
            }
            s = env.tmplVars.replaceTemplateVars(p.toString());
        }
        return new JSONObject(s);
    }

}

