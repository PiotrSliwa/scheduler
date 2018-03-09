package scheduler;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class WorkPackage {
    private ProjectResource resource;
    private Item item;
    private float workDone;

    @Override
    public String toString() {
        return String.format("{%s, %s, %f}", resource.getId(), item.getId(), workDone);
    }
}