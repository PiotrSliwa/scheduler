package scheduler.capacity;

import lombok.AllArgsConstructor;
import scheduler.Item;

@AllArgsConstructor
public class InGroupProvider implements CapacityProvider {
    private CapacityProvider provider;

    @Override
    public float provide(Item item) {
        Item current = item.getParent();
        float result = 0.0f;
        while (current != null) {
            result = Math.max(result, provider.provide(current));
            current = current.getParent();
        }
        return result;
    }
}
