package org.server.pixelogueCook.fishing.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WeightedTable<T> {
    private static final Random RND = new Random();
    private final List<Entry<T>> entries = new ArrayList<>();
    private double total = 0;

    public WeightedTable<T> add(double weight, T value){
        if (weight <= 0) return this;
        total += weight;
        entries.add(new Entry<>(weight, value));
        return this;
    }

    public T pick(){
        if (entries.isEmpty()) return null;
        double r = RND.nextDouble() * total;
        double acc = 0;
        for (Entry<T> e : entries) {
            acc += e.w;
            if (r <= acc) return e.v;
        }
        return entries.getLast().v;
    }

    private record Entry<T>(double w, T v){}
}