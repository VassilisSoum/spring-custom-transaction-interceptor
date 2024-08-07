package com.custom.transaction.example.repository;

import com.custom.transaction.example.repository.entity.BookEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<BookEntity, Long> {

  Optional<BookEntity> findByIsbn(String isbn);

}
