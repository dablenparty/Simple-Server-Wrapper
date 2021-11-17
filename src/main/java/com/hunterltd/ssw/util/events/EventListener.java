package com.hunterltd.ssw.util.events;

import java.util.Objects;

public class EventListener {
    private final EventCallback callback;
    private final String eventName;
    private final boolean once;

    EventListener(String eventName, EventCallback callback) {
        this(eventName, callback, false);
    }

    public EventListener(String eventName, EventCallback callback, boolean once) {
        this.eventName = eventName;
        this.callback = callback;
        this.once = once;
    }

    public EventCallback getCallback() {
        return callback;
    }

    public String getEventName() {
        return eventName;
    }

    public boolean isOnce() {
        return once;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof EventListener eventListener)) {
            return false;
        } else {
            return this.callback.equals(eventListener.callback) && this.eventName.equals(eventListener.eventName);
        }
    }

    public int hashCode() {
        return Objects.hash(callback, eventName);
    }
}
