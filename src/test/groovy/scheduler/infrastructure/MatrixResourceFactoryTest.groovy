package scheduler.infrastructure

import javafx.util.Pair
import scheduler.capacity.CapacityProvider
import spock.lang.Specification

class MatrixResourceFactoryTest extends Specification {

    def reader = Mock(MatrixReader)
    def capacityProviderFactory = Mock(CapacityProviderFactory)
    def interruptionsFactory = Mock(InterruptionsFactory)
    def cut = new MatrixResourceFactory(reader, capacityProviderFactory, interruptionsFactory)

    def "shall throw when unrecognized structure"() {
        given:
        reader.read() >> matrix

        when:
        cut.create()

        then:
        thrown(UnrecognizedStructureException)

        where:
        matrix << [
                [[]],
                [["resource"]],
                [["resource", "skill"]]
        ]
    }

    def "shall throw when no resource given"() {
        given:
        reader.read() >> matrix

        when:
        cut.create()

        then:
        thrown(EmptyIdException)

        where:
        matrix << [
                [[null, "skill", "1.0"]],
                [["", "skill", "1.0"]],
                [[" ", "skill", "1.0"]]
        ]
    }

    def "shall throw when non-number capacity"() {
        given:
        reader.read() >> matrix

        when:
        cut.create()

        then:
        thrown(NumberFormatException)

        where:
        matrix << [
                [["resource", "skill", "1"], ["resource1", "skill", null]],
                [["resource", "skill", "1"], ["resource2", "skill", ""]],
                [["resource", "skill", "1"], ["resource2", "skill", " "]],
                [["resource", "skill", "1"], ["resource2", "skill", "something"]],
        ]
    }

    def "shall create a list of resources using an interruption provider and a capacity creator"() {
        given:
        Map<String, List<Pair<Float, Float>>> globalInterruptions = [resource2: [new Pair<>(0.5f, 1.0f), new Pair<>(2.0f, 30.0f)]]
        def capacityProvider1 = Mock(CapacityProvider)
        def capacityProvider2 = Mock(CapacityProvider)
        reader.read() >> [["resource1", "capacityOne", "1.0"],
                          ["resource2", "capacityTwo", "2"]]

        when:
        def result = cut.create()

        then:
        1 * capacityProviderFactory.create("capacityOne", 1.0f) >> capacityProvider1
        1 * capacityProviderFactory.create("capacityTwo", 2.0f) >> capacityProvider2
        1 * interruptionsFactory.create() >> globalInterruptions
        result.size() == 2
        with(result[0]) {
            id == "resource1"
            capacityCalculator.providers == [capacityProvider1]
            interruptions.isEmpty()
        }
        with(result[1]) {
            id == "resource2"
            capacityCalculator.providers == [capacityProvider2]
            interruptions == globalInterruptions.resource2
        }
    }

    def "shall throw when duplicated id"() {
        given:
        reader.read() >> [
                ["resource1", "skill", "1"],
                ["resource2", "skill", "1"],
                ["resource1", "skill", "1"]
        ]

        when:
        cut.create()

        then:
        def e = thrown(DuplicationException)
        e.message == "resource1"
    }

    def "shall create resources with multiple skills"() {
        given:
        def capacityProvider1 = Mock(CapacityProvider)
        def capacityProvider2 = Mock(CapacityProvider)
        def capacityProvider3 = Mock(CapacityProvider)
        reader.read() >> [["resource1", "capacityOne", "1.0"],
                          ["  ", "capacityTwo", "2.0"],
                          ["resource2", "capacityTwo", "2"],
                          [null, "capacityThree", "3"]]

        when:
        def result = cut.create()

        then:
        1 * capacityProviderFactory.create("capacityOne", 1.0f) >> capacityProvider1
        2 * capacityProviderFactory.create("capacityTwo", 2.0f) >> capacityProvider2
        1 * capacityProviderFactory.create("capacityThree", 3.0f) >> capacityProvider3
        result.size() == 2
        with(result[0]) {
            id == "resource1"
            capacityCalculator.providers == [capacityProvider1, capacityProvider2]
        }
        with(result[1]) {
            id == "resource2"
            capacityCalculator.providers == [capacityProvider2, capacityProvider3]
        }
    }

}
