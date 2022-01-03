package com.hunterltd.ssw.util;

import java.util.ArrayList;
import java.util.EmptyStackException;

public class FixedSizeStack<E> {
    private final ArrayList<E> elements;
    private final int maxSize;

    public FixedSizeStack(int maxSize) {
        this.maxSize = maxSize;
        elements = new ArrayList<>(maxSize + 1);
    }

    public void push(E item) {
        if (elements.size() == maxSize)
            elements.remove(0);
        elements.add(item);
    }

    public E pop() {
        if (elements.isEmpty())
            throw new EmptyStackException();
        return elements.remove(elements.size() - 1);
    }
}
