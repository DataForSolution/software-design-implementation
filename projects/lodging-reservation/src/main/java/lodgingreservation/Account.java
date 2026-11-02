package lodgingreservation;


import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class Account {
    private String accountNumber;
    private Address mailingAddress;
    private List<Reservation> reservations;
    private String emailAddress;
    private String phoneNumber;
    // Runtime association only; Manager owns the system-wide identity scope.
    private Manager manager;

    void attachManager(Manager owner) {
        if (manager != null && manager != owner)
            throw new IllegalArgumentException("Account is already managed by another Manager.");
        manager = owner;
    }

    public Account(String accountNumber, Address mailingAddress,
            String emailAddress, String phoneNumber) {
        if (accountNumber == null || accountNumber.length() != 10
                || !accountNumber.startsWith("A")) {
            throw new IllegalArgumentException(
                    "Account number must start with A and contain 10 characters.");
        }
        if (mailingAddress == null) {
            throw new IllegalArgumentException("Mailing address cannot be null.");
        }
        if (emailAddress == null || emailAddress.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or blank.");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank.");
        }

        this.accountNumber = accountNumber;
        this.mailingAddress = new Address(mailingAddress.getAddress());
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
        this.reservations = new ArrayList<Reservation>();
    }

    /** Load an account manifest; each reservation class parses its own JSON. */
    public Account(Path file) { this(JsonData.read(file), file.toAbsolutePath().getParent()); }

    public Account(String json, Path directory) {
        this(JsonData.object(json), directory);
    }

    private Account(JsonObject data, Path directory) {
        this(JsonData.string(data, "accountNumber"), JsonData.address(data, "mailingAddress", false),
                JsonData.string(data, "emailAddress"), JsonData.string(data, "phoneNumber"));
        JsonData.schema(data);
        if (directory == null) throw new IllegalArgumentException("Storage directory is required.");
        for (String filename : JsonData.strings(data, "reservationFiles")) {
            try {
                Path file = JsonData.child(directory, filename);
                Reservation loaded = Reservation.fromJson(JsonData.read(file).toString());
                if (!accountNumber.equals(loaded.getAccountNumber()))
                    throw new IllegalArgumentException("Reservation owner does not match account.");
                for (Reservation existing : reservations) {
                    if (existing.getReservationNumber().equalsIgnoreCase(loaded.getReservationNumber()))
                        throw new DuplicateObjectException(accountNumber, loaded.getReservationNumber());
                }
                reservations.add(loaded); // restore final states too; never invoke add/complete actions
            } catch (RuntimeException ex) {
                IllegalLoadException failure = new IllegalLoadException(accountNumber, "unknown", filename);
                failure.initCause(ex);
                throw failure;
            }
        }
    }

    public void updateAccount(Address mailingAddress, String phoneNumber, String emailAddress) {
        if (mailingAddress == null || phoneNumber == null || phoneNumber.isBlank()
                || emailAddress == null || emailAddress.isBlank()) {
            throw new IllegalArgumentException("Updated account values cannot be null or blank.");
        }
        this.mailingAddress = new Address(mailingAddress.getAddress());
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
    }

    public void addReservation(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
        }

        if (retrieveReservation(reservation.getReservationNumber()) != null) {
            throw new DuplicateObjectException(accountNumber,
                    reservation.getReservationNumber());
        }

        if (manager != null)
            manager.requireUniqueReservationNumber(accountNumber, reservation.getReservationNumber());
        if (!"draft".equals(reservation.getStatus()))
            throw new IllegalOperationException("add", accountNumber, reservation.getReservationNumber(),
                    "only draft reservations may be added.");
        String owner = reservation.getAccountNumber();
        if (!"-99".equals(owner) && !accountNumber.equalsIgnoreCase(owner))
            throw new IllegalOperationException("add", accountNumber, reservation.getReservationNumber(),
                    "reservation belongs to a different account.");

        // Requirement: account number is updated when reservation is added.
        reservation.accountNumber = accountNumber;
        reservations.add(reservation);
    }

    public Reservation retrieveReservation(String reservationNumber) {
        for (Reservation reservation : reservations) {
            if (reservation.getReservationNumber().equalsIgnoreCase(reservationNumber)) {
                return reservation;
            }
        }
        return null;
    }

    public void completeReservation(String reservationNumber) {
        Reservation reservation = retrieveReservation(reservationNumber);
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "No reservation exists for number " + reservationNumber + ".");
        }
        reservation.completeReservation();
    }

    public void cancelReservation(String reservationNumber) {
        Reservation reservation = retrieveReservation(reservationNumber);
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "No reservation exists for number " + reservationNumber + ".");
        }
        reservation.cancelReservation();
    }

    public void updateReservation(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
        }
        Reservation stored = retrieveReservation(reservation.getReservationNumber());
        if (stored == null) {
            throw new IllegalArgumentException(
                    "No reservation exists for number " + reservation.getReservationNumber() + ".");
        }
        if (!"draft".equals(stored.getStatus()) || !stored.getStartDate().after(new java.util.Date()))
            throw new IllegalOperationException("update", accountNumber, stored.getReservationNumber(),
                    "only future draft reservations may be updated.");
        if (!"draft".equals(reservation.getStatus()) || !reservation.getStartDate().after(new java.util.Date()))
            throw new IllegalOperationException("update", accountNumber, reservation.getReservationNumber(),
                    "replacement must be a future draft reservation.");
        if (!stored.getClass().equals(reservation.getClass()))
            throw new IllegalOperationException("update", accountNumber, reservation.getReservationNumber(),
                    "reservation type cannot change during update.");
        String owner = reservation.getAccountNumber();
        if (!"-99".equals(owner) && !accountNumber.equalsIgnoreCase(owner))
            throw new IllegalOperationException("update", accountNumber, reservation.getReservationNumber(),
                    "replacement belongs to a different account.");
        Reservation replacement = Reservation.fromJson(reservation.toString());
        // The private validated snapshot cannot alias the caller's mutable values.
        // Keep the original object, exact IDs, status and stored price per the SDD.
        stored.applyUpdate(replacement);
    }

    public float calculatePricePerNight(String reservationNumber) {
        Reservation reservation = retrieveReservation(reservationNumber);
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "No reservation exists for number " + reservationNumber + ".");
        }
        return reservation.calculatePricePerNight();
    }

    public float calculateTotalPrice(String reservationNumber) {
        Reservation reservation = retrieveReservation(reservationNumber);
        if (reservation == null) {
            throw new IllegalArgumentException(
                    "No reservation exists for number " + reservationNumber + ".");
        }
        return reservation.calculateTotalPrice();
    }

    private String reservationFileName(Reservation reservation) {
        return "reservation-" + JsonData.safeName(accountNumber) + "-"
                + JsonData.safeName(reservation.getReservationNumber()) + ".json";
    }

    public void save(Path directory) {
        if (directory == null) throw new IllegalArgumentException("Storage directory is required.");
        for (Reservation reservation : reservations) {
            String filename = reservationFileName(reservation);
            JsonData.write(directory.resolve(filename), reservation.toString());
        }
        JsonData.write(directory.resolve("account-" + JsonData.safeName(accountNumber) + ".json"), toString());
    }

    @Override
    public String toString() {
        JsonObject data = new JsonObject();
        data.addProperty("schemaVersion", 1);
        data.addProperty("accountNumber", accountNumber);
        data.add("mailingAddress", JsonData.object(mailingAddress.toString()));
        data.addProperty("emailAddress", emailAddress);
        data.addProperty("phoneNumber", phoneNumber);
        JsonArray files = new JsonArray();
        for (Reservation reservation : reservations) files.add(reservationFileName(reservation));
        data.add("reservationFiles", files);
        return data.toString();
    }

    public String getAccountNumber() { return accountNumber; }
    public Address getMailingAddress() { return new Address(mailingAddress.getAddress()); }
    public List<Reservation> getReservations() { return new ArrayList<Reservation>(reservations); }
    public String getEmailAddress() { return emailAddress; }
    public String getPhoneNumber() { return phoneNumber; }

    public void setMailingAddress(Address mailingAddress) {
        if (mailingAddress == null) {
            throw new IllegalArgumentException("Mailing address cannot be null.");
        }
        this.mailingAddress = new Address(mailingAddress.getAddress());
    }

    public void setEmailAddress(String emailAddress) {
        if (emailAddress == null || emailAddress.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or blank.");
        }
        this.emailAddress = emailAddress;
    }

    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be null or blank.");
        }
        this.phoneNumber = phoneNumber;
    }
}
