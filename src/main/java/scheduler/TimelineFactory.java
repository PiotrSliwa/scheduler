package scheduler;

import lombok.Setter;
import lombok.val;

import java.util.*;
import java.util.stream.Collectors;

public class TimelineFactory {

    public static class ExceededMaxInactivityException extends RuntimeException {
    }

    private float resolution;
    private final Collection<ProjectResource> resources;
    @Setter private float maxInactivity = 100.0f;

    private class ActivityGuard {
        float inactivityTime = 0.0f;

        void update(List<WorkPackage> workPackages) {
            if (workPackages.isEmpty())
                inactivityTime += resolution;
            else
                inactivityTime = 0.0f;
            if (inactivityTime > maxInactivity)
                throw new ExceededMaxInactivityException();
        }
    }

    public TimelineFactory(float resolution, Collection<ProjectResource> resources) {
        this.resolution = resolution;
        this.resources = resources;
    }

    public List<List<WorkPackage>> create(Collection<Collection<Item>> prioritizedGroups) {
        List<List<WorkPackage>> result = new ArrayList<>();
        val board = new Board(createTodoList(prioritizedGroups), resources);
        val activityGuard = new ActivityGuard();
        while (!board.getTodo().isEmpty() || !board.getOngoing().isEmpty()) {
            deallocateRedundant(board);
            for (val allocation : sortByImpact(board.getIndependentTodo()))
                allocate(allocation, board);
            for (val allocation : sortByImpact(board.getSpareOngoing()))
                allocate(allocation, board);
            val workPackages = increaseTime(board);
            activityGuard.update(workPackages);
            result.add(workPackages);
        }
        return result;
    }

    private List<WorkPackage> increaseTime(Board board) {
        val workDone = board.increaseTime(resolution);
        return workDone.stream().filter(p -> p.getWorkDone() > 0.0f).collect(Collectors.toList());
    }

    private List<Board.Allocation> sortByImpact(List<Board.Allocation> allocations) {
        List<Board.Allocation> result = new ArrayList<>(allocations);
        result.sort((a, b) -> Float.compare(b.getItem().getTotalDependentSize(), a.getItem().getTotalDependentSize()));
        return result;
    }

    private static List<Item> createTodoList(Collection<Collection<Item>> prioritizedGroups) {
        return prioritizedGroups.stream()
                .flatMap(Collection::stream)
                .filter(i -> i.getParameters().getSize() != null && i.getParameters().getSize() > 0)
                .filter(i -> i.getParameters().getThreads() != null && i.getParameters().getThreads() > 0)
                .collect(Collectors.toList());
    }

    private void allocate(Board.Allocation allocation, Board board) {
        double currentCapacity = allocation.assignedResources.stream()
                .mapToDouble(r -> r.getCapacity(allocation.getItem(), board.getTime(), resolution))
                .sum();
        val maxThreads = allocation.item.getParameters().getThreads();
        Deque<ProjectResource> resources = new ArrayDeque<>(board.getSortedFreeResources(allocation.item, resolution));
        while (!resources.isEmpty() && currentCapacity < allocation.todo && allocation.assignedResources.size() < maxThreads) {
            val resource = resources.pop();
            board.allocate(allocation, resource);
            currentCapacity += resource.getCapacity(allocation.getItem(), board.getTime(), resolution);
        }
    }

    private void deallocateRedundant(Board board) {
        for (val allocation : board.getOngoing()) {
            float capacity = 0;
            List<ProjectResource> redundant = new ArrayList<>();
            for (ProjectResource resource : allocation.assignedResources) {
                capacity += resource.getCapacity(allocation.getItem(), board.getTime(), resolution);
                if (capacity > allocation.todo)
                    redundant.add(resource);
            }
            for (ProjectResource resource : redundant)
                board.deallocate(allocation, resource);
        }
    }

}
