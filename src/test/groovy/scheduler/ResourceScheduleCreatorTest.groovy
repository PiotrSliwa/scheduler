package scheduler

import spock.lang.Specification

class ResourceScheduleCreatorTest extends Specification {

    def "one-item schedule"() {
        given:
        def resource = Mock(ProjectResource)
        def item = new Item('id')

        and:
        def frame = [new WorkPackage(resource, item, 1.0f)]

        when:
        def result = ResourceScheduleCreator.create([frame])

        then:
        result[resource] == [item]
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
        def result = ResourceScheduleCreator.create([frame1, frame2, frame3])

        then:
        result[resource1] == [item1, item2, item2]
        result[resource2] == [null, item3, item4]
    }

}
