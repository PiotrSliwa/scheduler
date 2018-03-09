package scheduler;

import lombok.AllArgsConstructor;
import lombok.val;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectLengthCostCalculator implements CostCalculator {

    @Override
    public float calculate(List<List<WorkPackage>> timeline, float resolution) {
        val resources = timeline.stream()
                .flatMap(Collection::stream)
                .map(WorkPackage::getResource)
                .collect(Collectors.toSet());
        return resources.size() * timeline.size() * resolution;
    }
}
