package io.github.hyunikn.testbot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// log4j that we use
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class Main {
    public static final Logger logger = LogManager.getLogger("RUNTIME");

    public static volatile boolean shutdown = false;
    public static volatile Options options = null;
    public static volatile Map<String, Inspection> inspectionMap = null;

    public static void main(String[] argv)
        throws IOException, FileNotFoundException, UnsupportedEncodingException, InstantiationException,
                          IllegalAccessException, InvocationTargetException {

        if (argv.length == 0) {
            Options.printUsage();
            return;
        }

        // parse command line arguments
        try {
            options = Options.parseArguments(argv);
        } catch (Options.ParseError e) {
            Main.logger.error(Misc.getIndent(0) + "error while parsing options: " + e.getMessage());
            Options.printUsage();
            System.exit(10);
        }

        JSONObject inspectionsOnErrorConf = options.conf.optJSONObject("inspections-on-error");
        if (inspectionsOnErrorConf == null) {
            Main.logger.warn(Misc.getIndent(0) + "conf file does not have a 'inspections-on-error' value");
            inspectionsOnErrorConf = new JSONObject();
        }
        inspectionMap = getInspectionMap(inspectionsOnErrorConf);

        // initialize log4j that expect uses (version 1.x)
        org.apache.log4j.BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.toLevel(options.toolLogLevel));

        //
        Level l = Level.toLevel(options.logLevel);
        System.out.println("### log level = " + l);
        Configurator.setRootLevel(l);

        // read and check scenarios
        List<Scenario> scenarios = buildScenarios(options);
        if (options.checkOnly) {
            System.exit(0);
        }

        // run scenarios
        run(scenarios, options);
    }

    //-------------------------------------------------------------------------
    // Package
    //-------------------------------------------------------------------------


    //-------------------------------------------------------------------------
    // Private
    //-------------------------------------------------------------------------

    private static Map<String, Inspection> getInspectionMap(JSONObject inspectionsOnErrorConf) {
        Map<String, Inspection> map = new TreeMap<>();

        // TODO: add inspections as plug-ins

        return map;
    }

    private static List<Scenario> buildScenarios(Options options)
        throws IOException, UnsupportedEncodingException, InstantiationException, IllegalAccessException,
                          InvocationTargetException {

        Main.logger.info(Misc.getIndent(0) + "### Checking scenarios");

        List<ScenarioSpec> scenarioSpecs = options.scenarioSpecs;

        List<Scenario> list = new LinkedList<>();
        Map<String, ScenarioEntryList> builtEntryLists = new TreeMap<>();
        Deque<String> scenarioStack = new LinkedList<>();

        Env env = new Env();
        for (ScenarioSpec spec: scenarioSpecs) {
            Main.logger.info(Misc.getIndent(1) + "<Scenario " + spec.relPath() + ">");
            Scenario s = Scenario.compile(env, 1, options, builtEntryLists, scenarioStack, spec);
            assert env.tmplVars.isEmpty();
            list.add(s);
        }

        Main.logger.info(Misc.getIndent(0) + "### Checking scenarios done - OK");

        return list;
    }

    static private void run(List<Scenario> scenarios, Options options) {
        String err;

        Deque<Action> actionStack = options.noRevert ? null : new ConcurrentLinkedDeque<>();

        // shutdown hook
        Thread mainThread = Thread.currentThread();
        Thread shutdownHook = new Thread(){
            @Override
            public void run() {
                Main.logger.info(Misc.getIndent(0) + "### Shutting down !!!");
                Main.shutdown = true;
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // run
        Main.logger.info(Misc.getIndent(0) + "### Running scenarios");
        int exitCode = 0;
        Env env = new Env();
        for (Scenario s: scenarios) {
            Main.logger.info(Misc.getIndent(1) + "<Scenario " + s.entryName + ">");
            err = s.run(env, options, actionStack, 1);
            assert env.tmplVars.isEmpty();
            if (err != null) {
                exitCode = 1;
                break;
            }
        }
        Main.logger.info(Misc.getIndent(0) + "### Running scenarios done");
        if (!shutdown) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        // revert
        if (options.noRevert) {
            Main.logger.info(Misc.getIndent(0) + "### Skipping reversion as indicated by the --no-revert option");
        } else {
            boolean revert = true;
            if (options.interlude) {
                UserChoice uc = getUserChoice();
                if (uc == UserChoice.Go) {
                    // do nothing
                } else if (uc == UserChoice.Quit) {
                    revert = false;
                } else if (uc == UserChoice.Error) {
                    Main.logger.error(Misc.getIndent(0) +
                            "IO Error while getting user input, but just ignoring that error");
                }
            }

            if (revert) {
                if (actionStack.isEmpty()) {
                    Main.logger.info(Misc.getIndent(0) + "### No actions to revert");
                } else {
                    Main.logger.info(Misc.getIndent(0) + "### Reverting actions");
                    while (!actionStack.isEmpty()) {
                        Action a = actionStack.pop();
                        try {
                            err = a.revert(options);
                            if (err != null) {
                                throw new Error("error while reverting");
                            }
                        } catch (Throwable e) {
                            exitCode += 2;
                            throw e;
                        }
                    }
                    Main.logger.info(Misc.getIndent(0) + "### Done with reverting actions");
                }
            }
        }

        if (exitCode == 0) {
            Main.logger.info(Misc.getIndent(0) + "### Test done SUCCESSFULLY");
        } else {
            Main.logger.info(Misc.getIndent(0) + "### Test done with ERROR");
            if (exitCode > 1) {
                Main.logger.info(Misc.getIndent(0) +
                        "### Error occured during the reversion phase. Manual clean-up required.");
            }
        }

        if (!shutdown) {
            System.exit(exitCode);  // some libraries may spawn their own threads, which prevents the program to exit.
                                    // so, this line.
                                    // exit code = 0 if successful
                                    //             1 if error during the run phase
                                    //             2 if error during the reversion phase
                                    //             3 if error during both phases
        }
    }

    private enum UserChoice {
        Go,
        Quit,
        Error   // indeed, not a user choice
    }

    private static UserChoice getUserChoice() {
        for (;;) {
            System.out.println("");
            System.out.print("# Hit Enter to start reversion (or Q to quit): ");

            int n = 0, key = 0;
            try {
                key = System.in.read();
                n = System.in.available();
                System.in.skip(n);
            } catch (IOException e) {
                Main.logger.error(Misc.getIndent(0) + "", e);
                return UserChoice.Error;
            }
            if (n > 1) {
                Main.logger.error(Misc.getIndent(0) + "# Unrecognized answer");
                continue;
            }

            switch (key) {
            case 10:    // Enter
                return UserChoice.Go;
            case 113: case 81:  // q or Q
                return UserChoice.Quit;
            default:
                Main.logger.error(Misc.getIndent(0) + "# Unrecognized key " + new String(new byte[]{ (byte) key }));
            }
        }
    }
}
