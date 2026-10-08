package kz.edu.sdu.advisor.repository;

import kz.edu.sdu.advisor.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByStudent_StudentIdOrderByCourse_CodeAsc(String studentId);

    Optional<CartItem> findByStudent_StudentIdAndCourse_Id(String studentId, Long courseId);

    boolean existsByStudent_StudentIdAndCourse_Id(String studentId, Long courseId);

    void deleteByStudent_StudentIdAndCourse_Id(String studentId, Long courseId);
}

