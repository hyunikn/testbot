package io.github.hyunikn.testbot;

import java.util.concurrent.ConcurrentHashMap;

public class Env {

    //-------------------------------------------------------
    // Package
    //-------------------------------------------------------

    public TemplateVars tmplVars;
    public final String threadId;
    public final ConcurrentHashMap<String, Thread> threads;

    public Env() {
        this(new TemplateVars(), "", new ConcurrentHashMap<>());
    }

    public Env spawn(String childName) {
        return new Env(this.tmplVars.copy(), (threadId.equals("")) ? childName : threadId + "." + childName, threads);
    }

    //--------------------------------------------------------
    // Private
    //--------------------------------------------------------

    private Env(TemplateVars tmplVars, String threadId, ConcurrentHashMap<String, Thread> threads) {
        this.tmplVars = tmplVars;
        this.threadId = threadId;
        this.threads = threads;
    }
}
