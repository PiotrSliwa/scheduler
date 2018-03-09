package scheduler.infrastructure

import spock.lang.Specification


class ColumnConfigurationTest extends Specification {

    def "shall throw when unknown column"() {
        given:
        def cut = new ColumnConfiguration([])

        when:
        cut.getIndexOf("dummy")

        then:
        def e = thrown(UnknownColumnException)
        e.message == "dummy"
    }

    def "shall not include empty names"() {
        given:
        def cut = new ColumnConfiguration(header)

        when:
        cut.getIndexOf(lookup)

        then:
        def e = thrown(UnknownColumnException)
        e.message == lookup

        where:
        header | lookup
        [""]   | ""
        [" "]  | ""
        [""]   | " "
        [" "]  | " "
    }

    def "shall throw on duplicated column"() {
        when:
        new ColumnConfiguration(header)

        then:
        def e = thrown(DuplicationException)
        e.message == duplication

        where:
        header       | duplication
        ["a", "a"]   | "a"
        ["a", "A"]   | "a"
        ["a", " a "] | "a"
        ["a", " A "] | "a"
    }

    def "shall return column index"() {
        given:
        def cut = new ColumnConfiguration(header)

        when:
        def result = cut.getIndexOf(lookup)

        then:
        result == expected

        where:
        header                | lookup          | expected
        ["a"]                 | "a"             | 0
        ["a"]                 | "A"             | 0
        ["a"]                 | " a "           | 0
        ["a"]                 | " A "           | 0
        ["a", "b"]            | "b"             | 1
        ["a", "b"]            | "B"             | 1
        ["a", "b"]            | " b "           | 1
        ["a", "b"]            | " B "           | 1
        ["a", "b", "  c  D "] | "c d"           | 2
        ["a", "b", "c d"]     | "    C    D   " | 2
    }

}
