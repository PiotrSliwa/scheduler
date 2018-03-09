package scheduler.infrastructure;

import scheduler.Item;

import java.util.Map;

public interface ItemFactory {
    Map<String, Item> create();
}
