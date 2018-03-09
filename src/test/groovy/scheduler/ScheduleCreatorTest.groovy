package scheduler

import spock.lang.Specification
import spock.lang.Unroll

import static spock.util.matcher.HamcrestMatchers.closeTo


class ScheduleCreatorTest extends Specification {

    Collection<Item> createItems(List<Float> sizes) {
        return sizes.collect { new Item('id', new Item.Parameters('name', it, 1)) }
    }

    def costCalculator = Mock(CostCalculator)

    @Unroll
    def "resolution shall fit the needs of item sizes - #expected - #sizes"() {
        given:
        def cut = new ScheduleCreator(createItems(sizes), costCalculator)

        when:
        def resolution = cut.getResolution()

        then:
        resolution closeTo(expected, 0.0001f)

        where:
        sizes                     | expected
        [1.0f]                    | 1.0f
        [0.5f]                    | 0.5f
        [1.0f, 0.5f]              | 0.5f
        [1.0f, 0.5f, 0.3f]        | 0.1f
        [0.2f, 0.6f, 1.0f]        | 0.2f
        [0.2f, 0.6f, 0.9f, 1.0f]  | 0.1f
        [0.02f, 0.6f, 0.9f, 1.0f] | 0.02f
    }

    def "shall not consider null sizes"() {
        given:
        def cut = new ScheduleCreator(createItems(sizes), costCalculator)

        when:
        def resolution = cut.getResolution()

        then:
        resolution closeTo(expected, 0.0001f)

        where:
        sizes                     | expected
        [null]                    | 1.0f
        [null, 0.5f]              | 0.5f
        [1.0f, 0.5f, null]        | 0.5f
        [0.2f, 0.6f, null, 1.0f]  | 0.2f
    }

    def "shall throw when size is less than minimum resolution"() {
        when:
        new ScheduleCreator(createItems([(float)(ScheduleCreator.MIN_RESOLUTION / 2)]), costCalculator)

        then:
        thrown(ScheduleCreator.SizeLessThanMinResolutionException)
    }

    def "shall calculate cost"() {
        given:
        def cut = new ScheduleCreator([], costCalculator)

        when:
        def result = cut.create([])

        then:
        1 * costCalculator.calculate(_, cut.resolution) >> 123.0f
        result.getTotalCost() == 123.0f
    }

}
