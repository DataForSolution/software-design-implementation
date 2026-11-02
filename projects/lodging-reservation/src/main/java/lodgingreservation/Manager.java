package lodgingreservation;


import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.stream.Stream;

public class Manager {
    private List<Account> accounts;
    private Path storageDirectory;

    /** Load account manifests from ./data; an absent directory is an empty installation. */
    public Manager() { this(Path.of("data")); }

    // Loaded accounts only retain this reference; attachManager never calls back.
    // Neither Manager nor its accounts are published until construction succeeds.
    @SuppressWarnings("this-escape")
    public Manager(Path directory) {
        if (directory == null) throw new IllegalArgumentException("Storage directory is required.");
        accounts = new ArrayList<Account>();
        storageDirectory = directory;
        if (Files.notExists(directory)) return;
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path file : entries.filter(p -> p.getFileName().toString().startsWith("account-")
                    && p.getFileName().toString().endsWith(".json")).sorted().toList()) {
                try {
                    Account loaded = new Account(file);
                    for (Account existing : accounts) {
                        if (existing.getAccountNumber().equalsIgnoreCase(loaded.getAccountNumber()))
                            throw new DuplicateObjectException(loaded.getAccountNumber());
                    }
                    for (Reservation reservation : loaded.getReservations()) {
                        if (reservationNumberExists(reservation.getReservationNumber()))
                            throw new DuplicateObjectException(loaded.getAccountNumber(),
                                    reservation.getReservationNumber());
                    }
                    loaded.attachManager(this);
                    accounts.add(loaded);
                } catch (IllegalLoadException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    IllegalLoadException failure = new IllegalLoadException("unknown", file.toString());
                    failure.initCause(ex);
                    throw failure;
                }
            }
        } catch (IOException ex) {
            IllegalLoadException failure = new IllegalLoadException("unknown", directory.toString());
            failure.initCause(ex);
            throw failure;
        }
    }

    private boolean reservationNumberExists(String number) {
        if (number == null) return false;
        for (Account account : accounts)
            if (account.retrieveReservation(number) != null) return true;
        return false;
    }

    void requireUniqueReservationNumber(String accountNumber, String number) {
        if (reservationNumberExists(number))
            throw new DuplicateObjectException(accountNumber, number);
    }

    public List<Account> retrieveAccounts() {
        return new ArrayList<Account>(accounts);
    }

    public Account retrieveAccount(String accountNumber) {
        for (Account account : accounts) {
            if (account.getAccountNumber().equalsIgnoreCase(accountNumber)) {
                return account;
            }
        }
        return null;
    }

    // Overloaded account lookup.
    public Account retrieveAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }
        return retrieveAccount(account.getAccountNumber());
    }

    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null.");
        }

        if (retrieveAccount(account.getAccountNumber()) != null) {
            throw new DuplicateObjectException(account.getAccountNumber());
        }

        for (Reservation reservation : account.getReservations()) {
            if (reservationNumberExists(reservation.getReservationNumber()))
                throw new DuplicateObjectException(account.getAccountNumber(),
                        reservation.getReservationNumber());
        }
        account.attachManager(this);
        accounts.add(account);
    }

    public void updateAccountFiles(String accountNumber) {
        Account account = retrieveAccount(accountNumber);
        if (account == null)
            throw new IllegalArgumentException("No account exists for number " + accountNumber + ".");
        account.save(storageDirectory);
    }

    public void addReservation(String accountNumber, Reservation reservation) {
        Account account = retrieveAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("No account exists for number " + accountNumber + ".");
        }
        if (reservation == null) throw new IllegalArgumentException("Reservation cannot be null.");
        account.addReservation(reservation);
    }

    public void completeReservation(String accountNumber, String reservationNumber) {
        Account account = retrieveAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("No account exists for number " + accountNumber + ".");
        }
        account.completeReservation(reservationNumber);
    }

    public void cancelReservation(String accountNumber, String reservationNumber) {
        Account account = retrieveAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("No account exists for number " + accountNumber + ".");
        }
        account.cancelReservation(reservationNumber);
    }

    public void updateReservation(String accountNumber, Reservation reservation) {
        Account account = retrieveAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("No account exists for number " + accountNumber + ".");
        }
        account.updateReservation(reservation);
    }

    public float calculatePricePerNight(String accountNumber, String reservationNumber) {
        Account account = retrieveAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("No account exists for number " + accountNumber + ".");
        }
        return account.calculatePricePerNight(reservationNumber);
    }

    public float calculateTotalPrice(String accountNumber, String reservationNumber) {
        Account account = retrieveAccount(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("No account exists for number " + accountNumber + ".");
        }
        return account.calculateTotalPrice(reservationNumber);
    }
}
