package scheduler

import spock.lang.Specification

class ItemScheduleCreatorIntegrationTest extends Specification {

    def "one-item schedule"() {
        given:
        def resource = Mock(ProjectResource)
        def item = new Item('id')

        and:
        def frame = [new WorkPackage(resource, item, 1.0f)]

        when:
        def result = ItemScheduleCreator.create([frame])

        then:
        result[item] == [resource]
    }

    def "shall create item schedule"() {
        given:
        def resource1 = Mock(ProjectResource)
        def resource2 = Mock(ProjectResource)
        def item1 = new Item('id1')
        def item2 = new Item('id2')
        def item3 = new Item('id3')
        def item4 = new Item('id4')

        and:
        def frame1 = [new WorkPackage(resource1, item1, 1.0f)]

        and:
        def frame2 = [new WorkPackage(resource1, item2, 1.0f),
                      new WorkPackage(resource2, item3, 1.0f)]

        and:
        def frame3 = [new WorkPackage(resource1, item2, 1.0f),
                      new WorkPackage(resource2, item4, 1.0f)]

        when:
        def result = ItemScheduleCreator.create([frame1, frame2, frame3])

        then:
        result[item1] == [resource1, null, null]
        result[item2] == [null, resource1, resource1]
        result[item3] == [null, resource2, null]
        result[item4] == [null, null, resource2]
    }

}
