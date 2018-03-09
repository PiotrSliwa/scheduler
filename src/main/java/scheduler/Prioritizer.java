package scheduler;

import lombok.AllArgsConstructor;
import lombok.val;

import java.util.*;
import java.util.stream.Collectors;

public class Prioritizer {

    @AllArgsConstructor
    private static class Node {
        Item item;
        int position;
    }

    public Collection<Collection<Item>> prioritize(Collection<Item> items) {
        val positions = initPositionMap(items);
        for (Item item : items)
            for (Item dependency : item.getDependencies())
                putInFront(positions, dependency, item);
        return collect(positions);
    }

    private static Map<String, Node> initPositionMap(Collection<Item> items) {
        Map<String, Node> result = new HashMap<>();
        for (Item item : items)
            result.put(item.getId(), new Node(item, 0));
        return result;
    }

    private static void putInFront(Map<String, Node> positions, Item predecessor, Item successor) {
        val predecessorNode = positions.get(predecessor.getId());
        val successorNode = positions.get(successor.getId());
        val positionDiff = successorNode.position - predecessorNode.position;
        if (positionDiff <= 0) {
            move(positions, predecessor, positionDiff - 1);
            alignAncestors(positions, predecessor);
        }
    }

    private static void move(Map<String, Node> positions, Item item, int offset) {
        Stack<Item> stack = new Stack<>();
        stack.push(item);
        while (!stack.isEmpty()) {
            Item current = stack.pop();
            positions.get(current.getId()).position += offset;
            stack.addAll(current.getChildren());
            stack.addAll(current.getDependencies());
        }
    }

    private static void alignAncestors(Map<String, Node> positions, Item item) {
        val itemNode = positions.get(item.getId());
        Item ancestor = item.getParent();
        while (ancestor != null) {
            val ancestorNode = positions.get(ancestor.getId());
            val positionDiff = itemNode.position - ancestorNode.position;
            if (positionDiff != 0)
                expand(positions, ancestorNode.item, positionDiff);
            ancestor = ancestor.getParent();
        }
    }

    private static void expand(Map<String, Node> positions, Item item, int offset) {
        for (Item dependency : item.getDependencies())
            move(positions, dependency, offset);
    }

    private static Collection<Collection<Item>> collect(Map<String, Node> positions) {
        Map<Integer, Collection<Item>> result = new TreeMap<>();
        for (Node node : positions.values())
            result
                    .computeIfAbsent(node.position, k -> new ArrayList<>())
                    .add(node.item);
        return result.values();
    }

}
