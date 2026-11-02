package lodgingreservation;


import java.util.Date;
import com.google.gson.JsonObject;

public class HouseReservation extends Reservation {
    private int numberOfFloors;

    public HouseReservation(String accountNumber, String reservationNumber,
            Address physicalAddress, Address mailingAddress,
            Date startDate, int numberOfNights, int numberOfBeds,
            int numberOfBedrooms, int numberOfBathrooms, int lodgingSize, int numberOfFloors) {
        super(accountNumber, reservationNumber, physicalAddress, mailingAddress,
                startDate, numberOfNights, numberOfBeds, numberOfBedrooms,
                numberOfBathrooms, lodgingSize);
        if (numberOfFloors <= 0) {
            throw new IllegalArgumentException("Number of floors must be positive.");
        }
        this.numberOfFloors = numberOfFloors;
    }

    /** Restore a validated persisted house without replaying lifecycle actions. */
    public HouseReservation(String json) { this(JsonData.object(json)); }

    HouseReservation(JsonObject data) {
        super(data);
        JsonData.type(data, "house");
        numberOfFloors = JsonData.integer(data, "numberOfFloors");
        if (numberOfFloors <= 0) throw new IllegalArgumentException("Number of floors must be positive.");
    }

    @Override
    public float calculatePricePerNight() {
        float nightly = 120.0f;
        if (lodgingSize > 900) nightly += 15.0f;
        return nightly;
    }

    public int getNumberOfFloors() { return numberOfFloors; }

    @Override
    void copySubtypeFrom(Reservation validated) {
        numberOfFloors = ((HouseReservation) validated).numberOfFloors;
    }

    public void setNumberOfFloors(int numberOfFloors) {
        requireEditable();
        if (numberOfFloors <= 0) {
            throw new IllegalArgumentException("Number of floors must be positive.");
        }
        this.numberOfFloors = numberOfFloors;
    }
    @Override
    public String toString() {
        JsonObject data = toJsonObject();
        data.addProperty("type", "house");
        data.addProperty("numberOfFloors", numberOfFloors);
        return data.toString();
    }
}
