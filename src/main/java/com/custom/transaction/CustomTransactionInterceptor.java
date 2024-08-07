package com.custom.transaction;

import com.soumakis.control.Try;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.util.ClassUtils;

/**
 * CustomTransactionInterceptor is a Spring AOP MethodInterceptor for managing transactions in
 * methods that return Try monad types. It extends TransactionAspectSupport to utilize its
 * transaction management functionalities.
 */
public class CustomTransactionInterceptor extends TransactionInterceptor {

  public CustomTransactionInterceptor(TransactionManager transactionManager,
      TransactionAttributeSource tas) {
    super(transactionManager, tas);
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // Work out the target class: may be {@code null}.
    // The TransactionAttributeSource should be passed the target class
    // as well as the method, which may be from an interface.
    Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(
        invocation.getThis()) : null);

    // Adapt to TransactionAspectSupport's invokeWithinTransaction...
    return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
  }

  /**
   * Invokes the method within a transaction and manages the transaction based on the return type of
   * the method. If the method returns a Try type, it will manage the transaction accordingly.
   * Otherwise, it will manage the transaction as usual.
   *
   * @param method      the Method being invoked
   * @param targetClass the target class that we're invoking the method on
   * @param invocation  the callback to use for proceeding with the target invocation
   * @return the result of the method invocation
   */
  @Override
  @Nullable
  protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
      final InvocationCallback invocation) {
    TransactionAttributeSource transactionAttributeSource = getTransactionAttributeSource();
    final TransactionAttribute transactionAttribute = (transactionAttributeSource != null)
        ? transactionAttributeSource.getTransactionAttribute(method, targetClass) : null;
    final TransactionManager transactionManager = determineTransactionManager(transactionAttribute);

    PlatformTransactionManager platformTransactionManager = asPlatformTransactionManager(
        transactionManager);
    final String joinpointIdentification = methodIdentification(method, targetClass,
        transactionAttribute);

    if (transactionAttribute == null
        || !(platformTransactionManager instanceof CallbackPreferringPlatformTransactionManager)) {
      return handleStandardTransaction(method, platformTransactionManager, transactionAttribute,
          joinpointIdentification, invocation);
    } else {
      return handleCallbackPreferringTransaction(
          method,
          (CallbackPreferringPlatformTransactionManager) platformTransactionManager,
          transactionAttribute, joinpointIdentification, invocation);
    }
  }

  /**
   * Handles a standard transaction by creating a transaction if necessary, proceeding with the
   * method invocation, evaluating the transaction, and cleaning up the transaction info.
   *
   * @param method                     the method being invoked
   * @param platformTransactionManager the transaction manager
   * @param transactionAttribute       the transaction attribute
   * @param joinpointIdentification    the identification of the joinpoint
   * @param invocation                 the callback to use for proceeding with the target
   *                                   invocation
   * @return the result of the method invocation
   */
  private Object handleStandardTransaction(
      Method method,
      PlatformTransactionManager platformTransactionManager,
      TransactionAttribute transactionAttribute,
      String joinpointIdentification, InvocationCallback invocation) {
    TransactionInfo txInfo = createTransactionIfNecessary(platformTransactionManager,
        transactionAttribute, joinpointIdentification);
    AtomicReference<Object> retVal = new AtomicReference<>();

    try {
      retVal.set(invocation.proceedWithInvocation());
      if (transactionAttribute != null) {
        return evaluateTransaction(txInfo, retVal, transactionAttribute);
      }
      // It means that no transaction is demarcated.
      return retVal;
    } catch (Throwable ex) {
      rollback(txInfo,
          ignored -> super.completeTransactionAfterThrowing(txInfo, ex));
      if (method.getReturnType().isAssignableFrom(Try.class)) {
        return Try.failure(ex);
      }
      throw new RuntimeException(ex);
    } finally {
      cleanupTransactionInfo(txInfo);
    }
  }

  /**
   * Handles a callback preferring transaction by executing the transaction, proceeding with the
   * method invocation, evaluating the transaction, and cleaning up the transaction info.
   *
   * @param method                     the Method being invoked
   * @param platformTransactionManager the transaction manager
   * @param transactionAttribute       the transaction attribute
   * @param joinpointIdentification    the identification of the joinpoint
   * @param invocation                 the callback to use for proceeding with the target
   *                                   invocation
   * @return the result of the method invocation
   */
  private Object handleCallbackPreferringTransaction(
      Method method,
      CallbackPreferringPlatformTransactionManager platformTransactionManager,
      TransactionAttribute transactionAttribute, String joinpointIdentification,
      InvocationCallback invocation) {
    try {
      return platformTransactionManager.execute(transactionAttribute, status -> {
        TransactionInfo txInfo = prepareTransactionInfo(platformTransactionManager,
            transactionAttribute, joinpointIdentification, status);
        try {
          return invocation.proceedWithInvocation();
        } catch (Throwable ex) {
          rollback(txInfo, ignored -> {
          });
          if (method.getReturnType().isAssignableFrom(Try.class)) {
            return Try.failure(ex);
          }
          throw new RuntimeException(ex);
        } finally {
          cleanupTransactionInfo(txInfo);
        }
      });
    } catch (TransactionSystemException ex) {
      return Try.failure(ex);
    }
  }

  /**
   * Evaluates the transaction by committing the transaction after returning and returning the
   * result of the method invocation.
   *
   * @param txInfo               the transaction info
   * @param retVal               the result of the method invocation
   * @param transactionAttribute the transaction attribute
   * @return the result of the method invocation
   */
  private Object evaluateTransaction(TransactionInfo txInfo,
      AtomicReference<Object> retVal, TransactionAttribute transactionAttribute) {
    TransactionStatus status = txInfo.getTransactionStatus();
    if (status != null && (retVal.get() instanceof Try<?>)) {
      retVal.set(evaluateTryFailure(retVal, transactionAttribute, status));
      //return Try.of(() -> commitTransaction(txInfo, retVal));
      try {
        return commitTransaction(txInfo, retVal);
      } catch (Exception e) {
        return Try.failure(e);
      }
    }
    return commitTransaction(txInfo, retVal);
  }

  private Object commitTransaction(TransactionInfo txInfo, AtomicReference<Object> retVal) {
    commitTransactionAfterReturning(txInfo);
    return retVal.get();
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    oos.writeObject(getTransactionManagerBeanName());
    oos.writeObject(getTransactionManager());
    oos.writeObject(getTransactionAttributeSource());
    oos.writeObject(getBeanFactory());
  }

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    setTransactionManagerBeanName((String) ois.readObject());
    setTransactionManager((PlatformTransactionManager) ois.readObject());
    setTransactionAttributeSource((TransactionAttributeSource) ois.readObject());
    setBeanFactory((BeanFactory) ois.readObject());
  }

  private PlatformTransactionManager asPlatformTransactionManager(
      @Nullable Object transactionManager) {
    if (transactionManager == null) {
      return null;
    }
    if (transactionManager instanceof PlatformTransactionManager ptm) {
      return ptm;
    } else {
      throw new IllegalStateException(
          "Specified transaction manager is not a PlatformTransactionManager: "
              + transactionManager);
    }
  }

  private String methodIdentification(Method method, @Nullable Class<?> targetClass,
      @Nullable TransactionAttribute transactionAttribute) {
    String methodIdentification = methodIdentification(method, targetClass);
    if (methodIdentification == null) {
      if (transactionAttribute instanceof DefaultTransactionAttribute dta) {
        methodIdentification = dta.getDescriptor();
      }
      if (methodIdentification == null) {
        methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
      }
    }
    return methodIdentification;
  }

  @SneakyThrows
  private void rollback(TransactionInfo txInfo, Consumer<?> rollbackAction) {
    if (txInfo.getTransactionStatus() != null) {
      rollbackAction.accept(null);
    }
  }

  private static Object evaluateTryFailure(AtomicReference<Object> retVal,
      TransactionAttribute txAttr,
      TransactionStatus status) {
    return ((Try<?>) retVal.get()).onFailure(ex -> {
      if (txAttr.rollbackOn(ex)) {
        status.setRollbackOnly();
      }
    });
  }
}
