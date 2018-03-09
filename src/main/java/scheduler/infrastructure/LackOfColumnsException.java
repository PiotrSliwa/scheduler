package scheduler.infrastructure;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@ToString
public class LackOfColumnsException extends RuntimeException {
    private List<String> columns;
}
