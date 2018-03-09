package scheduler

import scheduler.infrastructure.ItemBuilder
import spock.lang.Specification

class ItemBuilderTest extends Specification {

    def "shall return empty list by default"() {
        when:
        def result = new ItemBuilder().build()

        then:
        result.isEmpty()
    }

    def "shall throw when any id is empty or null"() {
        when:
        new ItemBuilder().add(new ItemBuilder.Placeholder(id, 'name', 1, 2, [childId], [depId]))

        then:
        thrown(ItemBuilder.InvalidIdException)

        where:
        id    | childId | depId
        null  | 'id1'   | 'id2'
        ''    | 'id1'   | 'id2'
        'id1' | null    | 'id2'
        'id1' | ''      | 'id2'
        'id1' | 'id2'   | null
        'id1' | 'id2'   | ''
    }

    def "shall throw if same id is added"() {
        given:
        def cut = new ItemBuilder()
        cut.add(new ItemBuilder.Placeholder('id1', 'name1', 1, 3, [], []))

        when:
        cut.add(new ItemBuilder.Placeholder('id1', 'name2', 2, 4, [], []))

        then:
        thrown(ItemBuilder.IdAlreadyAddedException)
    }

    def "shall build flat items"() {
        given:
        def cut = new ItemBuilder()
        cut.add(new ItemBuilder.Placeholder('id1', 'name1', 1, 3, [], []))
        cut.add(new ItemBuilder.Placeholder('id2', 'name2', 2, 4, [], []))

        when:
        def result = cut.build()

        then:
        result.size() == 2
        with(result['id1']) {
            id == 'id1'
            parameters.name == 'name1'
            parameters.size == 1
            parameters.threads == 3
        }
        with(result['id2']) {
            id == 'id2'
            parameters.name == 'name2'
            parameters.size == 2
            parameters.threads == 4
        }
    }

    def "shall throw on build() when non-existing id added as a child"() {
        given:
        def cut = new ItemBuilder()
        cut.add(new ItemBuilder.Placeholder('id1', 'name1', 1, 2, ['id2'], []))

        when:
        cut.build()

        then:
        thrown(ItemBuilder.NonExistingIdAsChildException)
    }

    void assertChildren(Item item, String... ids) {
        assert !item.children.isEmpty()
        assert ids.each { id -> item.children.any { child -> child.id == id } }
    }

    def "shall add children"() {
        given:
        def cut = new ItemBuilder()
        cut.add(new ItemBuilder.Placeholder('id1', 'name1', null, 1, ['id2', 'id3'], []))
        cut.add(new ItemBuilder.Placeholder('id2', 'name1', 2, 2, ['id4'], []))
        cut.add(new ItemBuilder.Placeholder('id3', 'name1', 3, 3, [], []))
        cut.add(new ItemBuilder.Placeholder('id4', 'name1', null, 4, [], []))
        cut.add(new ItemBuilder.Placeholder('id5', 'name1', 5, 5, ['id1'], []))

        when:
        def result = cut.build()

        then:
        assertChildren(result['id1'], 'id2', 'id3')
        assertChildren(result['id2'], 'id4')
        result['id3'].children.isEmpty()
        result['id4'].children.isEmpty()
        assertChildren(result['id5'], 'id1')
    }

    def "shall throw on build() when non-existing id added as a dependency"() {
        given:
        def cut = new ItemBuilder()
        cut.add(new ItemBuilder.Placeholder('id1', 'name1', 1, 2, [], ['id2']))

        when:
        cut.build()

        then:
        thrown(ItemBuilder.NonExistingIdAsDependencyException)
    }

    void assertDependencies(Item item, String... ids) {
        assert !item.dependencies.isEmpty()
        assert ids.each { id -> item.dependencies.any { dep -> dep.id == id } }
    }

    def "shall add dependencies"() {
        given:
        def cut = new ItemBuilder()
        cut.add(new ItemBuilder.Placeholder('id1', 'name1', 1, 1, [], ['id2', 'id3']))
        cut.add(new ItemBuilder.Placeholder('id2', 'name2', 2, 2, [], ['id4']))
        cut.add(new ItemBuilder.Placeholder('id3', 'name3', 3, 3, [], []))
        cut.add(new ItemBuilder.Placeholder('id4', 'name4', 4, 4, [], []))
        cut.add(new ItemBuilder.Placeholder('id5', 'name5', 5, 5, [], ['id1']))

        when:
        def result = cut.build()

        then:
        assertDependencies(result['id1'], 'id2', 'id3')
        assertDependencies(result['id2'], 'id4')
        result['id3'].dependencies.isEmpty()
        result['id4'].dependencies.isEmpty()
        assertDependencies(result['id5'], 'id1')
    }
}
