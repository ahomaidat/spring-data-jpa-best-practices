package com.atypon.springdatajpabestpractices;

import com.atypon.springdatajpabestpractices.entities.mappedByIssue.bad.PostBad;
import com.atypon.springdatajpabestpractices.entities.mappedByIssue.bad.PostCommentBad;
import com.atypon.springdatajpabestpractices.entities.mappedByIssue.good.PostGood;
import com.atypon.springdatajpabestpractices.entities.mappedByIssue.good.PostCommentGood;
import com.atypon.springdatajpabestpractices.util.SQLInterceptor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates why mappedBy is critical in @OneToMany associations.
 * WITHOUT mappedBy: Creates extra JOIN TABLE, inefficient SQL
 * WITH mappedBy: Uses FK directly, optimal SQL
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MappedByTest {

    @Autowired
    private EntityManager em;

    @BeforeEach
    void clearQueries() {
        SQLInterceptor.clear();
    }

    @Test
    @Order(0)
    @DisplayName("Show database tables created by Hibernate")
    void showCreatedTables() {
        // Query H2 metadata to show all tables
        @SuppressWarnings("unchecked")
        List<String> tables = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'"
        ).getResultList();

        System.out.println("\n=== TABLES CREATED BY HIBERNATE ===");
        System.out.println("-".repeat(50));
        for (String table : tables) {
            System.out.println("  - " + table);
        }
        System.out.println("-".repeat(50));

        // Show the JOIN TABLE exists for the "bad" version
        assertTrue(tables.stream().anyMatch(t -> t.equalsIgnoreCase("POST_BAD_COMMENTS")),
                "JOIN TABLE 'post_bad_comments' should exist for @OneToMany without mappedBy");

        // Show NO join table for the "good" version
        assertFalse(tables.stream().anyMatch(t -> t.equalsIgnoreCase("POST_GOOD_COMMENTS")),
                "NO join table should exist for @OneToMany with mappedBy");

        System.out.println("\nExpected tables for WITHOUT mappedBy:");
        System.out.println("  1. POST_BAD");
        System.out.println("  2. POST_COMMENT_BAD");
        System.out.println("  3. POST_BAD_COMMENTS  <-- EXTRA JOIN TABLE!");

        System.out.println("\nExpected tables for WITH mappedBy:");
        System.out.println("  1. POST_GOOD");
        System.out.println("  2. POST_COMMENT_GOOD");
        System.out.println("  (no join table - uses FK in POST_COMMENT_GOOD)\n");
    }

    @Test
    @Order(1)
    @DisplayName("BAD: WITHOUT mappedBy - INSERT creates JOIN TABLE rows")
    void withoutMappedBy_insert() {
        PostBad post = new PostBad();
        post.setTitle("Test Post");
        for (int i = 1; i <= 3; i++) {
            PostCommentBad comment = new PostCommentBad();
            comment.setReview("Comment " + i);
            post.addComment(comment);
        }

        SQLInterceptor.clear();
        em.persist(post);
        em.flush();

        SQLInterceptor.printQueries("BAD: WITHOUT mappedBy - INSERT 1 post + 3 comments");

        // 1 post + 3 comments + 3 join tables = 7 inserts
        assertEquals(7, SQLInterceptor.getInsertCount(),
                "Expected 7 inserts (1 post + 3 comments + 3 join table rows)");
    }

    @Test
    @Order(2)
    @DisplayName("GOOD: WITH mappedBy - INSERT uses FK directly")
    void withMappedBy_insert() {
        PostGood post = new PostGood();
        post.setTitle("Test Post");
        for (int i = 1; i <= 3; i++) {
            PostCommentGood comment = new PostCommentGood();
            comment.setReview("Comment " + i);
            post.addComment(comment);
        }

        SQLInterceptor.clear();
        em.persist(post);
        em.flush();

        SQLInterceptor.printQueries("GOOD: WITH mappedBy - INSERT 1 post + 3 comments");

        // 1 post + 3 comments = 4 inserts only
        assertEquals(4, SQLInterceptor.getInsertCount(),
                "Expected 4 inserts (1 post + 3 comments, no join table)");
    }

    @Test
    @Order(3)
    @DisplayName("BAD: WITHOUT mappedBy - REMOVE deletes all, re-inserts remaining")
    void withoutMappedBy_remove() {
        PostBad post = new PostBad();
        post.setTitle("Test");
        for (int i = 0; i < 5; i++) {
            PostCommentBad c = new PostCommentBad();
            c.setReview("Comment " + i);
            post.addComment(c);
        }
        em.persist(post);
        em.flush();
        em.clear();

        PostBad loaded = em.find(PostBad.class, post.getId());
        loaded.removeComment(loaded.getComments().getFirst());

        SQLInterceptor.clear();
        em.flush();

        SQLInterceptor.printQueries("BAD: WITHOUT mappedBy - REMOVE 1 of 5 comments");

        // DELETE all + RE-INSERT 4 = 1 delete + 4 inserts
        assertEquals(1, SQLInterceptor.getDeleteCount(), "Expected 1 delete");
        assertEquals(4, SQLInterceptor.getInsertCount(), "Expected 4 re-inserts");
    }

    @Test
    @Order(4)
    @DisplayName("GOOD: WITH mappedBy - REMOVE executes single DELETE")
    void withMappedBy_remove() {
        PostGood post = new PostGood();
        post.setTitle("Test");
        for (int i = 0; i < 5; i++) {
            PostCommentGood c = new PostCommentGood();
            c.setReview("Comment " + i);
            post.addComment(c);
        }
        em.persist(post);
        em.flush();
        em.clear();

        PostGood loaded = em.find(PostGood.class, post.getId());
        loaded.removeComment(loaded.getComments().getFirst());

        SQLInterceptor.clear();
        em.flush();

        SQLInterceptor.printQueries("GOOD: WITH mappedBy - REMOVE 1 of 5 comments");

        // Just 1 DELETE
        assertEquals(1, SQLInterceptor.getDeleteCount(), "Expected 1 delete");
        assertEquals(0, SQLInterceptor.getInsertCount(), "Expected 0 inserts");
    }

    @Test
    @Order(5)
    @DisplayName("SUMMARY: Side-by-side comparison")
    void summary() {
        System.out.println("""
            
            WHAT WE LEARNED
            ---------------
            
            Without mappedBy, Hibernate creates an extra join table (post_bad_comments)
            to manage the relationship. This causes:
            
              - 7 inserts instead of 4 when adding 3 comments
              - When removing 1 comment from 5: deletes ALL join table rows, 
                then re-inserts the remaining 4. That's 5 operations instead of 1.
              - With 10,000 comments, removing 1 would cause 10,000 operations!
            
            With mappedBy, Hibernate uses the foreign key column (post_id) directly
            in the child table. No extra table, no extra operations.
            
            
            THE FIX
            -------
            
            Change this:
            
                @OneToMany
                private List<PostComment> comments = new ArrayList<>();
            
            To this:
            
                @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
                private List<PostComment> comments = new ArrayList<>();
            
            The "mappedBy" tells Hibernate: the PostComment.post field owns this 
            relationship, so use its @JoinColumn instead of creating a join table.
            
            """);
    }
}