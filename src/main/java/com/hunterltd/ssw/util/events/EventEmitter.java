package com.hunterltd.ssw.util.events;

import java.util.*;

public class EventEmitter {
    private final HashMap<String, List<EventListener>> eventListenerMap = new HashMap<>();
    private final Queue<EventListener> markedForRemoval = new LinkedList<>();
    private int maxListeners = 10;

    /**
     * Gets the maximum number of listeners. This value can be changed with {@link EventEmitter#setMaxListeners}.
     *
     * @return Maximum number of listeners
     */
    public int getMaxListeners() {
        return maxListeners;
    }

    /**
     * Sets the maximum number of listeners allowed
     *
     * @param maxListeners Maximum number of listeners
     */
    public void setMaxListeners(int maxListeners) {
        this.maxListeners = maxListeners;
    }

    /**
     * Gets a list of event listeners for a given event name.
     *
     * @param event Event name
     * @return Unmodifiable List of listeners on event, or null if there are none.
     * @see EventListener
     */
    public List<EventListener> getListeners(String event) {
        List<EventListener> eventListeners = eventListenerMap.get(event);
        return eventListeners != null ? Collections.unmodifiableList(eventListeners) : null;
    }

    /**
     * Gets a set of event names.
     *
     * @return Unmodifiable Set of event names
     */
    public Set<String> getEventNames() {
        return Collections.unmodifiableSet(eventListenerMap.keySet());
    }

    /**
     * Gets the number of listeners currently registered for a given event name
     *
     * @param event Event name
     * @return Number of registered listeners
     */
    public int getListenerCount(String event) {
        List<EventListener> eventListeners = eventListenerMap.get(event);
        return eventListeners != null ? eventListeners.size() : 0;
    }

    /**
     * Emits an event to all listeners alongside any extra objects you want to pass to the listeners
     *
     * @param event Event name to emit
     * @param args  Objects to pass to listeners (can be empty)
     * @return Whether any listeners were notified
     */
    public boolean emit(String event, Object... args) {
        List<EventListener> callbacks = eventListenerMap.get(event);
        boolean eventHasListeners = callbacks != null && callbacks.size() != 0;
        if (eventHasListeners) {
            callbacks.forEach(listener -> {
                listener.getCallback().call(args);
                if (listener.isOnce())
                    markedForRemoval.add(listener);
            });
        }

        Iterator<EventListener> removalIterator = markedForRemoval.iterator();
        while (removalIterator.hasNext()) {
            EventListener listener = removalIterator.next();
            (eventListenerMap.get(event)).remove(listener);
            removalIterator.remove();
        }

        if (eventListenerMap.get(event).size() == 0)
            eventListenerMap.remove(event);

        return eventHasListeners;
    }

    /**
     * Removes a listener from an event.
     *
     * @param event    Event to remove listener from
     * @param callback Listener to remove
     * @return This emitter for method call chaining
     */
    public EventEmitter removeListener(String event, EventCallback callback) {
        EventListener eventListener = new EventListener(event, callback);
        (eventListenerMap.get(eventListener.getEventName())).remove(eventListener);
        return this;
    }

    /**
     * Removes a listener from an event.
     * <p>
     * Alias for {@link EventEmitter#removeListener(String, EventCallback)}.
     *
     * @param event    Event to remove listener from
     * @param callback Listener to remove
     * @return This emitter for method call chaining
     */
    public EventEmitter off(String event, EventCallback callback) {
        return removeListener(event, callback);
    }

    /**
     * Removes all event listeners from this emitter.
     *
     * @return This emitter for method call chaining
     */
    public EventEmitter removeAllListeners() {
        Set<String> keys = Set.copyOf(eventListenerMap.keySet());
        for (String key : keys)
            removeAllListeners(key);
        return this;
    }

    /**
     * Removes all listener from a given event.
     *
     * @param event Event to remove events from
     * @return This emitter for method call chaining
     */
    public EventEmitter removeAllListeners(String event) {
        eventListenerMap.remove(event);
        return this;
    }

    private void registerNewListener(EventListener eventListener) {
        registerNewListener(eventListener, false);
    }

    private void registerNewListener(EventListener eventListener, boolean prepend) {
        List<EventListener> eventListeners = eventListenerMap.get(eventListener.getEventName());
        if (eventListeners == null) {
            List<EventListener> newList = new ArrayList<>(1);
            newList.add(eventListener);
            eventListenerMap.put(eventListener.getEventName(), newList);
        } else {
            if (eventListeners.size() < maxListeners) {
                if (prepend)
                    eventListeners.add(0, eventListener);
                else if (!eventListeners.contains(eventListener))
                    eventListeners.add(eventListener);
            } else
                System.err.printf("WARNING: %d listeners detected for [%s] event on [%s]. " +
                                "Use the setMaxListeners() method to increase this limit%n",
                        eventListeners.size(),
                        eventListener.getEventName(),
                        getClass());
        }
    }

    /**
     * Registers a new listener for an event
     *
     * @param event    Event to listen for
     * @param callback Listener to be called when event is emitted
     * @return This emitter for method call chaining
     */
    public EventEmitter on(String event, EventCallback callback) {
        EventListener newEventListener = new EventListener(event, callback);
        registerNewListener(newEventListener);
        return this;
    }

    /**
     * Registers a new listener for an event
     * <p>
     * Alias for {@link EventEmitter#on(String, EventCallback)}.
     *
     * @param event    Event to listen for
     * @param callback Listener to be called when event is emitted
     */
    public void addListener(String event, EventCallback callback) {
        on(event, callback);
    }

    /**
     * Registers a one-time listener for an event. This means that the listener will be called once then removed from
     * the event.
     *
     * @param event    Event to listen for
     * @param callback Listener to be called when event is emitted
     * @return This emitter for method call chaining
     */
    public EventEmitter once(String event, EventCallback callback) {
        EventListener newEventListener = new EventListener(event, callback, true);
        registerNewListener(newEventListener);
        return this;
    }

    /**
     * Registers a new listener for an event to be called before all other listeners.
     *
     * @param event    Event to listen for
     * @param callback Listener to be called when event is emitted
     * @return This emitter for method call chaining
     */
    public EventEmitter prependListener(String event, EventCallback callback) {
        EventListener eventListener = new EventListener(event, callback);
        registerNewListener(eventListener, true);
        return this;
    }

    /**
     * Registers a new one-time listener for an event to be called before all other listeners.
     *
     * @param event    Event to listen for
     * @param callback Listener to be called when event is emitted
     * @return This emitter for method call chaining
     */
    public EventEmitter prependOnceListener(String event, EventCallback callback) {
        EventListener eventListener = new EventListener(event, callback, true);
        registerNewListener(eventListener, true);
        return this;
    }
}

