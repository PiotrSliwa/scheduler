package scheduler;

import java.util.List;

public interface CostCalculator {
    float calculate(List<List<WorkPackage>> timeline, float resolution); //TODO: Timeline as a class (with resolution included)
}
