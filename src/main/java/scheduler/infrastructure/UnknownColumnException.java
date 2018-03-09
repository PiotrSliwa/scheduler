package scheduler.infrastructure;

public class UnknownColumnException extends RuntimeException {
    public UnknownColumnException(String column) {
        super(column);
    }
}
