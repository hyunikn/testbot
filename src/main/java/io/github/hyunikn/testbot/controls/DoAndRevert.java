package io.github.hyunikn.testbot.controls;

import io.github.hyunikn.testbot.Env;
import io.github.hyunikn.testbot.Action;
import io.github.hyunikn.testbot.Control;
import io.github.hyunikn.testbot.Options;
import io.github.hyunikn.testbot.ScenarioEntryList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;

import java.lang.reflect.InvocationTargetException;

import java.util.Map;
import java.util.Deque;

/*
 params must be as follows:

 {
    ...

    'do': [ <non-empty array of scenario entries> ],
 }
 */
public class DoAndRevert extends Control {

    public DoAndRevert(Env env, JSONObject spec, Options options) {
        super(env, spec, options);
    }

    @Override
    public ImmutableSet<String> getParameterNames() {
        if (paramNames == null) {
            ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
            builder.addAll(super.getParameterNames());
            builder.add("do");
            paramNames = builder.build();
        }

        return paramNames;
    }

    @Override
    public void compile(Env env, int depth, Options options, Map<String, ScenarioEntryList> builtEntryLists,
            Deque<String> scenarioStack)
        throws IOException, InstantiationException, IllegalAccessException, InvocationTargetException {

        JSONArray jarr = spec.getJSONArray("do");
        entryList = ScenarioEntryList.compile(env, depth + 1, options, builtEntryLists, scenarioStack, null, jarr);
    }

	@Override
    public String run(Env env, Options options, Deque<Action> actionStack, int depth) {
        return entryList.run(env, options, actionStack, depth + 1, true);
    }

    public String toString() {
        return "<DoAndRevert>";
    }

    //------------------------------------
    // Private
    //------------------------------------

    private ScenarioEntryList entryList = null;
    private static ImmutableSet<String> paramNames = null;
}
