package io.github.hyunikn.testbot;

import io.github.hyunikn.testbot.annot.InspectionsOnError;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Deque;
import java.util.TreeSet;
import java.util.LinkedList;

import java.util.concurrent.ConcurrentLinkedDeque;

public class ScenarioEntryList {

    public static ScenarioEntryList compile(Env env, int depth, Options options, Map<String,
            ScenarioEntryList> builtEntryLists, Deque<String> scenarioStack, String scenario, JSONArray jsonArr)
        throws IOException, UnsupportedEncodingException, InstantiationException, IllegalAccessException,
                          InvocationTargetException {

        ScenarioEntryList result = new ScenarioEntryList(jsonArr);
        if (scenario != null) {
            // only when this scenario entry list belongs to a scenrio which has not been compiled yet
            assert builtEntryLists.get(scenario) == null;
            builtEntryLists.put(scenario, result);
        }

        int len = jsonArr.length();
        for (int i = 0; i < len; i++) {
            Object o = jsonArr.get(i);
            if (o instanceof JSONObject) {

                JSONObject jsonObj = (JSONObject) o;

                String prefix, name;
                String actionName = jsonObj.optString(ACTION_MEMBER_NAME, null);
                String controlName = jsonObj.optString(CONTROL_MEMBER_NAME, null);
                if (actionName != null && controlName == null) {
                    prefix = Action.ACTION_CLASS_NAME_PREFIX;
                    name = actionName;
                } else if (actionName == null && controlName != null) {
                    prefix = Control.CONTROL_CLASS_NAME_PREFIX;
                    name = controlName;
                } else if (actionName != null && controlName != null) {
                    throw new Error("entry " + i + " has both '" +
                            ACTION_MEMBER_NAME + "' and '" + CONTROL_MEMBER_NAME + "' members");
                } else {
                    throw new Error("entry " + i + " has neither '" +
                            ACTION_MEMBER_NAME + "' nor '" + CONTROL_MEMBER_NAME + "' member");
                }

                if (actionName == null) {
                    Main.logger.info(Misc.getIndent(depth) + "<" + name + ">");
                } else {
                    Main.logger.info(Misc.getIndent(depth) + name);
                }

                Class<ScenarioEntry> entryCls;
                try {
                    entryCls = (Class<ScenarioEntry>) Class.forName(prefix + name);

                    if (actionName != null) {
                        // check the inspections-on-error if any
                        InspectionsOnError ioe = (InspectionsOnError) entryCls.getAnnotation(InspectionsOnError.class);
                        if (ioe != null) {
                            String[] inspectionNames = ioe.value();
                            if (inspectionNames == null) {
                                throw new Error("action class " + actionName + " has null inspections");
                            }
                            if (inspectionNames.length == 0) {
                                throw new Error("action class " + actionName + " has empty inspections");
                            }

                            for (String n: inspectionNames) {
                                if (!Main.inspectionMap.containsKey(n)) {
                                    throw new Error("action class " + actionName + " has an undefined inspection " + n);
                                }
                                // NOTE: using static dupCheckSet is safe: compile is run on the main (single) thread
                                dupCheckSet.add(n);
                            }

                            if (inspectionNames.length != dupCheckSet.size()) {
                                throw new Error("action class " + actionName + " has a duplicate entry in inspections");
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Error("unknown scenario entry " + name);
                }

                ScenarioEntry se = null;
                try {
                    se = entryCls.getConstructor(Env.class, JSONObject.class, Options.class)
                                 .newInstance(env, jsonObj, options);
                    if (controlName != null) {
                        assert se instanceof Control;
                        ((Control) se).compile(env, depth, options, builtEntryLists, scenarioStack);
                    }
                } catch (NoSuchMethodException e) {
                    assert false: "Java class for scenario entry " + name + " does not have a valid constructor";
                }

                result.addEntry(se);

            } else if (o instanceof String) {

                String name = (String) o;

                ScenarioSpec childSpec;
                try {
                    childSpec = ScenarioSpec.parseScenario(name);
                } catch (Error e) {
                    throw new Error("illegal entry " + i + ": " + e.getMessage());
                }
                Main.logger.info(Misc.getIndent(depth) + "<Scenario " + childSpec.relPath() + ">");
                Scenario child = Scenario.compile(env, depth, options, builtEntryLists, scenarioStack, childSpec);
                assert child != null;
                result.addEntry(child);

            } else {
                throw new Error("entry " + i + " is neither a JSON Object nor a string)");
            }
        }

        return result;
    }

    //----------------------------------------------------------------------

    JSONArray jsonArr;

    ScenarioEntryList(JSONArray jsonArr) {
        this.jsonArr = jsonArr;
    }

    void addEntry(ScenarioEntry e) {
        entryList.add(e);
    }

    public String run(Env env, Options options, Deque<Action> globalStack, int depth, boolean localRevert) {

        String res;
        Deque<Action> localStack = localRevert ? new ConcurrentLinkedDeque<Action>() : null;

        boolean revertTurnedOn = false;;
        if (localRevert && options.noRevert) {
            options.noRevert = false;
            revertTurnedOn = true;
        }

        try {
            String err = null;
            for (ScenarioEntry e: entryList) {
                if (Main.shutdown) {
                    err = "shutting down";
                    break;
                }

                if (e instanceof Action) {
                    Action a = (Action) e;
                    try {
                        a = a.getClass()
                            .getConstructor(Env.class, JSONObject.class, Options.class)
                            .newInstance(env, a.spec, options);
                    } catch (InstantiationException e0) {
                        throw new Error(e0);
                    } catch (IllegalAccessException e1) {
                        throw new Error(e1);
                    } catch (InvocationTargetException e2) {
                        throw new Error(e2);
                    } catch (NoSuchMethodException e3) {
                        throw new Error(e3);
                    }

                    // wait interval seconds, if neccessary
                    if (options.interval > 0) {
                        try {
                            Thread.sleep(options.interval * 1000);
                        } catch (InterruptedException _) {
                            // do nothing
                        }
                    }

                    // run the action
                    a.runDepth = depth;
                    err = a.run(env, options, null, depth);
                    if (err == null) {
                        if (a.hasSideEffect) {
                            if (localRevert) {
                                localStack.push(a);
                            } else if (!options.noRevert) {
                                globalStack.push(a);
                            }
                        }
                    } else {
                        // run the inspections-on-error if any
                        InspectionsOnError ioe;
                        ioe = (InspectionsOnError) a.getClass().getAnnotation(InspectionsOnError.class);
                        if (ioe != null) {
                            String[] inspectionNames = ioe.value();
                            for (String n: inspectionNames) {
                                Inspection insp = Main.inspectionMap.get(n);
                                if (insp != null) {     // insp can be null
                                    try {
                                        String ret = insp.inspect();
                                        Main.logger.error(Misc.getIndent(depth) + "Inspection " + n + ":\n" + ret);
                                    } catch (Throwable e2) {
                                        Main.logger.error(Misc.getIndent(depth) + "Inspection " + n + ":\n " + e2);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Main.logger.info(Misc.getIndent(depth) + e.toString());
                    err = e.run(env, options, localRevert ? localStack : globalStack, depth);
                }

                if (err == null) {
                    if (options.interactive) {
                        UserChoice uc = getUserChoice();
                        if (uc == UserChoice.Go) {
                            // nothing to do
                        } else if (uc == UserChoice.Quit) {
                            err = "user selected quit";
                            break;
                        } else if (uc == UserChoice.FF) {
                            options.interactive = false;
                        } else if (uc == UserChoice.Error) {
                            err = "IO error while getting user input";
                            break;
                        } else {
                            assert false: "unreachable";
                        }
                    }
                } else {
                    break;  // I hope the action which resulted in an error did not make a side effect.
                }
            }

            if (localRevert && !localStack.isEmpty()) {
                Main.logger.info(Misc.getIndent(depth) + "reverting actions in the current local stack");
                while (!localStack.isEmpty()) {
                    Action a = localStack.pop();
                    String err2 = a.revert(options);
                    if (err2 != null) {
                        throw new Error("error while reverting");
                    }
                }
            }

            return err;
        } finally {
            if (revertTurnedOn) {
                options.noRevert = true;
            }
        }
    }

    //-------------------------------------------------------
    // Private
    //-------------------------------------------------------

    static final String ACTION_MEMBER_NAME = "_action_";
    static final String CONTROL_MEMBER_NAME = "_control_";
    static final Set<String> dupCheckSet = new TreeSet<>();

    private final List<ScenarioEntry> entryList = new LinkedList<>();

    private enum UserChoice {
        Go,
        Quit,
        FF,     // fast-forward
        Error   // indeed, not a user choice
    }

    private static UserChoice getUserChoice() {
        for (;;) {
            System.out.println("");
            System.out.print("# Hit Enter to proceed (or Q to quit, or F to go fast-forward): ");

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
            case 102: case 70: // f or F
                return UserChoice.FF;
            default:
                System.err.println("# Unrecognized key " + new String(new byte[]{ (byte) key }));
            }
        }
    }
}

