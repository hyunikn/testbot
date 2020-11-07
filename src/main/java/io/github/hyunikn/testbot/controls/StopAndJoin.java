package io.github.hyunikn.testbot.controls;

import io.github.hyunikn.testbot.Env;
import io.github.hyunikn.testbot.Main;
import io.github.hyunikn.testbot.Misc;
import io.github.hyunikn.testbot.Action;
import io.github.hyunikn.testbot.Control;
import io.github.hyunikn.testbot.Options;
import io.github.hyunikn.testbot.ScenarioEntryList;

import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.InvocationTargetException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Map;
import java.util.Deque;
/*
 params must be as follows:

 // CAUTION: you must choose one and only one of 'in' and 'below' parameters
 {
    ...
    // required
    'id-to-join': INTEGER
 }
 */
public class StopAndJoin extends Control {

    public StopAndJoin(Env env, JSONObject spec, Options options) {
        super(env, spec, options);

        idToJoin = params.getString("id-to-join");
    }

    @Override
    public ImmutableSet<String> getParameterNames() {
        if (paramNames == null) {
            ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
            builder.addAll(super.getParameterNames());
            builder.add("id-to-join");
            paramNames = builder.build();
        }

        return paramNames;
    }

    @Override
    public void compile(Env env, int depth, Options options, Map<String, ScenarioEntryList> builtEntryLists,
            Deque<String> scenarioStack)
        throws IOException, UnsupportedEncodingException, InstantiationException, IllegalAccessException,
                          InvocationTargetException {
        // nothing to do

    }

    @Override
    public String run(Env env, Options options, Deque<Action> actionStack, int depth) {

        String err;

        Thread t = env.threads.remove(idToJoin);
        if (t == null) {
            err = "cannot find a thread of id " + idToJoin;
            Main.logger.error(Misc.getIndent(depth) + err);
            return err;
        } else if (!(t instanceof ForkAndLoop.ForkAndLoopThread)) {
            err = String.format("thread of id %s is not a ForkAndLoop thread", idToJoin);
            Main.logger.error(Misc.getIndent(depth) + err);
            return err;
        }

        ((ForkAndLoop.ForkAndLoopThread) t).quit();
        if (t != Thread.currentThread()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                return "interrupted";
            }
        }

        Main.logger.info(Misc.getIndent(depth) + String.format("joined a ForkAndLoop thread of id %s", idToJoin));

        return null;
    }

    public String toString() {
        return "<StopAndJoin>";
    }


    //------------------------------------
    // Protected
    //------------------------------------

    private final String idToJoin;

    private static ImmutableSet<String> paramNames = null;
}
