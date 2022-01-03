package com.hunterltd.ssw.util;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Optional;

/**
 * A bare-bones stack implementation that holds a fixed number of elements. An {@link ArrayList} is used internally, so
 * this class is not inherently thread-safe. It is still, by definition, a FILO (First In Last Out) data structure, but
 * it works by removing the element at the bottom of the stack if the max size has been reached when adding a new
 * element.
 * This does not extend the built-in {@link java.util.Stack} class because that implementation is based upon
 * {@code Vector}'s, which are (by design) thread-safe and have too many functions to override for
 * adding/removing/inserting elements if I wish to keep my sanity.
 *
 * @param <E> Type of elements
 */
public class FixedSizeStack<E> {
    private final List<StackElement<E>> elements;
    private final int maxSize;

    /**
     * Constructs a new {@code FixedSizeStack} with a maximum size of {@code maxSize} elements
     *
     * @param maxSize Maximum number of elements
     */
    public FixedSizeStack(int maxSize) {
        this.maxSize = maxSize;
        elements = new ArrayList<>(maxSize);
    }

    /**
     * Returns the number of elements in the stack
     *
     * @return Number of elements in this stack
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns the maximum number of elements this stack can hold
     *
     * @return Maximum number of elements
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Pushes an element to the top of the stack
     *
     * @param item Element to add
     */
    public void push(E item) {
        if (elements.size() == maxSize) elements.remove(0);
        StackElement<E> newElement = new StackElement<>(item);
        StackElement<E> topElement = peekElement();
        // link new element to top of stack
        newElement.setPrevious(topElement);
        topElement.setNext(newElement);
        elements.add(newElement);
    }

    /**
     * Pops an element off the top of the stack
     *
     * @return Element on top of the stack
     */
    public E pop() {
        if (elements.isEmpty()) throw new EmptyStackException();
        return elements.remove(elements.size() - 1).getValue();
    }

    /**
     * Returns, but does not remove, the element at the top of the stack
     *
     * @return Element on top of the stack
     */
    public E peek() {
        if (elements.isEmpty()) throw new EmptyStackException();
        return elements.get(elements.size() - 1).getValue();
    }

    /**
     * Returns, but does not remove, the {@code StackElement} wrapper around the value at the top of the stack
     *
     * @return {@code StackElement} at the top of the stack
     */
    public StackElement<E> peekElement() {
        if (elements.isEmpty()) throw new EmptyStackException();
        return elements.get(elements.size() - 1);
    }

    /**
     * Gets an item from the stack without removing it
     *
     * @param index index at which to access
     * @return item at {@code index}
     * @throws IndexOutOfBoundsException if the index is out of range {@code (index < 0 || index >= size())}
     */
    public E get(int index) {
        return elements.get(size() - index - 1).getValue();
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    public static class StackElement<E> {
        private final E value;
        private StackElement<E> previous = null;
        private StackElement<E> next = null;

        public StackElement(E value) {
            this.value = value;
        }

        public Optional<StackElement<E>> getPrevious() {
            return Optional.ofNullable(previous);
        }

        protected void setPrevious(StackElement<E> previous) {
            this.previous = previous;
        }

        public Optional<StackElement<E>> getNext() {
            return Optional.ofNullable(next);
        }

        protected void setNext(StackElement<E> next) {
            this.next = next;
        }

        public E getValue() {
            return value;
        }
    }
}
