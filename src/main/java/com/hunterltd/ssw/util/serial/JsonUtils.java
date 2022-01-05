package com.hunterltd.ssw.util.serial;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;

public class JsonUtils {
    private static final ExclusionStrategy GSON_EXCLUDE_STRATEGY = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getAnnotation(GsonExclude.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
            return false;
        }
    };
    public static final GsonBuilder GSON_BUILDER = new GsonBuilder()
            .setExclusionStrategies(GSON_EXCLUDE_STRATEGY);
}
