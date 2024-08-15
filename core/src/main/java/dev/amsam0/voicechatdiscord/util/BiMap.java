package dev.amsam0.voicechatdiscord.util;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class BiMap<K, V> extends HashMap<K, V> {
    public @Nullable K getKey(V v) {
        return this.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == v)
                .findFirst()
                .map(Entry::getKey)
                .orElse(null);
    }
}
