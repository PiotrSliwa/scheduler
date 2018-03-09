package scheduler;

import lombok.Getter;
import lombok.val;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ScheduleCreator {

    public static final float MIN_RESOLUTION = 0.00001f;

    public static class SizeLessThanMinResolutionException extends RuntimeException {
    }

    private Prioritizer prioritizer = new Prioritizer();
    private Collection<Collection<Item>> prioritizedItems;
    @Getter private float resolution;
    private final CostCalculator costCalculator;

    public ScheduleCreator(Collection<Item> items, CostCalculator costCalculator) {
        this.prioritizedItems = prioritizer.prioritize(items);
        this.resolution = calculateResolution(items);
        this.costCalculator = costCalculator;
    }

    private static int gcd(int a, int b) {
        return a == 0 ? b : gcd(b % a, a);
    }

    private static int findGCD(List<Integer> ints) {
        int result = ints.get(0);
        for (val i : ints)
            result = gcd(i, result);
        return result;
    }

    private static float calculateResolution(Collection<Item> items) {
        List<Integer> sizes = items.stream()
                .map(i -> i.getParameters().getSize())
                .filter(Objects::nonNull)
                .peek(s -> { if (s < MIN_RESOLUTION) throw new SizeLessThanMinResolutionException(); })
                .map(s -> (int)(s / MIN_RESOLUTION))
                .collect(Collectors.toList());
        if (sizes.isEmpty())
            return 1;
        val intResult = findGCD(sizes);
        return intResult * MIN_RESOLUTION;
    }

    public Schedule create(Collection<ProjectResource> resources) {
        val timelineFactory = new TimelineFactory(resolution, resources);
        val timeline = timelineFactory.create(prioritizedItems);
        val itemSchedule = ItemScheduleCreator.create(timeline);
        val resourceSchedule = ResourceScheduleCreator.create(timeline);
        return new Schedule(resourceSchedule, itemSchedule, costCalculator.calculate(timeline, resolution));
    }

}
