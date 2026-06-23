package com.jsh.englishstudy.repository;

import com.jsh.englishstudy.entity.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
}