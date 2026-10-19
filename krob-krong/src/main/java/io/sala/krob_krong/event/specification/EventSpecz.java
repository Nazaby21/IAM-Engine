package io.sala.krob_krong.event.specification;

import io.sala.krob_krong.event.entity.EventEntity;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

@UtilityClass
public class EventSpecz {
    public static Specification<EventEntity> schoolId(UUID schoolId) {
        return (root, query, cb) ->
                cb.equal(root.get(EventEntity.Fields.schoolId), schoolId);
    }

    public static Specification<EventEntity> title(String title) {
        return (root, query, cb) ->
                cb.equal(root.get(EventEntity.Fields.title), title);
    }

    public static Specification<EventEntity> eventDate(LocalDate eventDate) {
        return (root, query, cb) ->
                cb.equal(root.get(EventEntity.Fields.eventDate), eventDate);
    }

    public static Specification<EventEntity> bySchoolIdAndTitleAndEventDate(
            UUID schoolId,
            String title,
            LocalDate eventDate
    ) {
        return Specification.allOf(
                schoolId(schoolId),
                title(title),
                eventDate(eventDate)
        );
    }
}
