package scheduler.infrastructure;

import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.val;

import java.util.*;

@AllArgsConstructor
public class MatrixInterruptionsFactory implements InterruptionsFactory {
    private MatrixReader reader;

    @Override
    public Map<String, List<Pair<Float, Float>>> create() {
        val matrix = reader.read();
        if (!isValidStructure(matrix))
            throw new UnrecognizedStructureException();
        Map<String, List<Pair<Float, Float>>> result = new HashMap<>();
        String previousId = null;
        for (val row : matrix) {
            val rowId = row.get(0);
            val isGrouped = isSignificantlyEmpty(rowId);
            val id = isGrouped ? previousId : rowId;
            if (isSignificantlyEmpty(id))
                throw new EmptyIdException();
            if (isGrouped)
                result.get(id).add(getDateRange(row));
            else if (result.containsKey(id))
                throw new DuplicationException(id);
            else
                result.put(id, new ArrayList<>(Collections.singletonList(getDateRange(row))));
            previousId = id;
        }
        return result;
    }

    private static Pair<Float, Float> getDateRange(List<String> row) {
        return new Pair<>(getFloat(row, 1), getFloat(row, 2));
    }

    private static float getFloat(List<String> row, int index) {
        val capacity = row.get(index);
        if (capacity == null)
            throw new NumberFormatException("null");
        return Float.parseFloat(capacity);
    }

    private static boolean isValidStructure(List<List<String>> matrix) {
        return matrix.size() == 0 || matrix.stream().allMatch(row -> row.size() == 3);
    }

    private static boolean isSignificantlyEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }
}
