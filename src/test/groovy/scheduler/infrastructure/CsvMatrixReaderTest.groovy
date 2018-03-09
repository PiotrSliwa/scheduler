package scheduler.infrastructure

import spock.lang.Specification


class CsvMatrixReaderTest extends Specification {

    def "read"() {
        given:
        def cut = new CsvMatrixReader(new FileReader(getClass().getClassLoader().getResource("test.csv").getFile()))

        when:
        def result = cut.read()

        then:
        result == [
                ["a1", "a2", "", "a4"],
                ["", "b2", "b3", ""],
                ["c1", "c2", "c3", "c4"]
        ]
    }

}
