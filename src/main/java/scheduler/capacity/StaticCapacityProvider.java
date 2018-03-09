package scheduler.capacity;

import lombok.AllArgsConstructor;
import scheduler.Item;

import java.util.Optional;

@AllArgsConstructor
public class StaticCapacityProvider implements CapacityProvider {
    private float capacity;

    @Override
    public float provide(Item item) {
        return capacity;
    }
}
