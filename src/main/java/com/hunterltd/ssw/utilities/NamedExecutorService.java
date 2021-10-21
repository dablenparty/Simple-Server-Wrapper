package com.hunterltd.ssw.utilities;

import java.util.concurrent.ExecutorService;

/**
 * An ExecutorService with a {@code name} string
 */
public record NamedExecutorService(String name, ExecutorService executorService) {
}
