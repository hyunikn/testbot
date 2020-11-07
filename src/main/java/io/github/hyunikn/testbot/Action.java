package io.github.hyunikn.testbot;

import io.github.hyunikn.testbot.ScenarioEntry;

import org.json.JSONObject;

import com.google.common.collect.ImmutableSet;

import java.util.Deque;
import java.util.Calendar;
import java.util.GregorianCalendar;

import java.util.regex.Matcher;

/*
 params must be as follows:

 {
    ...
 }
 */
public abstract class Action extends EntryWithJSONParam {
    static final String ACTION_CLASS_NAME_PREFIX = "io.github.hyunikn.testbot.actions.";

    //------------------------------------
    // Protected
    //------------------------------------

    protected int runDepth = 0;

    protected enum Phase {
        Running,
        Reverting
    }

    protected Action(Env env, JSONObject spec, Options options, boolean hasSideEffect) {
        super(env, spec, options);
        this.hasSideEffect = hasSideEffect;
    }

    @Override
    public ImmutableSet<String> getParameterNames() {
        if (paramNames == null) {
            ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
            builder.addAll(super.getParameterNames());
            builder.add("_action_");
            paramNames = builder.build();
        }

        return paramNames;
    }

    protected abstract String checkPreCondition(Phase phase) throws Exception;
    protected abstract String checkPostCondition(Phase phase) throws Exception;
    protected abstract String run() throws Exception;
    protected String revert() throws Exception {
        return "reversion not implemented";
    }

    @Override
    final String classNamePrefix() {
        return ACTION_CLASS_NAME_PREFIX;
    }

    //-------------------------------------
    // Package
    //-------------------------------------

    final boolean hasSideEffect;    // per type

    @Override
    final String run(Env env, Options options, Deque<Action> actionStack, int depth) {
        Main.logger.info(Misc.getIndent(depth) + "==> " + entryName);
        Main.logger.info(Misc.getIndent(1) + params.toString());

        // ignore options, actionStack, and depth
        String err;

        // check pre-condition
        try {
            err = checkPreCondition(Phase.Running);
        } catch (Throwable e) {
            Main.logger.error("", e);
            err = "" + e.getMessage();
        }
        if (err != null) {
            err = String.format("error in a pre-condition check of %s: %s", params.toString(), err);
            Main.logger.error(Misc.getIndent(depth) + err);
            return err;
        }

        // run
        try {
            err = run();
        } catch (Throwable e) {
            Main.logger.error("", e);
            err = "" + e.getMessage();
        }
        if (err != null) {
            err = String.format("error in an action %s: %s", params.toString(), err);
            Main.logger.error(Misc.getIndent(depth) + err);
            return err;
        }

        // check post-condition
        try {
            err = checkPostCondition(Phase.Running);
        } catch (Throwable e) {
            Main.logger.error("", e);
            err = "" + e.getMessage();
        }
        if (err != null) {
            err = String.format("error in a post-condition check of %s: %s", params.toString(), err);
            Main.logger.error(Misc.getIndent(depth) + err);
            return err;
        }

        return null;
    }

    final String revert(Options options) {
        assert runDepth > 0;    // set before the action is added to the revert stack

        Main.logger.info(Misc.getIndent(runDepth) + "<-- " + entryName);
        Main.logger.info(Misc.getIndent(1) + params.toString());

        String err;
        // check post-condition
        try {
            err = checkPostCondition(Phase.Reverting);
        } catch (Throwable e) {
            Main.logger.error("", e);
            err = "" + e.getMessage();
        }
        if (err != null) {
            err = String.format("error in a post-condition check of %s: %s", params.toString(), err);
            Main.logger.error(Misc.getIndent(runDepth) + err);
            return err;
        }

        // revert
        try {
            err = revert();
        } catch (Throwable e) {
            Main.logger.error("", e);
            err = "" + e.getMessage();
        }
        if (err != null) {
            err = String.format("error in a reversion %s: %s", params.toString(), err);
            Main.logger.error(Misc.getIndent(runDepth) + err);
            return err;
        }

        // check pre-condition
        try {
            err = checkPreCondition(Phase.Reverting);
        } catch (Throwable e) {
            Main.logger.error("", e);
            err = "" + e.getMessage();
        }
        if (err != null) {
            err = String.format("error in a pre-condition check of %s: %s", params.toString(), err);
            Main.logger.error(Misc.getIndent(runDepth) + err);
            return err;
        }


        return null;
    }

    //------------------------------------
    // Private
    //------------------------------------

    private static ImmutableSet<String> paramNames = null;
}
