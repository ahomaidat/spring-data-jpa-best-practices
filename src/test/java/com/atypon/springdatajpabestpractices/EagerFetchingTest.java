package com.atypon.springdatajpabestpractices;

import com.atypon.springdatajpabestpractices.entities.eagerFetchingIssue.*;
import com.atypon.springdatajpabestpractices.repositroy.eagerFetchingIssue.AuthorLazyRepository;
import com.atypon.springdatajpabestpractices.repositroy.eagerFetchingIssue.AuthorRepository;
import com.atypon.springdatajpabestpractices.util.SQLInterceptor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EAGER Fetching Problem: When you mark a collection as FetchType.EAGER,
 * Hibernate ALWAYS loads it, even when you don't need it.  @ManyToOne and @OneToOne default to EAGER in JPA spec.
 *
 * Problems:
 * 1. Cartesian Product - Multiple EAGER collections multiply result rows
 * 2. Always loads data - No way to prevent fetching, wastes resources
 * 3. Cannot override - Unlike LAZY, you can't skip loading when not needed
 * 4. N+1 still happens - EAGER doesn't prevent N+1 with multiple parents!
 */
@SpringBootTest
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EagerFetchingTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorLazyRepository authorLazyRepository;

    @BeforeEach
    void setup() {
        // Create EAGER author with books and articles
        for (int i = 1; i <= 3; i++) {
            Author author = new Author();
            author.setName("Eager Author " + i);

            for (int j = 1; j <= 2; j++) {
                Book book = new Book();
                book.setTitle("Book " + j + " by Author " + i);
                author.addBook(book);
            }

            for (int k = 1; k <= 2; k++) {
                Article article = new Article();
                article.setTitle("Article " + k + " by Author " + i);
                author.addArticle(article);
            }

            em.persist(author);
        }

        // Create LAZY author with books and articles
        for (int i = 1; i <= 3; i++) {
            AuthorLazy author = new AuthorLazy();
            author.setName("Lazy Author " + i);

            for (int j = 1; j <= 2; j++) {
                BookLazy book = new BookLazy();
                book.setTitle("Book " + j + " by Author " + i);
                author.addBook(book);
            }

            for (int k = 1; k <= 2; k++) {
                ArticleLazy article = new ArticleLazy();
                article.setTitle("Article " + k + " by Author " + i);
                author.addArticle(article);
            }

            em.persist(author);
        }

        em.flush();
        em.clear();
        SQLInterceptor.clear();
    }

    @Test
    @Order(1)
    @DisplayName("BAD: EAGER always loads collections even when not needed")
    void eagerAlwaysLoadsCollections() {
        SQLInterceptor.clear();

        // We ONLY want the author names, but EAGER loads EVERYTHING
        List<Author> authors = authorRepository.findAll();

        // We're just printing names - but books AND articles were loaded!
        for (Author author : authors) {
            System.out.println("Author: " + author.getName());
        }

        SQLInterceptor.printQueries("BAD: EAGER - Always loads books and articles");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("\nWe only needed author names, but Hibernate executed " + selectCount + " queries!");
        System.out.println("Expected: 1 query for authors + 3 for books + 3 for articles = 7 queries");
        System.out.println("Books and articles were loaded unnecessarily.\n");

        // EAGER causes N+1: 1 for authors + N for books + N for articles = 7 queries
        // (3 authors, so 1 + 3 + 3 = 7)
        assertEquals(7, selectCount, "EAGER: Expected 1 + 3 + 3 = 7 queries (N+1 for both collections)");
    }

    @Test
    @Order(2)
    @DisplayName("GOOD: LAZY only loads what you need")
    void lazyOnlyLoadsWhenNeeded() {
        SQLInterceptor.clear();

        // With LAZY, we only get what we ask for
        List<AuthorLazy> authors = authorLazyRepository.findAll();

        // Just printing names - no collections loaded!
        for (AuthorLazy author : authors) {
            System.out.println("Author: " + author.getName());
        }

        SQLInterceptor.printQueries("GOOD: LAZY - Only fetches authors");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("\nWith LAZY fetching: only " + selectCount + " query for authors.");
        System.out.println("Books and articles are NOT loaded until accessed.");
        System.out.println("Compare to EAGER which would execute 7 queries!\n");

        assertEquals(1, selectCount, "LAZY: Should only execute 1 query");
        assertEquals(3, authors.size(), "Should have 3 authors");
    }

    @Test
    @Order(3)
    @DisplayName("GOOD: LAZY with JOIN FETCH when you need collections")
    void lazyWithJoinFetchWhenNeeded() {
        SQLInterceptor.clear();

        // When we NEED books, we explicitly fetch them
        List<AuthorLazy> authors = authorLazyRepository.findAllWithBooks();

        // Now we can access books without N+1
        int totalBooks = 0;
        for (AuthorLazy author : authors) {
            totalBooks += author.getBooks().size();
            System.out.println("Author: " + author.getName() +
                    " has " + author.getBooks().size() + " books");
        }

        SQLInterceptor.printQueries("GOOD: LAZY + JOIN FETCH for books");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("\nWith JOIN FETCH: only " + selectCount + " query that includes books.");
        System.out.println("Articles are NOT loaded because we don't need them.");
        System.out.println("This is the FLEXIBILITY that LAZY provides!\n");

        assertEquals(1, selectCount, "JOIN FETCH: Should execute only 1 query");
        assertEquals(3, authors.size(), "Should have 3 authors");
        assertEquals(6, totalBooks, "Should have 6 books total (2 per author)");
    }

    @Test
    @Order(4)
    @DisplayName("BAD: EAGER causes N+1 for EACH collection")
    void eagerCausesMultipleNPlusOne() {
        SQLInterceptor.clear();

        // Fetch all authors - EAGER will load both collections with N+1 pattern
        List<Author> authors = authorRepository.findAll();

        // Count total books and articles loaded
        int totalBooks = 0;
        int totalArticles = 0;
        for (Author author : authors) {
            totalBooks += author.getBooks().size();
            totalArticles += author.getArticles().size();
        }

        SQLInterceptor.printQueries("BAD: EAGER with multiple collections");

        int selectCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("\nAuthors: " + authors.size());
        System.out.println("Books: " + totalBooks);
        System.out.println("Articles: " + totalArticles);
        System.out.println("Total SELECT queries: " + selectCount);

        System.out.println("""

            With multiple EAGER collections, Hibernate executes:
            - 1 query for authors
            - N queries for books (one per author)
            - N queries for articles (one per author)

            For 3 authors: 1 + 3 + 3 = 7 queries!
            For 1000 authors: 1 + 1000 + 1000 = 2001 queries!

            This is N+1 problem MULTIPLIED by the number of EAGER collections.
            """);

        // Verify the N+1 pattern: 1 + N + N = 7 queries for 3 authors
        assertEquals(7, selectCount, "EAGER: Expected 1 + 3 + 3 = 7 queries");
        assertEquals(3, authors.size(), "Should have 3 authors");
        assertEquals(6, totalBooks, "Should have 6 books (2 per author)");
        assertEquals(6, totalArticles, "Should have 6 articles (2 per author)");
    }

    @Test
    @Order(5)
    @DisplayName("COMPARISON: EAGER vs LAZY query counts")
    void comparisonEagerVsLazy() {
        // Test EAGER - just getting author names
        SQLInterceptor.clear();
        List<Author> eagerAuthors = authorRepository.findAll();
        for (Author author : eagerAuthors) {
            author.getName(); // Only accessing name
        }
        int eagerQueryCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        // Test LAZY - just getting author names
        SQLInterceptor.clear();
        List<AuthorLazy> lazyAuthors = authorLazyRepository.findAll();
        for (AuthorLazy author : lazyAuthors) {
            author.getName(); // Only accessing name
        }
        int lazyQueryCount = (int) SQLInterceptor.getQueries().stream()
                .filter(q -> q.startsWith("select"))
                .count();

        System.out.println("\n========================================");
        System.out.println("COMPARISON: Just fetching author names");
        System.out.println("========================================");
        System.out.println("EAGER queries: " + eagerQueryCount + " (loads books AND articles)");
        System.out.println("LAZY queries:  " + lazyQueryCount + " (only loads authors)");
        System.out.println("Difference:    " + (eagerQueryCount - lazyQueryCount) + " unnecessary queries!");
        System.out.println("========================================\n");

        assertEquals(7, eagerQueryCount, "EAGER: 1 + 3 + 3 = 7 queries");
        assertEquals(1, lazyQueryCount, "LAZY: Only 1 query");
        assertTrue(eagerQueryCount > lazyQueryCount,
                "EAGER executes more queries than LAZY for the same simple operation");
    }

    @Test
    @Order(6)
    @DisplayName("SUMMARY: Why EAGER fetching is bad")
    void summary() {
        System.out.println("""

            WHY IS EAGER FETCHING BAD?
            ==========================

            1. ALWAYS LOADS DATA
            --------------------
            Even when you only need the parent entity, EAGER loads
            all associated collections. This wastes:
            - Database resources (unnecessary queries)
            - Network bandwidth (transferring unused data)
            - Memory (storing objects you don't need)


            2. CARTESIAN PRODUCT PROBLEM
            ----------------------------
            With multiple EAGER collections:

            @OneToMany(fetch = FetchType.EAGER)
            private List<Book> books;

            @OneToMany(fetch = FetchType.EAGER)
            private List<Article> articles;

            Hibernate may create a Cartesian product:
            - Author with 10 books and 10 articles
            - Result: 10 * 10 = 100 rows for 1 author!


            3. CANNOT BE OVERRIDDEN
            -----------------------
            LAZY -> EAGER: Easy with JOIN FETCH
            EAGER -> LAZY: IMPOSSIBLE at query time

            You lose flexibility and control.


            4. N+1 STILL HAPPENS
            --------------------
            EAGER doesn't prevent N+1! When you load multiple
            parents, each may trigger separate collection queries.


            BEST PRACTICE:
            ==============

            ALWAYS use LAZY fetching (the default for @OneToMany)
            and use JOIN FETCH or @EntityGraph when you need data.

            // Entity - keep it LAZY
            @OneToMany(mappedBy = "author")  // LAZY is default
            private List<Book> books;

            // Repository - fetch when needed
            @Query("SELECT a FROM Author a JOIN FETCH a.books")
            List<Author> findAllWithBooks();

            """);
    }
}
