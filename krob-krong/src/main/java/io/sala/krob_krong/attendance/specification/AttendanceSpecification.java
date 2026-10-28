package io.sala.krob_krong.attendance.specification;

import io.sala.krob_krong.attendance.entity.AttendanceEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class AttendanceSpecification {

    private AttendanceSpecification() {}

    public static Specification<AttendanceEntity> build(UUID search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null) {
                Predicate byStudentId = cb.equal(root.get("studentId"), search);
                Predicate byScheduleId = cb.equal(root.get("scheduleId"), search);
                //                Predicate byStatus = cb.equal(root.get("status"), search);
                predicates.add(cb.or(byStudentId, byScheduleId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
