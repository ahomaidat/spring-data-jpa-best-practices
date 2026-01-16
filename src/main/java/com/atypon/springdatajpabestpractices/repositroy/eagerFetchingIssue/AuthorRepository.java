package com.atypon.springdatajpabestpractices.repositroy.eagerFetchingIssue;

import com.atypon.springdatajpabestpractices.entities.eagerFetchingIssue.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    // Even a simple findAll() will ALWAYS load all books AND articles
    // because of EAGER fetching - no way to prevent this!
    List<Author> findAll();

    // This query should only return authors, but EAGER collections
    // will still be loaded automatically
    @Query("SELECT a FROM Author a WHERE a.name LIKE %:name%")
    List<Author> findByNameContaining(String name);
}
