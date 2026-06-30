package com.jsh.englishstudy.repository;

import com.jsh.englishstudy.entity.Expression;
import com.jsh.englishstudy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ExpressionRepository extends JpaRepository<Expression, Long> {
    List<Expression> findByStudyMaterialIdAndCategory(Long studyMaterialId, String category);
    List<Expression> findByUserAndWrong(User user, boolean wrong);
    @Modifying
    @Transactional
    @Query("delete from Expression e where e.studyMaterial.id = :materialId")
    void deleteByStudyMaterialId(@Param("materialId") Long materialId);

    @Query("""
    select distinct e.dialogueTitle
    from Expression e
    where e.studyMaterial.id = :materialId
      and e.category = 'DIALOGUE'
      and e.dialogueTitle is not null""")
    List<String> findDistinctDialogueTitles(@Param("materialId") Long materialId);

    List<Expression> findByStudyMaterialIdAndCategoryAndDialogueTitle(
            Long studyMaterialId,
            String category,
            String dialogueTitle
    );

}