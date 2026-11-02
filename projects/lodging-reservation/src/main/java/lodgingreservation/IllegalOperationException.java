package lodgingreservation;


/**
 * Unchecked exception used when a reservation cannot be completed or cancelled.
 */
public class IllegalOperationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String operation;
    private String accountNumber;
    private String reservationNumber;
    private String details;

    public IllegalOperationException(String operation, String accountNumber,
            String reservationNumber, String details) {
        super("Cannot " + operation + " reservation " + reservationNumber
                + " for account " + accountNumber + ": " + details);
        this.operation = operation;
        this.accountNumber = accountNumber;
        this.reservationNumber = reservationNumber;
        this.details = details;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
