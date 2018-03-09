package scheduler;

import lombok.val;

import java.util.*;

public class ItemScheduleCreator {

    public static Map<Item, List<ProjectResource>> create(List<List<WorkPackage>> timeline) {
        Map<Item, List<ProjectResource>> result = new HashMap<>();
        int frameNum = 0;
        for (val frame : timeline) {
            for (val pkg : frame) {
                result
                        .computeIfAbsent(pkg.getItem(), k -> new ArrayList<>(Arrays.asList(new ProjectResource[timeline.size()])))
                        .set(frameNum, pkg.getResource());
            }
            frameNum++;
        }
        return result;
    }

}
