package com.jsh.englishstudy.repository;

import com.jsh.englishstudy.entity.Expression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpressionRepository extends JpaRepository<Expression, Long> {
    List<Expression> findByMaterialId(Long materialId);
    List<Expression> findByIsWrong(boolean isWrong);
    List<Expression> findByMaterialIdAndCategory(Long materialId, String category);
}