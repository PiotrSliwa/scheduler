package scheduler.infrastructure;

import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.val;
import scheduler.ProjectResource;
import scheduler.capacity.CapacityCalculator;
import scheduler.capacity.CapacityProvider;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MatrixResourceFactory {
    private MatrixReader reader;
    private CapacityProviderFactory capacityProviderFactory;
    private InterruptionsFactory interruptionsFactory;

    @AllArgsConstructor
    private static class Placeholder {
        String skill;
        float capacity;
    }

    public List<ProjectResource> create() {
        val interruptions = interruptionsFactory.create();
        val matrix = reader.read();
        if (!isValidStructure(matrix))
            throw new UnrecognizedStructureException();
        Map<String, List<Placeholder>> result = new LinkedHashMap<>();
        String previousId = null;
        for (val row : matrix) {
            val rowId = row.get(0);
            val isGrouped = isSignificantlyEmpty(rowId);
            val id = isGrouped ? previousId : rowId;
            if (isSignificantlyEmpty(id))
                throw new EmptyIdException();
            if (isGrouped)
                result.get(id).add(new Placeholder(row.get(1), getCapacity(row)));
            else if (result.containsKey(id))
                throw new DuplicationException(id);
            else
                result.put(id, new ArrayList<>(Collections.singletonList(new Placeholder(row.get(1), getCapacity(row)))));
            previousId = id;
        }
        return result.entrySet().stream()
                .map(e -> createProjectResource(e.getKey(), e.getValue(), getInterruptions(interruptions, e.getKey())))
                .collect(Collectors.toList());
    }

    private static boolean isSignificantlyEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    private static float getCapacity(List<String> row) {
        val capacity = row.get(2);
        if (capacity == null)
            throw new NumberFormatException("null");
        return Float.parseFloat(capacity);
    }

    private static boolean isValidStructure(List<List<String>> matrix) {
        return matrix.size() >= 1 && matrix.get(0).size() == 3;
    }

    private static List<Pair<Float, Float>> getInterruptions(Map<String, List<Pair<Float, Float>>> interruptions, String resource) {
        if (interruptions != null) {
            val result = interruptions.get(resource);
            return result == null ? Collections.emptyList() : result;
        }
        return Collections.emptyList();
    }

    private ProjectResource createProjectResource(String id, List<Placeholder> placeholders, List<Pair<Float, Float>> interruptions) {
        val capacityCalculator = createCapacityCalculator(placeholders);
        return new ProjectResource(id, capacityCalculator, interruptions);
    }

    private CapacityCalculator createCapacityCalculator(List<Placeholder> placeholders) {
        val providers = createCapacityProviders(placeholders);
        return new CapacityCalculator(providers);
    }

    private List<CapacityProvider> createCapacityProviders(List<Placeholder> placeholders) {
        return placeholders.stream()
                .map(p -> capacityProviderFactory.create(p.skill, p.capacity))
                .collect(Collectors.toList());
    }

}
