package com.hunterltd.ssw.util;

import java.util.ArrayList;

public class FixedSizeStack<E> {
    private final ArrayList<E> elements;

    public FixedSizeStack(int size) {
        elements = new ArrayList<>(size + 1);
    }
}
