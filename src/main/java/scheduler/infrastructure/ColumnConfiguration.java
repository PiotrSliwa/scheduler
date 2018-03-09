package scheduler.infrastructure;

import lombok.val;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnConfiguration {
    private Map<String, Integer> indexes = new HashMap<>();

    public ColumnConfiguration(List<String> header) {
        for (int i = 0; i < header.size(); ++i) {
            val normalized = normalize(header.get(i));
            if (normalized.isEmpty())
                continue;
            if (indexes.containsKey(normalized))
                throw new DuplicationException(normalized);
            indexes.put(normalized, i);
        }
    }

    private static String normalize(String name) {
        return name.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    public int getIndexOf(String name) {
        val result = indexes.get(normalize(name));
        if (result == null)
            throw new UnknownColumnException(name);
        return result;
    }
}
