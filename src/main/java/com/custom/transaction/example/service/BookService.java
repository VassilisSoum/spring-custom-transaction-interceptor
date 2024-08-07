package com.custom.transaction.example.service;

import com.custom.transaction.example.repository.BookRepository;
import com.custom.transaction.example.repository.entity.BookEntity;
import com.soumakis.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

  private final BookRepository bookRepository;

  /**
   * Adds a book to the database. If an exception is thrown, the transaction will roll back.
   *
   * @param shouldThrowExceptionInTheEnd if true, an exception will be thrown in the end of the
   *                                     method
   * @return the id of the book that was added
   */
  @Transactional
  public Long addBook(boolean shouldThrowExceptionInTheEnd) {
    var book = new BookEntity();
    book.setAuthor("Author");
    book.setIsbn("1234567890");
    book.setPrice(100);
    book.setTitle("Title");

    bookRepository.save(book);
    if (shouldThrowExceptionInTheEnd) {
      throw new RuntimeException("Exception thrown intentionally");
    }
    return book.getId();
  }

  /**
   * Adds a book to the database. If an exception is thrown, the transaction will not roll back for
   * the specified exception.
   *
   * @param shouldThrowExceptionInTheEnd if true, an exception will be thrown in the end of the
   *                                     method
   */
  @Transactional(noRollbackFor = {IllegalStateException.class})
  public void addBookWithNoRollbackException(boolean shouldThrowExceptionInTheEnd) {
    var book = new BookEntity();
    book.setAuthor("Author");
    book.setIsbn("1234567890");
    book.setPrice(100);
    book.setTitle("Title");
    bookRepository.save(book);
    if (shouldThrowExceptionInTheEnd) {
      throw new IllegalStateException("Exception thrown intentionally");
    }
  }

  /**
   * Adds a book to the database. If an exception is thrown, the transaction will roll back and
   * return a Try#Failure. If no exception is thrown, it will return a Try#Success with the id of
   * the book.
   *
   * @param shouldReturnFailure if true, the method will return a Try#Failure with an exception
   * @return a Try with the id of the book
   */
  @Transactional
  public Try<Long> addBookTry(boolean shouldReturnFailure) {

    return Try.of(() -> {
      var book = new BookEntity();
      book.setAuthor("Author");
      book.setIsbn("1234567890");
      book.setPrice(100);
      book.setTitle("Title");
      bookRepository.save(book);
      if (shouldReturnFailure) {
        throw new RuntimeException("Exception thrown intentionally");
      }
      return book.getId();
    });
  }

  /**
   * Adds a book to the database. If an exception is thrown, the transaction will not roll back for
   * the specified exception and return a Try#Failure. If no exception is thrown, it will return a
   * Try#Success with the id of the book.
   *
   * @param shouldReturnFailure
   * @return
   */
  @Transactional(noRollbackFor = IllegalStateException.class)
  public Try<Long> addBookTryNoRollbackException(boolean shouldReturnFailure) {

    return Try.of(() -> {
      var book = new BookEntity();
      book.setAuthor("Author");
      book.setIsbn("1234567890");
      book.setPrice(100);
      book.setTitle("Title");

      bookRepository.save(book);
      if (shouldReturnFailure) {
        throw new IllegalStateException("Exception thrown intentionally");
      }
      return book.getId();
    });
  }

  @Transactional
  public Try<Void> alwaysThrowingException() {
    var book = new BookEntity();
    book.setAuthor("Author");
    book.setIsbn("1234567890");
    book.setPrice(100);
    book.setTitle("Title");

    bookRepository.save(book);
    throw new IllegalStateException();
  }

  @Transactional(noRollbackFor = IllegalStateException.class)
  public Try<Void> alwaysThrowingExceptionNoRollbackException() {
    var book = new BookEntity();
    book.setAuthor("Author");
    book.setIsbn("1234567890");
    book.setPrice(100);
    book.setTitle("Title");

    bookRepository.save(book);
    throw new IllegalStateException();
  }

}
