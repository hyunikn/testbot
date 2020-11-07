package io.github.hyunikn.testbot;

import java.util.Map;
import java.util.TreeMap;

public class FuncRepo {
    public interface Func { String invoke(String arguments); }

    public static void registerFunc(String funcName, Func func) {
        if (funcs.containsKey(funcName)) {
            throw new Error("a function named '" + funcName + "' has already been registered");
        }

        funcs.put(funcName, func);
    }

    //-----------------------------------------------------------------------------
    // Package
    //-----------------------------------------------------------------------------

    static Func getFunc(String funcName) {
        return funcs.get(funcName);
    }

    public static String invoke(String funcCall) {  // TODO: erase public
        int funcNameBound = funcCall.indexOf("(");
        int funcArgBound = funcCall.indexOf(")");

        assert funcNameBound < funcArgBound;
        assert funcCall.startsWith("!") && funcNameBound > 1 && funcCall.endsWith(")");

        String funcName = funcCall.substring(1, funcNameBound);
        String funcArg = funcCall.substring(funcNameBound + 1, funcArgBound);

        Func func = funcs.get(funcName);
        if (func == null) {
            throw new Error("unknown function " + funcName);
        }

        return func.invoke(funcArg.trim());
    }

    //-----------------------------------------------------------------------------
    // Private
    //-----------------------------------------------------------------------------

    private static final Map<String, Func> funcs = new TreeMap<>();
}
