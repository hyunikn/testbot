package io.github.hyunikn.testbot.controls;

import io.github.hyunikn.testbot.Env;
import io.github.hyunikn.testbot.Main;
import io.github.hyunikn.testbot.Misc;
import io.github.hyunikn.testbot.Action;
import io.github.hyunikn.testbot.Options;
import io.github.hyunikn.testbot.ScenarioEntryList;

import org.json.JSONObject;

import java.util.List;
import java.util.Deque;
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
public class Spawn extends IterateBase {

    public Spawn(Env env, JSONObject spec, Options options) {
        super(env, spec, options);
    }

    @Override
    public String run(Env env, Options options, Deque<Action> actionStack, int depth) {

        int len;
        if ((len = iterVals.size()) == 0) {
            return null;
        }

        // create threads
        int i = 0;
        SpawnThread [] threads = new SpawnThread[len];
        for (Object val: iterVals) {
            ScenarioEntryList body = iterBodies.get(val);
            assert body != null;
            Env childEnv = env.spawn(val.toString());
            childEnv.tmplVars.setTemplateVars(loopVar, val);
            threads[i] = new SpawnThread(body, childEnv, options, actionStack, depth + 1, loopVar, val);
            i++;
        }
        // run threads
        for (i = 0; i < len; i++) {
            threads[i].start();
        }

        // join threads and collect their error messages
        StringBuffer err = new StringBuffer();
        for (i = 0; i < len; i++) {
            SpawnThread t = threads[i];
            try {
                t.join();
                if (t.err != null) {
                    err.append(String.format("error for %s=%s: %s\n", loopVar, iterVals.get(i).toString(), t.err));
                }
            } catch (InterruptedException e) {
                err.append(String.format("thread interrupted for %s=%s\n", loopVar, iterVals.get(i).toString()));
            }
        }

        // report error if any
        if (err.length() > 0) {
            return err.toString();
        } else {
            return null;
        }
    }

    public String toString() {
        return "<Spawn>";
    }

    //------------------------------------------------------------
    // Private
    //------------------------------------------------------------

    private static class SpawnThread extends Thread {
        SpawnThread(ScenarioEntryList seList, Env env, Options options, Deque<Action> actionStack, int depth,
                String loopVar, Object val) {
            super(env.threadId);

            this.seList = seList;
            this.env = env;
            this.options = options;
            this.actionStack = actionStack;
            this.depth = depth;
            this.loopVar = loopVar;
            this.val = val;
        }

        public void run() {
            Main.logger.info(Misc.getIndent(depth) + String.format(" with %s=%s", loopVar, val.toString()));

            try {
                err = seList.run(env, options, actionStack, depth + 1, false);   // false: no revert
            } catch (Throwable e) {
                err = String.format("exception in thread for %s=%s: %s", loopVar, val.toString(), e.getMessage());
                Main.logger.error(err, e);
            }

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
        private final String loopVar;
        private final Object val;

        // out-parameters
        private String err = null;
    }
}
