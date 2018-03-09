package scheduler.infrastructure;

import javafx.util.Pair;

import java.util.List;
import java.util.Map;

public interface InterruptionsFactory {
    Map<String, List<Pair<Float, Float>>> create();
}
