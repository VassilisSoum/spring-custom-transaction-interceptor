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
