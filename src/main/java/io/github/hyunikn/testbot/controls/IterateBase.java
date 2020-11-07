package io.github.hyunikn.testbot.controls;

import io.github.hyunikn.testbot.Env;
import io.github.hyunikn.testbot.Main;
import io.github.hyunikn.testbot.Misc;
import io.github.hyunikn.testbot.Action;
import io.github.hyunikn.testbot.Control;
import io.github.hyunikn.testbot.Options;
import io.github.hyunikn.testbot.Scenario;
import io.github.hyunikn.testbot.TemplateVars;
import io.github.hyunikn.testbot.ScenarioEntryList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.InvocationTargetException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.regex.Matcher;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Deque;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
/*
 params must be as follows:

 // CAUTION: you must choose one and only one of 'in' and 'below' parameters
 {
    ...

    'for-each': '<loop variable name>',
    // choose only one of the following 'in' and 'below'
    'in':
        . {
            // required
            'begin': INTEGER,
            'end': INTEGER,
            // optional
            'step': INTEGER, default: 1
          }, or
        . [ <non-empty array of any values> ]
    'below': INTEGER,
    'do': [ <non-empty array of scenario entries> ],

    // optional
    'except': [ <non-empty array of integers that are skipped by the loop variable> ]
 }
 */
public abstract class IterateBase extends Control {

    public IterateBase(Env env, JSONObject spec, Options options) {
        super(env, spec, options);

        // loopVar
        String var = spec.getString("for-each");
        if (TemplateVars.hasTemplateVars(var)) {
            throw new Error("'for-each' parameter may not contain a template variable");
        }
        loopVar = var;

        // iterVals
        iterVals = (List<Object>) parseParams(params);
    }

    @Override
    public ImmutableSet<String> getParameterNames() {
        if (paramNames == null) {
            ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
            builder.addAll(super.getParameterNames());
            builder.add("for-each");
            builder.add("in");
            builder.add("below");
            builder.add("except");
            builder.add("do");
            paramNames = builder.build();
        }

        return paramNames;
    }

    @Override
    public void compile(Env env, int depth, Options options, Map<String, ScenarioEntryList> builtEntryLists,
            Deque<String> scenarioStack)
        throws IOException, UnsupportedEncodingException, InstantiationException, IllegalAccessException,
                          InvocationTargetException {

        if (iterVals.size() == 0) {
            return;
        }

        JSONArray jarr = spec.getJSONArray("do");  // NOTE: params has no 'do' member because
                                                   //       loops do not fill the template holes in 'do' member.
        for (Object val: iterVals) {
            Main.logger.info(Misc.getIndent(depth) + String.format(" with %s=%s", loopVar, val.toString()));

            env.tmplVars.setTemplateVars(loopVar, val);
            try {
                ScenarioEntryList l =
                    ScenarioEntryList.compile(env, depth + 1, options, builtEntryLists, scenarioStack, null, jarr);
                assert l != null;
                iterBodies.put(val, l);
            } finally {
                env.tmplVars.unsetTemplateVars(loopVar, val);
            }
        }
    }

    //------------------------------------
    // Protected
    //------------------------------------

    protected final String loopVar;
    protected final List<Object> iterVals;
    protected final Map<Object, ScenarioEntryList> iterBodies = new HashMap<>();

    @Override
    protected String [] getExcludedSpecMembers() {
        return excluded;
    }

    //------------------------------------
    // Private
    //------------------------------------

    private static final String[] excluded = new String[] { "do" };

    private static ImmutableSet<String> paramNames = null;

    // check params and get information from it, as 'parsing' usually means
    private static Object parseParams(JSONObject params) {

        List<Object> iterVals = new LinkedList<>();

        Set<Integer> exceptSet = new HashSet<>();
        JSONArray except = Misc.JSON.optJSONArray(params, "except", null);
        if (except != null) {
            int len = except.length();
            if (len == 0) {
                throw new Error("except parameter must be a non-empty array");
            }
            for (int i = 0; i < len; i++) {
                exceptSet.add(except.getInt(i));
            }
        }

        if (params.has("in")) {
            if (params.has("below")) {
                throw new Error("do not give both 'in' and 'below' parameters");
            }

            Object o = params.get("in");
            if (o instanceof JSONObject) {

                JSONObject jobj = (JSONObject) o;

                int begin = jobj.getInt("begin");
                int end = jobj.getInt("end");
                int step = Misc.JSON.optInt(jobj, "step", 1);
                if (step <= 0) {
                    throw new Error("step must be a positive integer");
                }

                Integer val;
                if (begin < end) {
                    for (int i = begin; i < end; i += step) {
                        val = i;
                        if (!exceptSet.contains(val)) {
                            iterVals.add(val);
                        }
                    }
                } else {
                    for (int i = begin; i > end; i -= step) {
                        val = i;
                        if (!exceptSet.contains(val)) {
                            iterVals.add(val);
                        }
                    }
                }

            } else if (o instanceof JSONArray) {

                if (except != null) {
                    throw new Error("except clause is not allowed in this array form of iterated values");
                }

                JSONArray jarr = (JSONArray) o;
                if (jarr.length() == 0) {
                    throw new Error("empty array is not allowed as values to iterateBase");
                }

                for (Object val: jarr) {
                    iterVals.add(val);
                }

            } else {
                throw new Error("invalid 'in': must be an JSON object or array");
            }

        } else if (params.has("below")) {
            int bound = params.getInt("below");
            Integer val;
            for (int i = 0; i < bound; i++) {
                val = i;
                if (!exceptSet.contains(val)) {
                    iterVals.add(val);
                }
            }
        } else {
            throw new Error("you have to specify one and only one of 'in' and 'below' parameters");
        }

        return iterVals;
    }

}
