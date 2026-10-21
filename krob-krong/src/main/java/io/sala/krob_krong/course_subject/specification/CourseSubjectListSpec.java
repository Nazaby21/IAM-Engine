package io.sala.krob_krong.course_subject.specification;

import io.sala.krob_krong.course.entity.CourseEntity;
import io.sala.krob_krong.course_subject.entity.CourseSubjectEntity;
import io.sala.krob_krong.subject.entity.SubjectEntity;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseSubjectListSpec {

    public static Specification<CourseSubjectEntity> byCourseId(UUID courseId) {
        return (root, query, cb) ->
                cb.equal(root.get(CourseSubjectEntity.Fields.id).get("courseId"), courseId);
    }

    public static Specification<CourseSubjectEntity> byStatus(String status) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(CourseSubjectEntity.Fields.status)), status.toLowerCase(Locale.ROOT));
    }

    public static Specification<CourseSubjectEntity> courseAndSubjectNotDeleted() {
        return (root, query, cb) -> {
            var courseSub = query.subquery(UUID.class);
            var c = courseSub.from(CourseEntity.class);
            courseSub
                    .select(c.get("id"))
                    .where(cb.equal(c.get("id"), root.get("id").get("courseId")), cb.isNull(c.get("deletedAt")));

            var subjectSub = query.subquery(UUID.class);
            var s = subjectSub.from(SubjectEntity.class);
            subjectSub
                    .select(s.get("id"))
                    .where(cb.equal(s.get("id"), root.get("id").get("subjectId")), cb.isNull(s.get("deletedAt")));

            return cb.and(cb.exists(courseSub), cb.exists(subjectSub));
        };
    }
}
