package io.github.hyunikn.testbot.controls;

import io.github.hyunikn.testbot.Env;
import io.github.hyunikn.testbot.Main;
import io.github.hyunikn.testbot.Misc;
import io.github.hyunikn.testbot.Action;
import io.github.hyunikn.testbot.Control;
import io.github.hyunikn.testbot.Options;
import io.github.hyunikn.testbot.ScenarioEntryList;

import org.json.JSONArray;
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
    'do': [ <non-empty array of scenario entries> ],
 }
 */
public class ForkAndLoop extends Control {

    public ForkAndLoop(Env env, JSONObject spec, Options options) {
        super(env, spec, options);

        idToJoin = params.getString("id-to-join");
    }

    @Override
    public ImmutableSet<String> getParameterNames() {
        if (paramNames == null) {
            ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
            builder.addAll(super.getParameterNames());
            builder.add("id-to-join");
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

        JSONArray jarr = params.getJSONArray("do");  // NOTE: params has no 'do' member because
        assert body == null;
        body = ScenarioEntryList.compile(env, depth + 1, options, builtEntryLists, scenarioStack, null, jarr);
        assert body != null;
    }

    @Override
    public String run(Env env, Options options, Deque<Action> actionStack, int depth) {

        String err;

        ForkAndLoopThread child =
            new ForkAndLoopThread(body, env.spawn(idToJoin), options, actionStack, depth + 1, idToJoin);
        Thread t = env.threads.put(idToJoin, child);
        if (t != null) {
            err = "already has a thread of id " + idToJoin;
            Main.logger.error(Misc.getIndent(depth) + err);
            return err;
        }
        child.start();

        return null;
    }

    public String toString() {
        return "<ForkAndLoop>";
    }

    //------------------------------------
    // Package
    //------------------------------------

    static class ForkAndLoopThread extends Thread {
        public void run() {
            String err;
            Main.logger.info(Misc.getIndent(depth) + String.format(" fork-and-loop id=%s", idToJoin));

            try {
                while (!quited()) {
                    err = seList.run(env, options, actionStack, depth + 1, false);   // false: no local revert
                    if (err != null) {
                        Main.logger.error(Misc.getIndent(depth) +
                                String.format("ForkAndLoop %s iteration resulted in an error: %s", idToJoin, err));
                    }
                }
            } catch (Throwable e) {
                err = String.format("exception in thread for fork-and-loop id=%s: %s", idToJoin, e.getMessage());
                Main.logger.error(Misc.getIndent(depth) + err, e);
            }

            Main.logger.info(Misc.getIndent(depth) + String.format("ForkAndLoop %s is done", idToJoin));
        }

        //---------------------------------------------------
        // Package
        //---------------------------------------------------

        ForkAndLoopThread(ScenarioEntryList seList, Env env, Options options, Deque<Action> actionStack, int depth,
                String idToJoin) {

            super(env.threadId);

            this.seList = seList;
            this.env = env;
            this.options = options;
            this.actionStack = actionStack;
            this.depth = depth;
            this.idToJoin = idToJoin;
        }

        synchronized void quit() {
            quited = true;
        }

        //---------------------------------------------------
        // Private
        //---------------------------------------------------

        // in-parameters
        private final ScenarioEntryList seList;
        private final Env env;
        private final Options options;
        private final Deque<Action> actionStack;
        private final int depth;
        private final String idToJoin;

        private volatile boolean quited = false;

        private synchronized boolean quited() {
            return quited;
        }

    }

    //------------------------------------
    // Private
    //------------------------------------

    private final String idToJoin;
    private ScenarioEntryList body = null;

    private static ImmutableSet<String> paramNames = null;

}
