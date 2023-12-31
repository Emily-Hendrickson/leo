package org.davincischools.leo.database.utils.repos;

import java.util.List;
import org.davincischools.leo.database.daos.ProjectPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectPostRepository extends JpaRepository<ProjectPost, Integer> {

  @Query(
      "SELECT pp"
          + " FROM ProjectPost pp"
          + " INNER JOIN FETCH pp.userX"
          + " INNER JOIN pp.project p"
          + " INNER JOIN p.projectInput"
          + " WHERE p.id = (:projectId)")
  List<ProjectPost> findAllByProjectId(@Param("projectId") int projectId);
}
