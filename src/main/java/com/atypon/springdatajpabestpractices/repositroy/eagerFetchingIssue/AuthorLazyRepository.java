package com.atypon.springdatajpabestpractices.repositroy.eagerFetchingIssue;

import com.atypon.springdatajpabestpractices.entities.eagerFetchingIssue.AuthorLazy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorLazyRepository extends JpaRepository<AuthorLazy, Long> {

    // Simple findAll - only fetches authors, no collections loaded
    List<AuthorLazy> findAll();

    // When you NEED books, use JOIN FETCH - explicit and controlled
    @Query("SELECT DISTINCT a FROM AuthorLazy a JOIN FETCH a.books")
    List<AuthorLazy> findAllWithBooks();

    // When you NEED articles, use JOIN FETCH
    @Query("SELECT DISTINCT a FROM AuthorLazy a JOIN FETCH a.articles")
    List<AuthorLazy> findAllWithArticles();

    // EntityGraph alternative for books
    @EntityGraph(attributePaths = "books")
    @Query("SELECT a FROM AuthorLazy a")
    List<AuthorLazy> findAllWithBooksGraph();

    // You can even fetch both when needed (but do it explicitly!)
    @Query("SELECT DISTINCT a FROM AuthorLazy a JOIN FETCH a.books JOIN FETCH a.articles")
    List<AuthorLazy> findAllWithBooksAndArticles();
}
