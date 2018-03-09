package scheduler.infrastructure;

public class DuplicationException extends RuntimeException {
    public DuplicationException(String duplication) {
        super(duplication);
    }
}
