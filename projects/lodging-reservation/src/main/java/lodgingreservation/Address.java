package lodgingreservation;


public class Address {
    /** JSON overload avoids collision with the existing plain-address String constructor. */
    public Address(com.google.gson.JsonObject data) { this(JsonData.string(data, "address")); }

    private String address;

    public Address(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or blank.");
        }
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or blank.");
        }
        this.address = address;
    }
    @Override
    public String toString() {
        com.google.gson.JsonObject data = new com.google.gson.JsonObject();
        data.addProperty("address", address);
        return data.toString();
    }
}
