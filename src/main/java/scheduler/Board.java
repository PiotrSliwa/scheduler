package scheduler;

import javafx.util.Pair;
import lombok.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class Board {

    @Getter
    @ToString
    public static class Allocation {
        Item item;
        float todo;
        List<ProjectResource> assignedResources;

        private Allocation(Item item) {
            this.item = item;
            todo = item.getParameters().getSize();
            assignedResources = new ArrayList<>();
        }
    }

    public static class InvalidDeltaException extends RuntimeException {
        public InvalidDeltaException(float delta) {
            super(Float.toString(delta));
        }
    }

    public static class InvalidAllocationException extends RuntimeException {
        public InvalidAllocationException(Allocation allocation) {
            super(allocation.toString());
        }
    }

    public static class UnknownResourceException extends RuntimeException {
        public UnknownResourceException(ProjectResource resource) {
            super(resource.toString());
        }
    }

    public static class ResourceAlreadyAssignedException extends RuntimeException {
        public ResourceAlreadyAssignedException(ProjectResource resource) {
            super(resource.toString());
        }
    }

    public static class OverflowingAllocationException extends RuntimeException {
        public OverflowingAllocationException(Allocation allocation, ProjectResource resource) {
            super(String.format("%s <= %s", allocation.toString(), resource.toString()));
        }
    }

    private Map<ProjectResource, Optional<Allocation>> occupations;
    private Deque<Allocation> todo = new ArrayDeque<>();
    private List<Allocation> ongoing = new ArrayList<>();
    private List<Allocation> done = new ArrayList<>();
    private float time = 0.0f;

    public Board(Collection<Item> items, Collection<ProjectResource> resources) {
        this.todo.addAll(items.stream().map(Allocation::new).collect(Collectors.toList()));
        this.occupations = resources.stream().collect(Collectors.toMap(Function.identity(), r -> Optional.empty()));
    }

    public List<Allocation> getIndependentTodo() {
        return todo.stream()
                .filter(this::isIndependent)
                .collect(Collectors.toList());
    }

    private boolean isIndependent(Allocation allocation) {
        return todo.stream().noneMatch(a -> allocation.item.isDependentFrom(a.item))
            && ongoing.stream().noneMatch(a -> allocation.item.isDependentFrom(a.item));
    }

    public List<Allocation> getSpareOngoing() {
        return ongoing.stream()
                .filter(a -> a.assignedResources.size() < a.item.getParameters().getThreads())
                .collect(Collectors.toList());
    }

    public List<ProjectResource> getSortedFreeResources(Item item, float delta) {
        val result = occupations.entrySet().stream()
                .filter(e -> !e.getValue().isPresent())
                .map(e -> new Pair<>(e.getKey(), e.getKey().getCapacity(item, time, delta)))
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toList());
        Collections.sort(result, (a, b) -> Float.compare(b.getValue(), a.getValue()));
        return result.stream()
                .map(Pair::getKey)
                .collect(Collectors.toList());
    }

    public void allocate(Allocation allocation, ProjectResource resource) {
        if (!occupations.containsKey(resource))
            throw new UnknownResourceException(resource);
        if (allocation.assignedResources.size() >= allocation.item.getParameters().getThreads())
            throw new OverflowingAllocationException(allocation, resource);
        if (todo.remove(allocation))
            ongoing.add(allocation);
        else if (!ongoing.contains(allocation))
            throw new InvalidAllocationException(allocation);
        else if (allocation.assignedResources.contains(resource))
            throw new ResourceAlreadyAssignedException(resource);
        allocation.assignedResources.add(resource);
        occupations.put(resource, Optional.of(allocation));
    }

    public void deallocate(Allocation allocation, ProjectResource resource) {
        if (!occupations.containsKey(resource))
            throw new UnknownResourceException(resource);
        if (!ongoing.contains(allocation))
            throw new InvalidAllocationException(allocation);
        allocation.assignedResources.remove(resource);
        occupations.put(resource, Optional.empty());
        if (allocation.assignedResources.isEmpty()) {
            ongoing.remove(allocation);
            todo.addFirst(allocation);
        }
    }

    public List<WorkPackage> increaseTime(float delta) {
        if (delta <= 0)
            throw new InvalidDeltaException(delta);
        List<WorkPackage> result = new ArrayList<>();
        val it = ongoing.listIterator();
        while (it.hasNext()) {
            val allocation = it.next();
            val workPackages = getPackagesForAssignedResources(allocation, delta);
            allocation.todo -= sumWorkDone(workPackages);
            result.addAll(workPackages);
            if (allocation.todo <= 0) {
                it.remove();
                done.add(allocation);
                for (val resource : allocation.assignedResources)
                    occupations.put(resource, Optional.empty());
                allocation.assignedResources.clear();
            }
        }
        time += delta;
        return result;
    }

    private List<WorkPackage> getPackagesForAssignedResources(Allocation allocation, float delta) {
        return allocation.assignedResources.stream()
                .map(r -> new WorkPackage(r, allocation.item, r.getCapacity(allocation.item, time, delta)))
                .collect(Collectors.toList());
    }

    private static float sumWorkDone(List<WorkPackage> workPackages) {
        return (float) workPackages.stream().mapToDouble(WorkPackage::getWorkDone).sum();
    }

}
