package scheduler;

import javafx.util.Pair;
import lombok.*;
import scheduler.capacity.CapacityCalculator;

import java.util.List;

@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public class ProjectResource implements Identifiable {
    @Getter private String id;
    private CapacityCalculator capacityCalculator;
    private List<Pair<Float, Float>> interruptions;

    public float getCapacity(Item item, float time, float delta) {
        Pair<Float, Float> taskTime = new Pair<>(time, time + delta);
        val interruptedTime = interruptions.stream()
                .map(i -> new Pair<>(Math.max(taskTime.getKey(), i.getKey()), Math.min(taskTime.getValue(), i.getValue())))
                .filter(ip -> ip.getValue() > taskTime.getKey())
                .filter(ip -> ip.getKey() < taskTime.getValue())
                .mapToDouble(ip -> ip.getValue() - ip.getKey())
                .sum();
        return (float) (delta - interruptedTime) * capacityCalculator.calculate(item);
    }
}
