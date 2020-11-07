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
    'do': [ <non-empty array of scenario entries> ],     // inherited from Loop

    // optional
    'except': [ <non-empty array of integers that are skipped by the loop variable> ]
 }
 */
public class Iterate extends IterateBase {

    public Iterate(Env env, JSONObject spec, Options options) {
        super(env, spec, options);
    }

    @Override
    public String run(Env env, Options options, Deque<Action> actionStack, int depth) {

        if (iterVals.size() == 0) {
            return null;
        }

        for (Object val: iterVals) {
            Main.logger.info(Misc.getIndent(depth) + String.format(" with %s=%s", loopVar, val.toString()));

            ScenarioEntryList body = iterBodies.get(val);
            assert body != null;
            env.tmplVars.setTemplateVars(loopVar, val);
            String err;
            try {
                err = body.run(env, options, actionStack, depth + 1, false);   // false: no revert
            } finally {
                env.tmplVars.unsetTemplateVars(loopVar, val);
            }
            if (err != null) {
                return err;
            }
        }

        return null;
    }

    public String toString() {
        return "<Iterate>";
    }
}
