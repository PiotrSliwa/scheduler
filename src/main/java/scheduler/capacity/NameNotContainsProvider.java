package scheduler.capacity;

import lombok.AllArgsConstructor;
import scheduler.Item;

@AllArgsConstructor
public class NameNotContainsProvider implements CapacityProvider {
    private String string;
    private float capacity;

    @Override
    public float provide(Item item) {
        return !item.getParameters().getName().contains(string) ? capacity : 0.0f;
    }
}