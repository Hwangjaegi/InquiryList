package didim.inquiry.repository;

import didim.inquiry.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project , Long> {

    List<Project> findByUserId(Long userId);
    //생성일기준 내림차순정렬
    Page<Project> findByUserIdAndSubjectContainingOrderByCreatedAtDesc(Long userId , String search , Pageable pageable);
    Page<Project> findByUserIdOrderByCreatedAtDesc(Long userId , Pageable pageable);

    //페이징 없이 다른곳에서 쓰일때 사용
    List<Project> findByUserIdOrderByCreatedAtDesc(Long userId);
}
