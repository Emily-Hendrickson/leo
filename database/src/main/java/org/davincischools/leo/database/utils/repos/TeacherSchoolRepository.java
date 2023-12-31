package org.davincischools.leo.database.utils.repos;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.List;
import org.davincischools.leo.database.daos.School;
import org.davincischools.leo.database.daos.Teacher;
import org.davincischools.leo.database.daos.TeacherSchool;
import org.davincischools.leo.database.daos.TeacherSchoolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherSchoolRepository extends JpaRepository<TeacherSchool, TeacherSchoolId> {

  default TeacherSchool upsert(Teacher teacher, School school) {
    checkNotNull(teacher);
    checkNotNull(school);

    return saveAndFlush(
        new TeacherSchool()
            .setCreationTime(Instant.now())
            .setId(new TeacherSchoolId().setTeacherId(teacher.getId()).setSchoolId(school.getId()))
            .setTeacher(teacher)
            .setSchool(school));
  }

  @Modifying
  @Query(
      "DELETE TeacherSchool ts"
          + " WHERE ts.teacher.id = (:teacherId)"
          + " AND NOT ts.school.id IN (:schoolIds)")
  void keepSchoolsForTeacher(
      @Param("teacherId") int teacherId, @Param("schoolIds") Iterable<Integer> schoolIdsToKeep);

  @Query("SELECT ts.school FROM TeacherSchool ts WHERE ts.teacher.id = (:teacherId)")
  List<School> findSchoolsByTeacherId(@Param("teacherId") int teacherId);
}
