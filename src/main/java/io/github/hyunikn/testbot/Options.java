package io.github.hyunikn.testbot;

import org.json.JSONObject;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.LinkedList;

public class Options {

    public String confFile;
    public JSONObject conf;
    public boolean noRevert;
    public boolean interactive;
    public boolean interlude;
    public boolean checkOnly;
    public int interval;
    public String logLevel;
    public String toolLogLevel;
    public List<ScenarioSpec> scenarioSpecs;

    static Options parseArguments(String[] argv) throws FileNotFoundException, IOException, ParseError  {
        return new Options(argv);
    }

    static void printUsage() {
        System.out.println(
            "usage: run.sh [--no-revert|-n] [--interactive|-i] [--interlude|-l] [--check-only|-k]\n" +
            "              [(--interval|-t)=<seconds between actions, default=0>] \n" +
            "              [(--config|-c)=<config file path, default=./conf.json>] \n" +
            "              [(--log-level|-g)=<all|trace|debug|info|warn|error|fatal|off, default=info>] \n" +
            "              [(--tool-log-level|-v)=<all|trace|debug|info|warn|error|fatal|off, default=error>] \n" +
            "              <scenario name> [ <scenario name> ... ]");
    }

    static class ParseError extends Exception {
        ParseError(String msg) {
            super(msg);
        }
    }

    //------------------------------------------------------
    // Private
    //------------------------------------------------------

    private static final String DEFAULT_CONF_FILE           = System.getProperty("user.dir") + "/" + "conf.json";
    private static final boolean DEFAULT_NO_REVERT          = false;
    private static final boolean DEFAULT_INTERACTIVE        = false;
    private static final boolean DEFAULT_INTERLUDE          = false;
    private static final boolean DEFAULT_CHECK_ONLY         = false;
    private static final int DEFAULT_INTERVAL               = 0;
    private static final String DEFAULT_LOG_LEVEL           = "INFO";
    private static final String DEFAULT_TOOL_LOG_LEVEL      = "ERROR";

    private static final String OPT_STR_CONFIG               = "--config=";
    private static final String OPT_STR_SHORT_CONFIG         = "-c=";
    private static final String OPT_STR_NO_REVERT            = "--no-revert";
    private static final String OPT_STR_SHORT_NO_REVERT      = "-n";
    private static final String OPT_STR_INTERACTIVE          = "--interactive";
    private static final String OPT_STR_SHORT_INTERACTIVE    = "-i";
    private static final String OPT_STR_INTERLUDE            = "--interlude";
    private static final String OPT_STR_SHORT_INTERLUDE      = "-l";
    private static final String OPT_STR_CHECK_ONLY           = "--check-only";
    private static final String OPT_STR_SHORT_CHECK_ONLY     = "-k";
    private static final String OPT_STR_INTERVAL             = "--interval=";
    private static final String OPT_STR_SHORT_INTERVAL       = "-t=";
    private static final String OPT_STR_LOG_LEVEL            = "--log-level=";
    private static final String OPT_STR_SHORT_LOG_LEVEL      = "-g=";
    private static final String OPT_STR_TOOL_LOG_LEVEL       = "--tool-log-level=";
    private static final String OPT_STR_SHORT_TOOL_LOG_LEVEL = "-v=";

    private static final String[] validLogLevels =
        new String[] { "ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "OFF" };

    private Options(String[] argv) throws FileNotFoundException, IOException, ParseError {
        // Set default values for each options
        confFile = DEFAULT_CONF_FILE;
        noRevert = DEFAULT_NO_REVERT;
        interactive = DEFAULT_INTERACTIVE;
        interlude = DEFAULT_INTERLUDE;
        checkOnly = DEFAULT_CHECK_ONLY;
        interval = DEFAULT_INTERVAL;
        logLevel = DEFAULT_LOG_LEVEL;
        toolLogLevel = DEFAULT_TOOL_LOG_LEVEL;

        scenarioSpecs = new LinkedList<>();

        // parse options
        int i;
        int argvLen = argv.length;
        for (i = 0; i < argvLen; i++) {
            String arg = argv[i];
            if (arg.charAt(0) == '-') {
                if (arg.startsWith(OPT_STR_CONFIG) || arg.startsWith(OPT_STR_SHORT_CONFIG)) {
                    confFile = arg.substring(arg.indexOf("=") + 1);
                    if (confFile.charAt(0) != '/') {
                        confFile = System.getProperty("user.dir") + "/" + confFile;
                    }
                } else if (arg.equals(OPT_STR_NO_REVERT) || arg.equals(OPT_STR_SHORT_NO_REVERT)) {
                    noRevert = true;
                } else if (arg.equals(OPT_STR_INTERACTIVE) || arg.equals(OPT_STR_SHORT_INTERACTIVE)) {
                    interactive = true;
                } else if (arg.equals(OPT_STR_INTERLUDE) || arg.equals(OPT_STR_SHORT_INTERLUDE)) {
                    interlude = true;
                } else if (arg.equals(OPT_STR_CHECK_ONLY) || arg.equals(OPT_STR_SHORT_CHECK_ONLY)) {
                    checkOnly = true;
                } else if (arg.startsWith(OPT_STR_INTERVAL) || arg.startsWith(OPT_STR_SHORT_INTERVAL)) {
                    try {
                        interval = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
                    } catch (NumberFormatException e) {
                        throw new Error("invalid value to --interval option");
                    }
                } else if (arg.startsWith(OPT_STR_LOG_LEVEL) || arg.startsWith(OPT_STR_SHORT_LOG_LEVEL)) {
                    logLevel = arg.substring(arg.indexOf("=") + 1).toUpperCase();
                    int j = 0;
                    for ( ; j < validLogLevels.length; j++) {
                        if (logLevel.equals(validLogLevels[j])) {
                            break;
                        }
                    }
                    if (j == validLogLevels.length) {
                        throw new Error("invalid log level " + logLevel);
                    }
                } else if (arg.startsWith(OPT_STR_TOOL_LOG_LEVEL) || arg.startsWith(OPT_STR_SHORT_TOOL_LOG_LEVEL)) {
                    toolLogLevel = arg.substring(arg.indexOf("=") + 1).toUpperCase();
                    int j = 0;
                    for ( ; j < validLogLevels.length; j++) {
                        if (toolLogLevel.equals(validLogLevels[j])) {
                            break;
                        }
                    }
                    if (j == validLogLevels.length) {
                        throw new Error("invalid tool log level " + toolLogLevel);
                    }
                } else {
                    throw new ParseError("unknown option: " + arg);
                }
            } else {
                break;
            }
        }
        if (i == argvLen) {
            throw new ParseError("no scenarios given");
        }

        // read configure file
        conf = (JSONObject) Misc.readAndParseJSON(confFile, false);

        // parse scenario specs
        for ( ; i < argvLen; i++) {
            String arg = argv[i];
            scenarioSpecs.add(ScenarioSpec.parseScenario(arg));
        }
    }

}
