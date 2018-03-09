package scheduler.capacity;

import scheduler.Item;

import java.util.Optional;

public interface CapacityProvider {
    float provide(Item item);
}
