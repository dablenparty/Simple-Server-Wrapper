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
        if (elements.size() == maxSize)
            elements.remove(0).getNext().ifPresent(element -> element.setNext(null));
        StackElement<E> newElement = new StackElement<>(item);
        try {
            StackElement<E> topElement = peekElement();
            // link new element to top of stack
            newElement.setNext(topElement);
            topElement.setPrevious(newElement);
        } catch (EmptyStackException ignored) {
        } finally {
            elements.add(newElement);
        }
    }

    /**
     * Pops an element off the top of the stack
     *
     * @return Element on top of the stack
     */
    public E pop() {
        if (elements.isEmpty()) throw new EmptyStackException();
        StackElement<E> poppedElement = elements.remove(elements.size() - 1);
        poppedElement.getPrevious().ifPresent(element -> element.setPrevious(null));
        return poppedElement.getValue();
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
     * @throws EmptyStackException if the stack is empty
     */
    public StackElement<E> peekElement() {
        if (elements.isEmpty()) throw new EmptyStackException();
        return elements.get(elements.size() - 1);
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    /**
     * Represents an element in the stack. This class also has {@code next} and {@code previous} fields for keeping
     * track of the order of elements in the stack. A {@code null} value in either of these fields indicates that this
     * element is either at the top or bottom of the stack.
     *
     * @param <E> Value type
     */
    public static class StackElement<E> {
        private final E value;
        private StackElement<E> previous = null;
        private StackElement<E> next = null;

        private StackElement(E value) {
            this.value = value;
        }

        /**
         * Gets the element above this one in the stack and wraps it in an {@link Optional}. If the {@code Optional} is
         * empty, this element is at the top of the stack
         *
         * @return element above this in the stack
         */
        public Optional<StackElement<E>> getPrevious() {
            return Optional.ofNullable(previous);
        }

        protected void setPrevious(StackElement<E> previous) {
            this.previous = previous;
        }

        /**
         * Gets the element below this one in the stack and wraps it in an {@link Optional}. If the {@code Optional} is
         * empty, this element is at the bottom of the stack
         *
         * @return element below this in the stack
         */
        public Optional<StackElement<E>> getNext() {
            return Optional.ofNullable(next);
        }

        protected void setNext(StackElement<E> next) {
            this.next = next;
        }

        /**
         * Returns the value of this element
         *
         * @return this elements value
         */
        public E getValue() {
            return value;
        }
    }
}
