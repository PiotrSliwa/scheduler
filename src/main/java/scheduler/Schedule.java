package scheduler;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class Schedule {
    private Map<ProjectResource, List<Item>> resourceSchedule;
    private Map<Item, List<ProjectResource>> itemSchedule;
    private Float totalCost;
}
