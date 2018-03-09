package scheduler.infrastructure;

import lombok.AllArgsConstructor;
import scheduler.Item;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ItemBuilder {

    @AllArgsConstructor
    public static class Placeholder {
        String id;
        String name;
        Float size;
        Integer threads;
        List<String> childIds;
        List<String> dependencyIds;

        public Placeholder(String id, String name) {
            this(id, name, null, null);
        }

        public Placeholder(String id, String name, Float size, Integer threads) {
            this(id, name, size, threads, new ArrayList<>(), new ArrayList<>());
        }
    }

    public static class IdAlreadyAddedException extends RuntimeException {
        IdAlreadyAddedException(String id) {
            super(id);
        }
    }

    public static class NonExistingIdAsChildException extends RuntimeException {
        NonExistingIdAsChildException(String id) {
            super(id);
        }
    }

    public static class NonExistingIdAsDependencyException extends RuntimeException {
        NonExistingIdAsDependencyException(String id) {
            super(id);
        }
    }

    public static class InvalidIdException extends RuntimeException {
        InvalidIdException(String id) {
            super(id);
        }
    }

    private final Map<String, Placeholder> placeholders = new HashMap<>();

    public void add(Placeholder placeholder) {
        assertValidPlaceholder(placeholder);
        if (placeholders.containsKey(placeholder.id))
            throw new IdAlreadyAddedException(placeholder.id);
        placeholders.put(placeholder.id, placeholder);
    }

    private static void assertValidPlaceholder(Placeholder placeholder) {
        assertValidId(placeholder.id);
        placeholder.childIds.forEach(ItemBuilder::assertValidId);
        placeholder.dependencyIds.forEach(ItemBuilder::assertValidId);
    }

    private static void assertValidId(String id) {
        if (id == null || id.isEmpty())
            throw new InvalidIdException(id);
    }

    public Map<String, Item> build() {
        Map<String, Item> itemMap = placeholders.values().stream()
                .map(v -> new Item(v.id, new Item.Parameters(v.name, v.size, v.threads)))
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        for (Placeholder placeholder : placeholders.values()) {
            Item item = itemMap.get(placeholder.id);
            for (String childId : placeholder.childIds) {
                Item child = itemMap.get(childId);
                if (child == null)
                    throw new NonExistingIdAsChildException(childId);
                item.addChild(child);
            }
            for (String depId : placeholder.dependencyIds) {
                Item dependency = itemMap.get(depId);
                if (dependency == null)
                    throw new NonExistingIdAsDependencyException(depId);
                item.addDependency(dependency);
            }
        }
        return itemMap;
    }
}
