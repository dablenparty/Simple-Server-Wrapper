package com.hunterltd.ssw.util.serial;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class GsonUtils {
    public static final ExclusionStrategy GSON_EXCLUDE_STRATEGY = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
            return fieldAttributes.getAnnotation(GsonExclude.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> aClass) {
            return false;
        }
    };
}
