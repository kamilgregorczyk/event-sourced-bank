package com.kgregorczyk.bank.aggregates;

/**
 * Exception that's thrown when {@link AccountAggregate} cannot be debited.
 */
class BalanceTooLowException extends RuntimeException {

}
