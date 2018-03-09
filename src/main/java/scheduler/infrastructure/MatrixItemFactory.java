package scheduler.infrastructure;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import scheduler.Item;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class MatrixItemFactory implements ItemFactory {
    private List<List<String>> matrix;
    private ColumnConfiguration columnConfiguration;
    private String dependencyDelimiter = ";";

    private static final String ID = "id";
    private static final String THREADS = "threads";
    private static final String PREDECESSORS = "predecessors";
    private static final String SIZE = "size";
    private static final String NAME = "name";

    public static class InvalidNamePositionException extends RuntimeException {
        public InvalidNamePositionException() {
            super("Name has to be last");
        }
    }

    public MatrixItemFactory(MatrixReader reader) {
        this.matrix = reader.read();
        this.columnConfiguration = new ColumnConfiguration(matrix.get(0));
        assertColumnsExist(ID, THREADS, PREDECESSORS, SIZE, NAME);
        assertNameIsLast(ID, THREADS, PREDECESSORS, SIZE);
    }

    private void assertColumnsExist(String... names) {
        List<String> result = new ArrayList<>();
        for (val name : names) {
            try {
                columnConfiguration.getIndexOf(name);
            }
            catch (UnknownColumnException e) {
                result.add(name);
            }
        }
        if (!result.isEmpty())
            throw new LackOfColumnsException(result);
    }

    private void assertNameIsLast(String... names) {
        for (val name : names)
            if (columnConfiguration.getIndexOf(name) > columnConfiguration.getIndexOf(NAME))
                throw new InvalidNamePositionException();
    }

    @AllArgsConstructor
    private static class PlaceholderNode {
        PlaceholderNode parent;
        ItemBuilder.Placeholder value;
    }

    @Override
    @SneakyThrows
    public Map<String, Item> create() {
        PlaceholderNode current = new PlaceholderNode(null, new ItemBuilder.Placeholder("__root__", "__root__"));
        ItemBuilder builder = new ItemBuilder();
        builder.add(current.value);
        int currentLevel = 0;
        for (int i = 1; i < matrix.size(); ++i) {
            val row = matrix.get(i);
            val nestedItem = getNestedItem(row);
            int diff = nestedItem.level - currentLevel;
            if (diff <= 0) {
                for (int j = diff; j < 0; ++j) {
                    current = current.parent;
                    currentLevel--;
                }
            }
            else if (diff > 1)
                throw new RuntimeException(String.format("Too big level difference (%d)!", diff));

            val child = new PlaceholderNode(current, nestedItem.placeholder);
            child.value.dependencyIds = getDependencies(row);
            builder.add(child.value);
            current.value.childIds.add(child.value.id);
            current = child;
            currentLevel++;
        }
        return builder.build();
    }

    private List<String> getDependencies(List<String> row) {
        return Arrays.stream(getValue(row, PREDECESSORS).split(dependencyDelimiter))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String getValue(List<String> row, String key) {
        return row.get(columnConfiguration.getIndexOf(key));
    }

    @AllArgsConstructor
    private static class NestedItem {
        int level;
        ItemBuilder.Placeholder placeholder;
    }

    private NestedItem getNestedItem(List<String> row) {
        val treeStartCol = columnConfiguration.getIndexOf(NAME);
        for (int i = treeStartCol; i < row.size(); ++i) {
            val name = row.get(i);
            if (!name.isEmpty())
                return new NestedItem(i - treeStartCol, new ItemBuilder.Placeholder(
                        getValue(row, ID),
                        name,
                        getItemSize(row),
                        getThreads(row)));
        }
        throw new RuntimeException(String.format("Cannot find a name for element id %s!", getValue(row, ID)));
    }

    private Integer getThreads(List<String> row) {
        return getInt(row, columnConfiguration.getIndexOf(THREADS));
    }

    private Float getItemSize(List<String> row) {
        return getFloat(row, columnConfiguration.getIndexOf(SIZE));
    }

    private static Float getFloat(List<String> row, int col) {
        val sizeStr = row.get(col);
        return sizeStr.isEmpty() ? null : Float.parseFloat(sizeStr);
    }

    private static Integer getInt(List<String> row, int col) {
        val sizeStr = row.get(col);
        return sizeStr.isEmpty() ? null : Integer.parseInt(sizeStr);
    }

}
