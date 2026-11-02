package lodgingreservation;


import java.util.Date;
import com.google.gson.JsonObject;

public abstract class Reservation {
    protected String accountNumber;
    protected String reservationNumber;
    protected Address physicalAddress;
    protected Address mailingAddress;
    protected Date startDate;
    protected int numberOfNights;
    protected int numberOfBeds;
    protected int numberOfBedrooms;
    protected int numberOfBathrooms;
    protected int lodgingSize;
    protected float price;
    protected String status;

    public Reservation(String accountNumber, String reservationNumber,
            Address physicalAddress, Address mailingAddress,
            Date startDate, int numberOfNights, int numberOfBeds,
            int numberOfBedrooms, int numberOfBathrooms, int lodgingSize) {

        if (reservationNumber == null || reservationNumber.length() != 10) {
            throw new IllegalArgumentException(
                    "Reservation number must contain exactly 10 characters.");
        }
        if (physicalAddress == null || startDate == null) {
            throw new IllegalArgumentException(
                    "Physical address and start date are required.");
        }
        if (numberOfNights <= 0 || numberOfBeds <= 0 || numberOfBedrooms <= 0
                || numberOfBathrooms <= 0 || lodgingSize <= 0) {
            throw new IllegalArgumentException(
                    "Nights, beds, bedrooms, bathrooms, and lodging size must be positive.");
        }

        this.accountNumber = (accountNumber == null ? "-99" : accountNumber);
        this.reservationNumber = reservationNumber;
        this.physicalAddress = new Address(physicalAddress.getAddress());
        this.mailingAddress = mailingAddress == null ? null : new Address(mailingAddress.getAddress());
        this.startDate = new Date(startDate.getTime());
        this.numberOfNights = numberOfNights;
        this.numberOfBeds = numberOfBeds;
        this.numberOfBedrooms = numberOfBedrooms;
        this.numberOfBathrooms = numberOfBathrooms;
        this.lodgingSize = lodgingSize;
        this.price = 0.0f;
        this.status = "draft";
    }

    /** Common JSON constructor; concrete subclasses validate their own fields. */
    protected Reservation(JsonObject data) {
        this(JsonData.string(data, "accountNumber"), JsonData.string(data, "reservationNumber"),
                JsonData.address(data, "physicalAddress", false), JsonData.address(data, "mailingAddress", true),
                JsonData.date(data, "startDate"), JsonData.integer(data, "numberOfNights"),
                JsonData.integer(data, "numberOfBeds"), JsonData.integer(data, "numberOfBedrooms"),
                JsonData.integer(data, "numberOfBathrooms"), JsonData.integer(data, "lodgingSize"));
        JsonData.schema(data);
        String loadedStatus = JsonData.string(data, "status");
        if (!java.util.Set.of("draft", "completed", "cancelled").contains(loadedStatus))
            throw new IllegalArgumentException("Invalid reservation status.");
        float loadedPrice = JsonData.money(data, "price");
        if (!loadedStatus.equals("completed") && loadedPrice != 0.0f)
            throw new IllegalArgumentException("Draft and cancelled reservations must have zero price.");
        if (!accountNumber.equals("-99") && (accountNumber.length() != 10 || !accountNumber.startsWith("A")))
            throw new IllegalArgumentException("Invalid loaded account number.");
        if (!loadedStatus.equals("draft") && accountNumber.equals("-99"))
            throw new IllegalArgumentException("Final reservation must belong to an account.");
        price = loadedPrice;
        status = loadedStatus;
    }

    /** Type dispatch only; each subtype remains responsible for its own fields. */
    public static Reservation fromJson(String json) {
        JsonObject data = JsonData.object(json);
        switch (JsonData.string(data, "type")) {
            case "hotel": return new HotelReservation(data);
            case "cabin": return new CabinReservation(data);
            case "house": return new HouseReservation(data);
            default: throw new IllegalArgumentException("Unknown reservation type.");
        }
    }

    protected void requireEditable() {
        if (!"draft".equals(status) || startDate.before(new Date()))
            throw new IllegalStateException("Reservation cannot be changed in its current state.");
    }

    public void completeReservation() {
        if ("cancelled".equalsIgnoreCase(status)) {
            throw new IllegalOperationException("complete", accountNumber,
                    reservationNumber, "a cancelled reservation cannot be completed.");
        }
        if ("completed".equalsIgnoreCase(status)) {
            throw new IllegalOperationException("complete", accountNumber,
                    reservationNumber, "the reservation is already completed.");
        }

        if (!"draft".equals(status)) {
            throw new IllegalOperationException("complete", accountNumber, reservationNumber,
                    "only draft reservations can be completed.");
        }
        float total = calculateTotalPrice();
        if (!Float.isFinite(total) || total < 0.0f)
            throw new IllegalOperationException("complete", accountNumber, reservationNumber,
                    "the calculated total price is invalid.");
        price = total;
        status = "completed";
    }

    public void cancelReservation() {
        if ("completed".equalsIgnoreCase(status)) {
            throw new IllegalOperationException("cancel", accountNumber,
                    reservationNumber, "a completed reservation can never be cancelled.");
        }
        if ("cancelled".equalsIgnoreCase(status)) {
            throw new IllegalOperationException("cancel", accountNumber,
                    reservationNumber, "the reservation is already cancelled.");
        }
        if (!startDate.after(new Date())) {
            throw new IllegalOperationException("cancel", accountNumber,
                    reservationNumber, "the reservation start date is not in the future.");
        }

        status = "cancelled";
    }

    public abstract float calculatePricePerNight();

    /** Account supplies a fully validated, same-type private snapshot before any assignment. */
    final void applyUpdate(Reservation validated) {
        copySubtypeFrom(validated);
        physicalAddress = validated.physicalAddress;
        mailingAddress = validated.mailingAddress;
        startDate = validated.startDate;
        numberOfNights = validated.numberOfNights;
        numberOfBeds = validated.numberOfBeds;
        numberOfBedrooms = validated.numberOfBedrooms;
        numberOfBathrooms = validated.numberOfBathrooms;
        lodgingSize = validated.lodgingSize;
    }

    // Each concrete class copies only its own private subtype state.
    abstract void copySubtypeFrom(Reservation validated);

    protected JsonObject toJsonObject() {
        JsonObject data = new JsonObject();
        data.addProperty("schemaVersion", 1);
        data.addProperty("accountNumber", accountNumber);
        data.addProperty("reservationNumber", reservationNumber);
        data.add("physicalAddress", JsonData.object(physicalAddress.toString()));
        if (mailingAddress == null) data.add("mailingAddress", com.google.gson.JsonNull.INSTANCE);
        else data.add("mailingAddress", JsonData.object(mailingAddress.toString()));
        data.addProperty("startDate", startDate.toInstant().toString());
        data.addProperty("numberOfNights", numberOfNights);
        data.addProperty("numberOfBeds", numberOfBeds);
        data.addProperty("numberOfBedrooms", numberOfBedrooms);
        data.addProperty("numberOfBathrooms", numberOfBathrooms);
        data.addProperty("lodgingSize", lodgingSize);
        data.addProperty("price", price);
        data.addProperty("status", status);
        return data;
    }

    public float calculateTotalPrice() {
        float nightly = calculatePricePerNight();
        if (!Float.isFinite(nightly) || nightly < 0.0f)
            throw new IllegalStateException("Calculated nightly price must be finite and nonnegative.");
        return nightly * numberOfNights;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getReservationNumber() { return reservationNumber; }
    public Address getPhysicalAddress() { return new Address(physicalAddress.getAddress()); }
    public Address getMailingAddress() { return mailingAddress == null ? null : new Address(mailingAddress.getAddress()); }
    public Date getStartDate() { return new Date(startDate.getTime()); }
    public int getNumberOfNights() { return numberOfNights; }
    public int getNumberOfBeds() { return numberOfBeds; }
    public int getNumberOfBedrooms() { return numberOfBedrooms; }
    public int getNumberOfBathrooms() { return numberOfBathrooms; }
    public int getLodgingSize() { return lodgingSize; }
    public float getPrice() { return price; }
    public String getStatus() { return status; }

    public void setPhysicalAddress(Address value) {
        requireEditable();
        if (value == null) throw new IllegalArgumentException("Physical address is required.");
        physicalAddress = new Address(value.getAddress());
    }

    public void setMailingAddress(Address value) {
        requireEditable();
        mailingAddress = value == null ? null : new Address(value.getAddress());
    }

    public void setStartDate(Date value) {
        requireEditable();
        if (value == null) throw new IllegalArgumentException("Start date is required.");
        startDate = new Date(value.getTime());
    }

    public void setNumberOfNights(int value) {
        requireEditable();
        if (value <= 0) throw new IllegalArgumentException("numberOfNights must be positive.");
        numberOfNights = value;
    }

    public void setNumberOfBeds(int value) {
        requireEditable();
        if (value <= 0) throw new IllegalArgumentException("numberOfBeds must be positive.");
        if (this instanceof HotelReservation && value != 2)
            throw new IllegalArgumentException("Hotel reservations require one bedroom, one bathroom, and two beds.");
        numberOfBeds = value;
    }

    public void setNumberOfBedrooms(int value) {
        requireEditable();
        if (value <= 0) throw new IllegalArgumentException("numberOfBedrooms must be positive.");
        if (this instanceof HotelReservation && value != 1)
            throw new IllegalArgumentException("Hotel reservations require one bedroom, one bathroom, and two beds.");
        numberOfBedrooms = value;
    }

    public void setNumberOfBathrooms(int value) {
        requireEditable();
        if (value <= 0) throw new IllegalArgumentException("numberOfBathrooms must be positive.");
        if (this instanceof HotelReservation && value != 1)
            throw new IllegalArgumentException("Hotel reservations require one bedroom, one bathroom, and two beds.");
        numberOfBathrooms = value;
    }

    public void setLodgingSize(int value) {
        requireEditable();
        if (value <= 0) throw new IllegalArgumentException("lodgingSize must be positive.");
        lodgingSize = value;
    }
}
