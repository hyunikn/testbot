package io.github.hyunikn.testbot;

import java.util.Deque;

abstract class ScenarioEntry<SpecType> {
    protected String entryName;              // per type
    final protected SpecType spec;            // per instance

    ScenarioEntry(String entryName, SpecType spec) {
        this.entryName = entryName;
        this.spec = spec;
    }

    abstract String run(Env env, Options options, Deque<Action> actionStack, int depth);

    //---------------------------------------------------------
    // Private
    //---------------------------------------------------------

}


