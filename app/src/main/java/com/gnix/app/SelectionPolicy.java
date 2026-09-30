package com.gnix.app;
import java.util.*;

/** Never keep a selected publisher hidden behind a different topic. */
public final class SelectionPolicy {
    public static Set<String> reconcile(List<Source> sources, Set<String> selected, Set<String> topics, boolean seedIfEmpty) {
        Set<String> result = new LinkedHashSet<>();
        for (Source source : sources) if (topics.contains(source.category) && selected.contains(source.id)) result.add(source.id);
        if (result.isEmpty() && seedIfEmpty) {
            for (String topic : topics) {
                for (Source source : sources) if (source.category.equals(topic)) { result.add(source.id); break; }
            }
        }
        return result;
    }
}
