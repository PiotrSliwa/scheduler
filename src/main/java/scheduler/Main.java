package scheduler;

import lombok.SneakyThrows;
import lombok.val;
import scheduler.infrastructure.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    private <T1 extends Identifiable, T2 extends Identifiable> List<List<String>> createSchedulePresentation(Map<T1, List<T2>> schedule, float resolution) {
        List<List<String>> result = new ArrayList<>();
        result.add(createScheduleHeader(schedule.values().iterator().next().size(), resolution));
        for (val entry : schedule.entrySet()) {
            List<String> row = new ArrayList<>();
            row.add(entry.getKey().getId());
            for (val frame : entry.getValue())
                row.add(frame == null ? "" : frame.getId());
            result.add(row);
        }
        return result;
    }

    private static List<String> createScheduleHeader(int scheduleSize, float resolution) {
        List<String> result = new ArrayList<>();
        result.add("");
        float time = 0.0f;
        for (int i = 0; i < scheduleSize; ++i) {
            result.add(Float.toString(time));
            time += resolution;
        }
        return result;
    }

    @SneakyThrows
    private MatrixReader createReader(String path) {
        return new CsvMatrixReader(Files.newBufferedReader(Paths.get(path)));
    }

    private void run() {

        val itemFactory = new MatrixItemFactory(createReader("pbs.csv"));
        val items = itemFactory.create().values();

        val interruptionsFactory = new MatrixInterruptionsFactory(createReader("interruptions.csv"));

        val capacityProviderFactory = new CapacityProviderFactory();
        val resourceFactory = new MatrixResourceFactory(createReader("resources.csv"), capacityProviderFactory, interruptionsFactory);
        val resources = resourceFactory.create();

        val costCalculator = new ProjectLengthCostCalculator();
        val scheduleCreator = new ScheduleCreator(items, costCalculator);
        val schedule = scheduleCreator.create(resources);

        val itemSchedulePresentation = createSchedulePresentation(schedule.getItemSchedule(), scheduleCreator.getResolution());
        val resourceSchedulePresentation = createSchedulePresentation(schedule.getResourceSchedule(), scheduleCreator.getResolution());

        val excelWriter = new ExcelMatrixWriter(Paths.get("result.xlsx"));
        excelWriter.addSection("Results", "Item schedule", itemSchedulePresentation);
        excelWriter.addSection("Results", "Resource schedule", resourceSchedulePresentation);
        excelWriter.addSection(
                "Results",
                "Total cost (all resources engaged for the whole time)",
                Collections.singletonList(Collections.singletonList(schedule.getTotalCost().toString())));
        excelWriter.write();
    }

    public static void main(String[] args) {
        new Main().run();
    }

}
