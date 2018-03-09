package scheduler

import scheduler.infrastructure.LackOfColumnsException
import scheduler.infrastructure.MatrixItemFactory
import scheduler.infrastructure.MatrixReader
import spock.lang.Specification

class MatrixItemFactoryTest extends Specification {

    def reader = Mock(MatrixReader) {
        read() >> [
                ['Id', 'Threads', 'Predecessors', 'Size', 'Name', '', '', '', '', '', ''],
                ['1', '', '', '', 'Task 1', '', '', '', '', '', ''],
                ['2', '2', '', '3.5', '', 'Task 1.1', '', '', '', '', ''],
                ['3', '3', '2', '3', '', 'Task 1.2', '', '', '', '', ''],
                ['4', '1', '2', '3', '', 'Task 1.3', '', '', '', '', ''],
                ['5', '1', '', '', 'Task 2', '', '', '', '', '', ''],
                ['6', '1', '', '', '', 'Task 2.1', '', '', '', '', ''],
                ['7', '1', '', '', '', '', 'Task 2.1.1', '', '', '', ''],
                ['8', '1', '', '4', '', '', '', 'Task 2.1.1.1', '', '', ''],
                ['9', '1', '', '', '', '', '', 'Task 2.1.1.2', '', '', ''],
                ['10', '1', '', '1.5', '', '', '', '', 'Task 2.1.1.2.1', '', ''],
                ['11', '1', '2;3;10', '5', '', '', '', '', 'Task 2.1.1.2.2', '', ''],
                ['12', '1', '10', '2.5', '', '', '', '', 'Task 2.1.1.2.3', '', ''],
                ['13', '1', '', '7.5', '', '', '', '', 'Task 2.1.1.2.4', '', ''],
                ['25', '1', '', '', '', 'Task 2.2', '', '', '', '', ''],
                ['26', '1', '13', '2.5', '', '', 'Task 2.2.1', '', '', '', ''],
                ['27', '1', '', '0', '', '', 'Task 2.2.2', '', '', '', '']
        ]
    }

    def cut = new MatrixItemFactory(reader)

    void assertSuccessor(Map<String, Item> result, String childId, String childName, int ... childAddress) {
        Item current = result["__root__"]
        for (int i = 0; i < childAddress.size(); ++i)
            current = current.children[childAddress[i]]
        assert current.id == childId
        assert current.parameters.name == childName
    }
    
    void assertDefaultSuccessors(Map<String, Item> result) {
        result["__root__"].getChildren().size() == 2
        assertSuccessor(result, '1', 'Task 1', 0)
        assertSuccessor(result, '2', 'Task 1.1', 0, 0)
        assertSuccessor(result, '3', 'Task 1.2', 0, 1)
        assertSuccessor(result, '4', 'Task 1.3', 0, 2)
        assertSuccessor(result, '5', 'Task 2', 1)
        assertSuccessor(result, '6', 'Task 2.1', 1, 0)
        assertSuccessor(result, '7', 'Task 2.1.1', 1, 0, 0)
        assertSuccessor(result, '8', 'Task 2.1.1.1', 1, 0, 0, 0)
        assertSuccessor(result, '9', 'Task 2.1.1.2', 1, 0, 0, 1)
        assertSuccessor(result, '10', 'Task 2.1.1.2.1', 1, 0, 0, 1, 0)
        assertSuccessor(result, '11', 'Task 2.1.1.2.2', 1, 0, 0, 1, 1)
        assertSuccessor(result, '12', 'Task 2.1.1.2.3', 1, 0, 0, 1, 2)
        assertSuccessor(result, '13', 'Task 2.1.1.2.4', 1, 0, 0, 1, 3)
        assertSuccessor(result, '25', 'Task 2.2', 1, 1)
        assertSuccessor(result, '26', 'Task 2.2.1', 1, 1, 0)
        assertSuccessor(result, '27', 'Task 2.2.2', 1, 1, 1)
    }

    def "genealogy"() {
        when:
        def result = cut.create()

        then:
        assertDefaultSuccessors(result)
    }

    def "genealogy from mixed columns"() {
        when:
        reader.read() >> [
                ['Predecessors', 'Id', 'Size', 'Threads', 'Name', '', '', '', '', '', ''],
                ['', '1', '', '', 'Task 1', '', '', '', '', '', ''],
                ['', '2', '3.5', '2', '', 'Task 1.1', '', '', '', '', ''],
                ['2', '3', '3', '3', '', 'Task 1.2', '', '', '', '', ''],
                ['2', '4', '3', '1', '', 'Task 1.3', '', '', '', '', ''],
                ['', '5', '', '1', 'Task 2', '', '', '', '', '', ''],
                ['', '6', '', '1', '', 'Task 2.1', '', '', '', '', ''],
                ['', '7', '', '1', '', '', 'Task 2.1.1', '', '', '', ''],
                ['', '8', '4', '1', '', '', '', 'Task 2.1.1.1', '', '', ''],
                ['', '9', '', '1', '', '', '', 'Task 2.1.1.2', '', '', ''],
                ['', '10', '1.5', '1', '', '', '', '', 'Task 2.1.1.2.1', '', ''],
                ['2;3;10', '11', '5', '1', '', '', '', '', 'Task 2.1.1.2.2', '', ''],
                ['10', '12', '2.5', '1', '', '', '', '', 'Task 2.1.1.2.3', '', ''],
                ['', '13', '7.5', '1', '', '', '', '', 'Task 2.1.1.2.4', '', ''],
                ['', '25', '', '1', '', 'Task 2.2', '', '', '', '', ''],
                ['13', '26', '2.5', '1', '', '', 'Task 2.2.1', '', '', '', ''],
                ['', '27', '0', '1', '', '', 'Task 2.2.2', '', '', '', '']
        ]
        def result = cut.create()

        then:
        assertDefaultSuccessors(result)
    }

    def findItem(Item root, String id) {
        def stack = [root]
        while (!stack.isEmpty()) {
            def item = stack.pop()
            if (item.id == id)
                return item
            stack.addAll(item.children)
        }
        throw new RuntimeException(sprintf("Cannot find item id %s", id))
    }

    void assertDependency(Map<String, Item> result, String id, String... dependencyIds) {
        def item = result[id]
        assert dependencyIds.each { depId ->
            item.dependencies.contains(depId)
        }
    }

    def "dependencies"() {
        when:
        def result = cut.create()

        then:
        result["__root__"].getDependencies().isEmpty()
        assertDependency(result, '3', '2')
        assertDependency(result, '4', '2')
        assertDependency(result, '11', '2', '3', '10')
        assertDependency(result, '12', '10')
        assertDependency(result, '26', '13')

    }

    def "size"() {
        when:
        def result = cut.create()

        then:
        result['1'].parameters.size == null
        result['2'].parameters.size == 3.5
        result['3'].parameters.size == 3
    }

    def "threads"() {
        when:
        def result = cut.create()

        then:
        result['1'].parameters.threads == null
        result['2'].parameters.threads == 2
        result['3'].parameters.threads == 3
    }

    def "shall throw when invalid structure given"() {
        given:
        def reader = Mock(MatrixReader) {
            read() >> matrix
        }

        when:
        new MatrixItemFactory(reader)

        then:
        def e = thrown(LackOfColumnsException)
        e.columns == notifiedColumns

        where:
        matrix << [
                [[]],
                [["Id"]],
                [["Id", "Threads"]],
                [["Id", "Threads", "Predecessors"]],
                [["Id", "Threads", "Predecessors", "Size"]]
        ]
        notifiedColumns << [
                ['id', 'threads', 'predecessors', 'size', 'name'],
                ['threads', 'predecessors', 'size', 'name'],
                ['predecessors', 'size', 'name'],
                ['size', 'name'],
                ['name'],
        ]
    }

    def "name has to be in the end"() {
        given:
        def reader = Mock(MatrixReader) {
            read() >> [["Id", "Threads", "Predecessors", "Name", "Size"]]
        }

        when:
        new MatrixItemFactory(reader)

        then:
        thrown(MatrixItemFactory.InvalidNamePositionException)
    }

}
