package kz.edu.sdu.advisor.service;

import kz.edu.sdu.advisor.model.CartItem;
import kz.edu.sdu.advisor.model.Course;
import kz.edu.sdu.advisor.model.Student;
import kz.edu.sdu.advisor.model.dto.CartDto;
import kz.edu.sdu.advisor.model.dto.CartItemDto;
import kz.edu.sdu.advisor.repository.CartItemRepository;
import kz.edu.sdu.advisor.repository.CourseRepository;
import kz.edu.sdu.advisor.repository.StudentRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    public static final int LIGHT_THRESHOLD = 15;
    public static final int OVERLOAD_THRESHOLD = 30;

    private final CartItemRepository cartItemRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Getter
    @Value("${advisor.cart.max-credits:40}")
    private int maxCredits = 40;

    @Transactional(readOnly = true)
    public CartDto getCart(String studentId) {
        List<CartItem> items = cartItemRepository.findByStudent_StudentIdOrderByCourse_CodeAsc(studentId);
        List<CartItemDto> itemDtos = items.stream()
                .map(item -> new CartItemDto(
                        item.getId(),
                        item.getCourse().getId(),
                        item.getCourse().getCode(),
                        item.getCourse().getName(),
                        item.getCourse().getCredits() != null ? item.getCourse().getCredits() : 0
                ))
                .toList();

        int totalCredits = itemDtos.stream().mapToInt(CartItemDto::credits).sum();
        String workloadState = getWorkloadState(totalCredits);
        String workloadColor = getWorkloadColor(totalCredits);

        return new CartDto(itemDtos, totalCredits, maxCredits, workloadState, workloadColor, itemDtos.isEmpty());
    }

    @Transactional(readOnly = true)
    public int getTotalCredits(String studentId) {
        return getCart(studentId).totalCredits();
    }

    @Transactional
    public CartDto addCourse(String studentId, Long courseId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (!cartItemRepository.existsByStudent_StudentIdAndCourse_Id(studentId, courseId)) {
            CartItem item = new CartItem(student, course);
            cartItemRepository.save(item);
            log.info("Course {} added to cart for student {}", course.getCode(), studentId);
        } else {
            log.debug("Course {} already in cart for student {}", course.getCode(), studentId);
        }

        return getCart(studentId);
    }

    @Transactional
    public CartDto removeCourse(String studentId, Long courseId) {
        cartItemRepository.deleteByStudent_StudentIdAndCourse_Id(studentId, courseId);
        log.info("Course ID {} removed from cart for student {}", courseId, studentId);
        return getCart(studentId);
    }

    public String getWorkloadState(int credits) {
        if (credits < LIGHT_THRESHOLD) {
            return "Light";
        } else if (credits < OVERLOAD_THRESHOLD) {
            return "Balanced";
        } else {
            return "Overload";
        }
    }

    public String getWorkloadColor(int credits) {
        if (credits < LIGHT_THRESHOLD) {
            return "green";
        } else if (credits < OVERLOAD_THRESHOLD) {
            return "yellow";
        } else {
            return "red";
        }
    }
}

