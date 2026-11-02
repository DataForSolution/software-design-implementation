package lodgingreservation;


/**
 * Unchecked exception used when an account or reservation number already exists.
 */
public class DuplicateObjectException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String accountNumber;
    private String reservationNumber;

    /** Creates an exception for a duplicate account. */
    public DuplicateObjectException(String accountNumber) {
        super("Account " + accountNumber
                + " cannot be added because an account with that number already exists.");
        this.accountNumber = accountNumber;
        this.reservationNumber = null;
    }

    /** Creates an exception for a duplicate reservation within an account. */
    public DuplicateObjectException(String accountNumber, String reservationNumber) {
        super("Reservation " + reservationNumber + " cannot be added to account "
                + accountNumber + " because that reservation number already exists.");
        this.accountNumber = accountNumber;
        this.reservationNumber = reservationNumber;
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
