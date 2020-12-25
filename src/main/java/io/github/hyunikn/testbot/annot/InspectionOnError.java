package io.github.hyunikn.testbot.annot;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InspectionOnError {
    String[] value();
}
