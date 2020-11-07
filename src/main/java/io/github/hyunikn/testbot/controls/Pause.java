package io.github.hyunikn.testbot.controls;

import io.github.hyunikn.testbot.Env;
import io.github.hyunikn.testbot.Main;
import io.github.hyunikn.testbot.Misc;
import io.github.hyunikn.testbot.Action;
import io.github.hyunikn.testbot.Control;
import io.github.hyunikn.testbot.Options;
import io.github.hyunikn.testbot.Scenario;
import io.github.hyunikn.testbot.ScenarioEntryList;

import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.regex.Matcher;

import java.lang.reflect.InvocationTargetException;

import java.util.Map;
import java.util.Deque;

/*
 params must be as follows:

 {
    ...

    // optional params
    'seconds': INTEGER, default: 0
 }
 */
public class Pause extends Control {

    public Pause(Env env, JSONObject spec, Options options) {
        super(env, spec, options);

        seconds = Misc.JSON.optInt(params, "seconds", 0);
    }

    @Override
    public ImmutableSet<String> getParameterNames() {
        if (paramNames == null) {
            ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
            builder.addAll(super.getParameterNames());
            builder.add("seconds");
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
        String err = null;

        if (seconds > 0) {
            try {
                Main.logger.info(Misc.getIndent(depth) + String.format(" pausing %d seconds", seconds));
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                // do nothing
            }
        } else {
            UserChoice uc = getUserChoice();
            if (uc == UserChoice.Go) {
                // nothing to do
            } else if (uc == UserChoice.Quit) {
                err = "user selected quit";
            } else if (uc == UserChoice.Error) {
                err = "IO error while getting user input";
            } else {
                assert false: "unreachable";
            }
        }

        return err;
    }

    public String toString() {
        return "<Pause>";
    }

    //------------------------------------
    // Private
    //------------------------------------

    private final int seconds;

    private static ImmutableSet<String> paramNames = null;

    private static UserChoice getUserChoice() {
        for (;;) {
            System.out.println("");
            System.out.print("# Paused. Hit Enter to proceed (or Q to quit): ");

            int n, key;
            try {
                key = System.in.read();
                n = System.in.available();
                System.in.skip(n);
            } catch (IOException e) {
                Main.logger.error("", e);
                return UserChoice.Error;
            }
            if (n > 1) {
                System.err.println("# Unrecognized answer");
                continue;
            }

            switch (key) {
            case 10:    // Enter
                return UserChoice.Go;
            case 113: case 81:  // q or Q
                return UserChoice.Quit;
            default:
                System.err.println("# Unrecognized key " + new String(new byte[]{ (byte) key }));
            }
        }
    }

    private enum UserChoice {
        Go,
        Quit,
        Error   // indeed, not a user choice
    }
}
