package lodgingreservation;


import java.util.Date;
import com.google.gson.JsonObject;

public class HotelReservation extends Reservation {
    private boolean kitchenette;

    public HotelReservation(String accountNumber, String reservationNumber,
            Address physicalAddress, Address mailingAddress,
            Date startDate, int numberOfNights, int numberOfBeds,
            int numberOfBedrooms, int numberOfBathrooms, int lodgingSize, boolean kitchenette) {
        super(accountNumber, reservationNumber, physicalAddress, mailingAddress,
                startDate, numberOfNights, numberOfBeds, numberOfBedrooms,
                numberOfBathrooms, lodgingSize);
        if (numberOfBedrooms != 1 || numberOfBathrooms != 1 || numberOfBeds != 2) {
            throw new IllegalArgumentException(
                    "Hotel reservations require one bedroom, one bathroom, and two beds.");
        }
        this.kitchenette = kitchenette;
    }

    /** Restore a validated persisted hotel without replaying lifecycle actions. */
    public HotelReservation(String json) { this(JsonData.object(json)); }

    HotelReservation(JsonObject data) {
        super(data);
        JsonData.type(data, "hotel");
        if (numberOfBeds != 2 || numberOfBedrooms != 1 || numberOfBathrooms != 1)
            throw new IllegalArgumentException("Hotel reservations require one bedroom, one bathroom, and two beds.");
        kitchenette = JsonData.bool(data, "kitchenette");
    }

    @Override
    public float calculatePricePerNight() {
        float nightly = 120.0f;
        if (lodgingSize > 900) nightly += 15.0f;
        nightly += 50.0f;
        if (kitchenette) nightly += 10.0f;
        return nightly;
    }

    public boolean getKitchenette() { return kitchenette; }

    @Override
    void copySubtypeFrom(Reservation validated) {
        kitchenette = ((HotelReservation) validated).kitchenette;
    }

    public void setKitchenette(boolean kitchenette) {
        requireEditable();
        this.kitchenette = kitchenette;
    }
    @Override
    public String toString() {
        JsonObject data = toJsonObject();
        data.addProperty("type", "hotel");
        data.addProperty("kitchenette", kitchenette);
        return data.toString();
    }
}
