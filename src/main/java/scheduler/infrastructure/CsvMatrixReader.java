package scheduler.infrastructure;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class CsvMatrixReader implements MatrixReader {
    private Reader reader;

    @SneakyThrows
    @Override
    public List<List<String>> read() {
        List<List<String>> result = new ArrayList<>();
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(reader);
        for (CSVRecord record : records) {
            List<String> row = new ArrayList<>();
            for (String elem : record)
                row.add(elem);
            result.add(row);
        }
        return result;
    }
}
