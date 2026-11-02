package lodgingreservation;


import java.util.Date;
import com.google.gson.JsonObject;

public class CabinReservation extends Reservation {
    private boolean fullKitchen;
    private boolean loft;

    public CabinReservation(String accountNumber, String reservationNumber,
            Address physicalAddress, Address mailingAddress,
            Date startDate, int numberOfNights, int numberOfBeds,
            int numberOfBedrooms, int numberOfBathrooms, int lodgingSize, boolean fullKitchen, boolean loft) {
        super(accountNumber, reservationNumber, physicalAddress, mailingAddress,
                startDate, numberOfNights, numberOfBeds, numberOfBedrooms,
                numberOfBathrooms, lodgingSize);
        this.fullKitchen = fullKitchen;
        this.loft = loft;
    }

    /** Restore a validated persisted cabin without replaying lifecycle actions. */
    public CabinReservation(String json) { this(JsonData.object(json)); }

    CabinReservation(JsonObject data) {
        super(data);
        JsonData.type(data, "cabin");
        fullKitchen = JsonData.bool(data, "fullKitchen");
        loft = JsonData.bool(data, "loft");
    }

    @Override
    public float calculatePricePerNight() {
        float nightly = 120.0f;
        if (lodgingSize > 900) nightly += 15.0f;
        if (fullKitchen) nightly += 20.0f;
        if (numberOfBathrooms > 1) nightly += 5.0f * (numberOfBathrooms - 1);
        return nightly;
    }

    public boolean getFullKitchen() { return fullKitchen; }
    public boolean getLoft() { return loft; }

    @Override
    void copySubtypeFrom(Reservation validated) {
        CabinReservation cabin = (CabinReservation) validated;
        fullKitchen = cabin.fullKitchen;
        loft = cabin.loft;
    }

    public void setFullKitchen(boolean fullKitchen) {
        requireEditable();
        this.fullKitchen = fullKitchen;
    }

    public void setLoft(boolean loft) {
        requireEditable();
        this.loft = loft;
    }
    @Override
    public String toString() {
        JsonObject data = toJsonObject();
        data.addProperty("type", "cabin");
        data.addProperty("fullKitchen", fullKitchen);
        data.addProperty("loft", loft);
        return data.toString();
    }
}
