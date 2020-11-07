package io.github.hyunikn.testbot;

import org.json.JSONArray;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.InvocationTargetException;

import java.util.Map;
import java.util.Deque;

public class Scenario extends ScenarioEntry<ScenarioSpec> {

    static Scenario compile(Env env, int depth, Options options, Map<String, ScenarioEntryList> builtEntryLists,
            Deque<String> scenarioStack, ScenarioSpec spec)
        throws IOException, UnsupportedEncodingException, InstantiationException, IllegalAccessException,
                          InvocationTargetException {

        String name = spec.relPath();

        // check if this scenarios appeared previously
        boolean recursion = false;
        for (String n: scenarioStack) {
            if (n.equals(name)) {
                recursion = true;
                break;
            }
        }
        if (recursion) {
            throw new Error("currently, recursion on scenarios is not supported");
        }

        TemplateVars origTV = env.tmplVars;
        env.tmplVars = env.tmplVars.copy();
        try {
            // set template variables of this scenario
            if (spec.arguments != null) {
                String[] sarr = new String[spec.arguments.size()];
                int i = 0;
                for (String key: spec.arguments.keySet()) {
                    String val = env.tmplVars.replaceTemplateVars(spec.arguments.get(key));
                    env.tmplVars.setTemplateVars(key, val);
                    sarr[i++] = String.format("%s=%s", key, val);
                }
                Main.logger.info(Misc.getIndent(depth) + String.format(" with %s", String.join(", ", sarr)));
            }

            Scenario scenario = new Scenario(spec, name);
            scenarioStack.push(name);

            ScenarioEntryList entryList = builtEntryLists.get(name);
            if (entryList == null) {

                JSONArray jsonArr;
                try {
                   jsonArr = (JSONArray) Misc.readAndParseJSON(name, true);
                } catch (FileNotFoundException e) {
                    throw new Error("cannot find the file for scenario " + name);
                }

                entryList = ScenarioEntryList.compile(env, depth + 1, options, builtEntryLists, scenarioStack, name,
                        jsonArr);
            } else {

                // Ignore the returned scenario entry list.
                // This is just to check the jsonArr under the given environment env.
                ScenarioEntryList.compile(env, depth + 1, options, builtEntryLists, scenarioStack, null,
                        entryList.jsonArr);
            }

            scenarioStack.pop();
            scenario.setEntryList(entryList);

            return scenario;
        } finally {
            env.tmplVars = origTV;
        }
    }

    //-----------------------------------------------------------------------------------------------

    Scenario(ScenarioSpec spec, String entryName) {
        super(entryName, spec);
    }

    String run(Env env, Options options, Deque<Action> actionStack, int depth) {
        TemplateVars origTV = env.tmplVars;
        env.tmplVars = env.tmplVars.copy();
        try {
            // set template variables of this scenario
            if (spec.arguments != null) {
                String[] sarr = new String[spec.arguments.size()];
                int i = 0;
                for (String key: spec.arguments.keySet()) {
                    String val = env.tmplVars.replaceTemplateVars(spec.arguments.get(key));
                    env.tmplVars.setTemplateVars(key, val);
                    sarr[i++] = String.format("%s=%s", key, val);
                }
                Main.logger.info(Misc.getIndent(depth) + String.format(" with %s", String.join(", ", sarr)));
            }

            return entryList.run(env, options, actionStack, depth + 1, false);
        } finally {
            env.tmplVars = origTV;
        }
    }

    public String toString() {
        return "<Scenario " + entryName + ">";
    }

    //-------------------------------------------------------
    // Private
    //-------------------------------------------------------

    private void setEntryList(ScenarioEntryList entryList) {
        this.entryList = entryList;
    }

    private ScenarioEntryList entryList = null;
}
