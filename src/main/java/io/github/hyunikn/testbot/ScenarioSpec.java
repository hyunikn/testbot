package io.github.hyunikn.testbot;

import java.util.Map;
import java.util.Arrays;
import java.util.TreeMap;

class ScenarioSpec {
    String relDir;
    String name;
    Map<String, String> arguments;

    ScenarioSpec(String relDir, String name, Map<String, String> arguments) {
        this.relDir = relDir;
        this.name = name;
        this.arguments = arguments;
    }

    String relPath() {
        return relDir.length() == 0 ? name : relDir + "/" + name;
    }

    //--------------------------------------------------------------------
    // Static
    //--------------------------------------------------------------------

    static ScenarioSpec parseScenario(String arg) {
        String[] split = arg.split(",");
        if (split.length == 1 || split.length == 2) {

            String scenarioPath = split[0];  // path relative to the scenarios-dir
            int errCode = Misc.Path.checkRelativePath(scenarioPath);
            switch (errCode) {
                case Misc.Path.CRP_ERROR_EMPTY:
                    throw new Error("empty scenario path in " + arg);
                case Misc.Path.CRP_ERROR_SPACE:
                    throw new Error("scenario path '" + scenarioPath + "' contains a space character");
                case Misc.Path.CRP_ERROR_TWO_SLASHES:
                    throw new Error("scenario path '" + scenarioPath + "' contains two consecutive slashes");
                case Misc.Path.CRP_ERROR_LEADING_SLASHES:
                    throw new Error("scenario path '" + scenarioPath + "' starts with a slash");
                case Misc.Path.CRP_ERROR_TRAILING_SLASHES:
                    throw new Error("scenario path '" + scenarioPath + "' ends with a slash");
            }

            // scenario name
            String [] segments = scenarioPath.split("/");
            assert segments.length > 0;
            String name = segments[segments.length - 1];     // the last path segment is the scenario name
            String [] subarr = Arrays.<String>copyOf(segments, segments.length - 1);
            String relDir = String.join("/", subarr);

            // scenario arguments
            Map<String, String> arguments = null;
            if (split.length == 2) {
                String scenarioArgs = split[1].trim();
                if (scenarioArgs.length() > 0) {
                    arguments = new TreeMap<>();
                    String[] argsSplit = scenarioArgs.split(":");
                    for (String a: argsSplit) {
                        String[] keyAndVal = a.split("=");
                        if (keyAndVal.length != 2) {
                            throw new Error("invalid form of a scenario argument " + a);
                        }

                        if (arguments.put(keyAndVal[0], keyAndVal[1]) != null) {
                            throw new Error("duplicate scenario arguments for key " + keyAndVal[0]);
                        }

                    }
                }
            }
            // scenario options
            /*
            boolean deferRevert = DEFAULT_SCENARIO_DEFER_REVERT;
            for (int j = 2; j < split.length; j++) {
                String opt = split[j];
                if (opt.equals(OPTION_STR_SCENARIO_DEFER_REVERT)) {
                    deferRevert = true;
                } else {
                    throw new Error("illegal option " + opt + " for scenario " + name);
                }
            }
             */

            return new ScenarioSpec(relDir, name, arguments);
        } else {
            throw new Error("illegal format of scenario specification: " + arg);
        }
    }

}

