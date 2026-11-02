package lodgingreservation;


/**
 * Unchecked exception used when required account or reservation data cannot be loaded.
 */
public class IllegalLoadException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private String accountNumber;
    private String reservationNumber;
    private String fileName;
    private String loadType;

    /** Creates an exception for a missing account file. */
    public IllegalLoadException(String accountNumber, String fileName) {
        super("Unable to load account file '" + fileName
                + "' for account " + accountNumber + ".");
        this.accountNumber = accountNumber;
        this.fileName = fileName;
        this.loadType = "account";
        this.reservationNumber = null;
    }

    /** Creates an exception for a missing reservation file. */
    public IllegalLoadException(String accountNumber, String reservationNumber, String fileName) {
        super("Unable to load reservation file '" + fileName
                + "' for reservation " + reservationNumber
                + " in account " + accountNumber + ".");
        this.accountNumber = accountNumber;
        this.reservationNumber = reservationNumber;
        this.fileName = fileName;
        this.loadType = "reservation";
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
