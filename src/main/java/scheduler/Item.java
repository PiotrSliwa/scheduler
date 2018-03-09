package scheduler;

import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

@Getter
@ToString(exclude = {"parent", "dependencies", "impacted"})
@EqualsAndHashCode(of = {"id"})
public class Item implements Identifiable {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Parameters {
        private String name;
        private Float size;
        private Integer threads;
    }

    private String id;
    private Parameters parameters;
    private Item parent;
    private List<Item> dependencies = new ArrayList<>();
    private List<Item> children = new ArrayList<>();
    private List<Item> impacted = new ArrayList<>();

    public static class CyclicDependencyException extends RuntimeException {
        CyclicDependencyException(Item item, Item dependency) {
            super(String.format("cycle found when adding %s as dependency to %s", dependency, item));
        }
    }

    public static class GenealogyException extends RuntimeException {
        GenealogyException(Item parent, Item child) {
            super(String.format("cycle found when adding %s as a child to %s", child, parent));
        }
    }

    public static class DependentFromPredecessorException extends RuntimeException {
        DependentFromPredecessorException(Item item, Item predecessor) {
            super(String.format("%s is dependent from its predecessor %s", item, predecessor));
        }
    }

    public static class DependentFromSuccessorException extends RuntimeException {
        DependentFromSuccessorException(Item item, Item successor) {
            super(String.format("%s is dependent from its successor %s", item, successor));
        }
    }

    public static class DependencyCannotBePredecessorException extends RuntimeException {
        DependencyCannotBePredecessorException(Item item, Item predecessor) {
            super(String.format("%s is predecessor of %s", predecessor, item));
        }
    }

    public static class PredecessorAlreadySizedException extends RuntimeException {
        PredecessorAlreadySizedException(Item item, Item predecessor) {
            super(String.format("predecessor %s of item %s already has non-empty size", predecessor, item));
        }
    }

    public Item(String id) {
        this(id, new Parameters());
    }

    public Item(String id, Parameters parameters) {
        this.id = id;
        this.parameters = parameters;
    }

    public void addDependency(Item dependency) {
        assertNoCyclicDependencies(dependency);
        assertIsNotSuccessor(dependency);
        assertNoPredecessorEqualsDependency(dependency);
        dependencies.add(dependency);
        dependency.impacted.add(this);
    }

    public boolean isDependentFrom(Item item) {
        Stack<Item> stack = new Stack<>();
        stack.addAll(this.dependencies);
        while (!stack.isEmpty()) {
            Item current = stack.pop();
            if (current.equals(item))
                return true;
            stack.addAll(current.dependencies);
            stack.addAll(current.children);
        }
        return false;
    }

    public Float getTotalDependentSize() {
        float result = 0.0f;
        Stack<Item> stack = new Stack<>();
        stack.addAll(this.impacted);
        while (!stack.isEmpty()) {
            Item current = stack.pop();
            val size = current.parameters.size;
            if (size != null)
                result += size;
            stack.addAll(current.impacted);
            stack.addAll(current.children);
        }
        return result;
    }

    private void assertNoPredecessorEqualsDependency(Item dependency) {
        assertNoEqualFound(
                Collections.singletonList(parent),
                dependency,
                item -> item == null ? Collections.emptyList() : Collections.singletonList(item.parent),
                item -> new DependencyCannotBePredecessorException(item, dependency));
    }

    private void assertNoCyclicDependencies(Item dependency) {
        assertNoEqualFound(
                Collections.singletonList(dependency),
                this,
                item -> item.dependencies,
                item -> new CyclicDependencyException(item, dependency));
    }

    private void assertIsNotSuccessor(Item dependency) {
        assertNoEqualFound(
                children,
                dependency,
                item -> item.children,
                item -> new DependentFromSuccessorException(item, dependency));
    }

    public void addChild(Item child) {
        assertNoCylicGenealogy(child);
        assertNotFoundInDependencies(child);
        assertNoOverridingSize(child);
        child.parent = this;
        children.add(child);
    }

    private void assertNoOverridingSize(Item child) {
        if (child.parameters.getSize() == null)
            return;
        for (Item current = this; current != null; current = current.parent)
            if (current.parameters.getSize() != null)
                throw new PredecessorAlreadySizedException(this, current);
    }

    private void assertNotFoundInDependencies(Item child) {
        assertNoEqualFound(
                dependencies,
                child,
                item -> item.dependencies,
                item -> new DependentFromPredecessorException(child, item));
        assertNoEqualFound(
                child.dependencies,
                this,
                item -> item.dependencies,
                item -> new DependentFromSuccessorException(item, child));
    }

    private void assertNoCylicGenealogy(Item child) {
        assertNoEqualFound(
                Collections.singletonList(child),
                this,
                item -> item.children,
                item -> new GenealogyException(item, child));
    }

    private void assertNoEqualFound(List<Item> subjects,
                                    Item equalToItem,
                                    Function<Item, List<Item>> nextSubjectsGetter,
                                    Function<Item, RuntimeException> exceptionProducer) {
        Stack<Item> stack = new Stack<>();
        stack.addAll(subjects);
        while (!stack.empty()) {
            Item item = stack.pop();
            if (equalToItem.equals(item))
                throw exceptionProducer.apply(item);
            stack.addAll(nextSubjectsGetter.apply(item));
        }
    }
}
