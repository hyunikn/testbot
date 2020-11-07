package io.github.hyunikn.testbot;

import org.json.JSONObject;
import org.json.JSONException;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.InvocationTargetException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Map;
import java.util.Deque;

public abstract class Control extends EntryWithJSONParam {
    static final String CONTROL_CLASS_NAME_PREFIX = "io.github.hyunikn.testbot.controls.";

    protected Control(Env env, JSONObject spec, Options options) {
        super(env, spec, options);
    }

    @Override
    public ImmutableSet<String> getParameterNames() {
        if (paramNames == null) {
            ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
            builder.addAll(super.getParameterNames());
            builder.add("_control_");
            paramNames = builder.build();
        }

        return paramNames;
    }

    @Override
    final String classNamePrefix() {
        return CONTROL_CLASS_NAME_PREFIX;
    }

    protected abstract void compile(Env env, int depth, Options options, Map<String, ScenarioEntryList> builtEntryLists,
            Deque<String> scenarioStack)
        throws IOException, UnsupportedEncodingException, InstantiationException, IllegalAccessException,
                          InvocationTargetException;


    @Override
    protected abstract String run(Env env, Options options, Deque<Action> actionStack, int depth);

    //----------------------------------------------------------
    // Private
    //----------------------------------------------------------

    private static ImmutableSet<String> paramNames = null;

}
