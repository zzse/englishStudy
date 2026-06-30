package com.jsh.englishstudy.repository;

import com.jsh.englishstudy.entity.StudyMaterial;
import com.jsh.englishstudy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findByUser(User user);
}