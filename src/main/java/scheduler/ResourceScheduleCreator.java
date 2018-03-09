package scheduler;

import lombok.val;

import java.util.*;

public class ResourceScheduleCreator {

    public static Map<ProjectResource, List<Item>> create(List<List<WorkPackage>> timeline) {
        Map<ProjectResource, List<Item>> result = new HashMap<>();
        int frameNum = 0;
        for (val frame : timeline) {
            for (val pkg : frame) {
                result
                        .computeIfAbsent(pkg.getResource(), k -> new ArrayList<>(Arrays.asList(new Item[timeline.size()])))
                        .set(frameNum, pkg.getItem());
            }
            frameNum++;
        }
        return result;
    }

}
