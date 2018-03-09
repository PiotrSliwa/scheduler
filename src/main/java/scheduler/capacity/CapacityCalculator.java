package scheduler.capacity;

import lombok.AllArgsConstructor;
import scheduler.Item;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class CapacityCalculator {
    private List<CapacityProvider> providers;

    public CapacityCalculator() {
        providers = Collections.singletonList(new StaticCapacityProvider(1.0f));
    }

    public float calculate(Item item) {
        return (float) providers.stream()
                .mapToDouble(p -> p.provide(item))
                .max()
                .orElse(0.0f);
    }

}
