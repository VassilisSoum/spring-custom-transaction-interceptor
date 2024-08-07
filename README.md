# Custom TransactionInterceptor for Spring Boot

This repository contains a custom TransactionInterceptor for Spring Boot. The TransactionInterceptor
is a Spring AOP interceptor that intercepts all methods annotated with the `@Transactional`
annotation.

The TransactionInterceptor is a custom implementation of
the `org.aopalliance.intercept.MethodInterceptor` interface. It is used to intercept method
invocations and execute custom logic before and after the method invocation.

In this example we use the custom TransactionInterceptor to handle transaction management for
the [com.soumakis.control.Try](https://github.com/VassilisSoum/FunctionalUtils/blob/master/src/main/java/com/soumakis/control/Try.java)
monad to be able to express exceptions as types in the method signature.

However, the custom TransactionInterceptor can be used to handle any custom return type for a method
annotated with the `@Transactional` annotation.

## Steps required

1. Create a class extending the `TransactionInterceptor` class. Take a look
   at `com.custom.transaction.CustomTransactionInterceptor`
2. Define the beans as in `com.custom.transaction.example.ExampleConfig`
3. In `application.properties` or `application.yml` allow overriding spring beans by
   setting `spring.main.allow-bean-definition-overriding=true`