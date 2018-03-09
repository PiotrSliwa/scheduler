package scheduler

import javafx.util.Pair
import scheduler.capacity.CapacityCalculator
import spock.lang.Specification
import spock.lang.Unroll

import static spock.util.matcher.HamcrestMatchers.closeTo

class ProjectResourceTest extends Specification {

    def capacityCalculator = Mock(CapacityCalculator)
    def item = Mock(Item)

    @Unroll
    def "get capacity (#time, #delta, #expected)"() {
        when:
        def cut = new ProjectResource(
                'resource',
                capacityCalculator,
                [new Pair<Float, Float>(0.3f, 0.5f),
                 new Pair<Float, Float>(0.9f, 1.0f)])
        def result = cut.getCapacity(item, time, delta)

        then:
        1 * capacityCalculator.calculate(item) >> baseCapacity
        expected closeTo(result, 0.001f)

        where:
        time | baseCapacity | delta | expected
        0.0f | 1.0f         | 0.0f  | 0.0f
        0.0f | 1.0f         | 0.1f  | 0.1f
        0.0f | 1.0f         | 0.3f  | 0.3f
        0.0f | 1.0f         | 0.4f  | 0.3f
        0.0f | 1.0f         | 0.5f  | 0.3f
        0.0f | 1.0f         | 0.6f  | 0.4f
        0.0f | 0.5f         | 0.6f  | 0.2f
        0.1f | 1.0f         | 0.6f  | 0.4f
        0.2f | 1.0f         | 0.2f  | 0.1f
        0.3f | 1.0f         | 0.2f  | 0.0f
        0.3f | 1.0f         | 0.3f  | 0.1f
        0.9f | 1.0f         | 0.1f  | 0.0f
        0.0f | 1.0f         | 1.0f  | 0.7f
        0.0f | 1.0f         | 1.1f  | 0.8f
        0.0f | 0.5f         | 1.1f  | 0.4f
        1.0f | 1.0f         | 1.0f  | 1.0f
        1.0f | 0.1f         | 1.0f  | 0.1f
        1.0f | 0.7f         | 1.0f  | 0.7f
    }

}
