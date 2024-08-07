package com.custom.transaction.example;

import com.custom.transaction.CustomTransactionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration
@EnableTransactionManagement
public class ExampleConfig {

  @Bean
  public TransactionAttributeSource transactionAttributeSource() {
    return new AnnotationTransactionAttributeSource();
  }

  @Bean
  public CustomTransactionInterceptor transactionInterceptorCustomizer(
      TransactionManager transactionManager, TransactionAttributeSource transactionAttributeSource) {
    
    return new CustomTransactionInterceptor(transactionManager,
        transactionAttributeSource);
  }

  @Bean
  public TransactionInterceptor transactionInterceptor(
      CustomTransactionInterceptor customTransactionInterceptor) {
    return customTransactionInterceptor;
  }

}
