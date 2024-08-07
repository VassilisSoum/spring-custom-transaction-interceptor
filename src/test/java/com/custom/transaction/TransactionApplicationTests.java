package com.custom.transaction;

import com.custom.transaction.example.repository.BookRepository;
import com.custom.transaction.example.repository.entity.BookEntity;
import com.custom.transaction.example.service.BookService;
import com.soumakis.control.Try;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransactionApplicationTests {

  @Autowired
  private BookService bookService;

  @Autowired
  private BookRepository bookRepository;

  @Test
  void testAddBook() {

    var bookId = bookService.addBook(false);

    assert (bookId != null);

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isPresent());
    assert (addedBook.get().getId() != null);
    assert (addedBook.get().getIsbn().equals("1234567890"));
  }

  @Test
  void testAddBookWithExceptionAndRollback() {

    try {
      bookService.addBook(true);
    } catch (Exception e) {
      // Expected
    }

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isEmpty());
  }

  @Test
  void testAddBookWithNoRollbackException() {

    try {
      bookService.addBookWithNoRollbackException(true);
    } catch (Exception e) {
      // Expected
    }

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isPresent());
    assert (addedBook.get().getId() != null);
    assert (addedBook.get().getIsbn().equals("1234567890"));
  }

  @Test
  void testAddBookTry() {
    Try<Long> result = bookService.addBookTry(false);

    assert (result.isSuccess());
    assert (result.get() != null);

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isPresent());
    assert (addedBook.get().getId() != null);
    assert (addedBook.get().getIsbn().equals("1234567890"));
  }

  @Test
  void testBookTryWithExceptionAndRollback() {
    Try<Long> result = bookService.addBookTry(true);

    assert (result.isFailure());

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isEmpty());
  }

  @Test
  void testAddBookTryNoRollbackException() {
    Try<Long> result = bookService.addBookTryNoRollbackException(true);

    assert (result.isFailure());

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isPresent());
    assert (addedBook.get().getId() != null);
    assert (addedBook.get().getIsbn().equals("1234567890"));
  }

  @Test
  void testAlwaysThrowingException() {
    Try<Void> result = bookService.alwaysThrowingException();

    assert (result.isFailure());

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isEmpty());
  }

  @Test
  void testAlwaysThrowingExceptionWithNoRollback() {
    Try<Void> result = bookService.alwaysThrowingExceptionNoRollbackException();

    assert (result.isFailure());

    Optional<BookEntity> addedBook = bookRepository.findByIsbn("1234567890");

    assert (addedBook.isPresent());
    assert (addedBook.get().getId() != null);
    assert (addedBook.get().getIsbn().equals("1234567890"));
  }

}
