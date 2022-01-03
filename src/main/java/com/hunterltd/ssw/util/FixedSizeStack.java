package com.hunterltd.ssw.util;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

public class FixedSizeStack<E> {
    private final List<E> elements;
    private final int maxSize;

    public FixedSizeStack(int maxSize) {
        this.maxSize = maxSize;
        elements = new ArrayList<>(maxSize);
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

    @Override
    public String toString() {
        return elements.toString();
    }
}
