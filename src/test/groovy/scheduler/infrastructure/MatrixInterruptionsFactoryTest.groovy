package scheduler.infrastructure

import spock.lang.Specification

class MatrixInterruptionsFactoryTest extends Specification {

    def reader = Mock(MatrixReader)
    def cut = new MatrixInterruptionsFactory(reader)

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
                [["resource", "start day"]]
        ]
    }

    def "shall not throw when empty matrix"() {
        given:
        reader.read() >> []

        when:
        def result = cut.create()

        then:
        result.isEmpty()
    }

    def "shall throw when non-number day given"() {
        given:
        reader.read() >> matrix

        when:
        cut.create()

        then:
        thrown(NumberFormatException)

        where:
        matrix << [
                [["resource", "string", "1"]],
                [["resource", "1", "string"]],
                [["resource", "  ", "1"]],
                [["resource", "", "  "]],
                [["resource", null, "1"]],
                [["resource", "1", null]]
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
                [[null, "1", "2"]],
                [["", "1", "2"]],
                [[" ", "1", "2"]]
        ]
    }

    def "shall throw when duplicated id"() {
        given:
        reader.read() >> [
                ["resource1", "1", "1"],
                ["resource2", "1", "1"],
                ["resource1", "1", "1"]
        ]

        when:
        cut.create()

        then:
        def e = thrown(DuplicationException)
        e.message == "resource1"
    }

    def "shall create map of interruptions"() {
        given:
        reader.read() >> [["resource1", "1", "2.5"],
                          ["  ", "3", "4.5"],
                          ["resource2", "5.5", "6"],
                          [null, "7.5", "8"]]

        when:
        def result = cut.create()

        then:
        result.size() == 2
        with (result['resource1'][0]) {
            Math.abs(getKey() - 1.0f) < 0.01
            Math.abs(getValue() - 2.5f) < 0.01
        }
        with (result['resource1'][1]) {
            Math.abs(getKey() - 3.0f) < 0.01
            Math.abs(getValue() - 4.5f) < 0.01
        }
        with (result['resource2'][0]) {
            Math.abs(getKey() - 5.5f) < 0.01
            Math.abs(getValue() - 6.0f) < 0.01
        }
        with (result['resource2'][1]) {
            Math.abs(getKey() - 7.5f) < 0.01
            Math.abs(getValue() - 8.0f) < 0.01
        }
    }

}
