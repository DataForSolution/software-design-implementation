package lodgingreservation;

import com.google.gson.JsonObject;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** Standalone scenario-based verification harness covering major domain behavior. */
public final class Test {
    private static int checks, scenarios, failures;
    private static Path temp;
    private Test() { }
    @FunctionalInterface private interface Scenario { void run() throws Exception; }
    private static void run(String name, Scenario scenario) {
        int before = checks;
        scenarios++;
        try {
            scenario.run();
            System.out.println("PASS " + name + " (" + (checks - before) + " checks)");
        } catch (Exception | AssertionError ex) {
            failures++;
            System.err.println("FAIL " + name + ": " + ex);
            ex.printStackTrace(System.err);
        }
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
    private static <T extends Throwable> T fails(Class<T> type, Runnable action) {
        try { action.run(); }
        catch (Throwable ex) {
            if (ex.getClass() != type) throw new AssertionError("Expected exactly " + type.getName(), ex);
            checks++;
            return type.cast(ex);
        }
        throw new AssertionError("Expected " + type.getName());
    }
    private static void unchanged(Reservation r, Class<? extends Throwable> type, Runnable action) {
        String before = r.toString();
        fails(type, action);
        check(before.equals(r.toString()), "failed operation preserves every field");
    }
    private static Date future() { return Date.from(Instant.parse("2099-01-01T00:00:00Z")); }
    private static Date past() { return Date.from(Instant.parse("2000-01-01T00:00:00Z")); }
    private static Address address() { return new Address("10 Café Road, \"Unit A\"\n東京"); }
    private static Account account(String id) { return new Account(id, address(), "student@example.invalid", "555-0100"); }
    private static HotelReservation hotel(String id) { return new HotelReservation(null, id, address(), null, future(), 3, 2, 1, 1, 400, true); }
    private static CabinReservation cabin(String id) { return new CabinReservation(null, id, address(), address(), future(), 2, 3, 2, 3, 1000, true, true); }
    private static HouseReservation house(String id) { return new HouseReservation(null, id, address(), address(), future(), 4, 4, 3, 2, 1000, 2); }
    private static List<Reservation> types() { return List.of(hotel("R000000001"), cabin("R000000002"), house("R000000003")); }
    private static Path directory(String name) throws Exception { return Files.createDirectory(temp.resolve(name)); }
    private static Path manifest(Path dir, Account a) { return dir.resolve("account-" + JsonData.safeName(a.getAccountNumber()) + ".json"); }
    private static void same(Reservation before, Reservation after) {
        check(after != null && before.getClass() == after.getClass(), "subtype preserved");
        check(JsonData.object(before.toString()).equals(JsonData.object(after.toString())), "all serialized fields preserved");
        check(before.getAccountNumber().equals(after.getAccountNumber()), "owner preserved");
        check(before.getReservationNumber().equals(after.getReservationNumber()), "identity preserved");
        check(before.getStatus().equals(after.getStatus()) && before.getPrice() == after.getPrice(), "status and stored price preserved");
        check(before.calculateTotalPrice() == after.calculateTotalPrice(), "pricing preserved");
    }
    private static void testAccountCreationAndValidation() {
        Account a = account("A000000001");
        check(a.getAccountNumber().equals("A000000001") && a.getReservations().isEmpty(), "initial account state");
        check(a.getMailingAddress().getAddress().equals(address().getAddress()) && a.getEmailAddress().equals("student@example.invalid") && a.getPhoneNumber().equals("555-0100"), "account fields");
        for (String id : new String[]{null, "", "A1", "B000000001", "a000000001", "A0000000001"}) fails(IllegalArgumentException.class, () -> account(id));
        fails(IllegalArgumentException.class, () -> new Account("A000000001", null, "e", "p"));
        for (String value : new String[]{null, "", " "}) {
            fails(IllegalArgumentException.class, () -> new Account("A000000001", address(), value, "p"));
            fails(IllegalArgumentException.class, () -> new Account("A000000001", address(), "e", value));
        }
    }
    private static void testDuplicateAccounts() throws Exception {
        Manager m = new Manager(directory("duplicate-accounts"));
        Account a = account("A000000001"); m.addAccount(a);
        fails(DuplicateObjectException.class, () -> m.addAccount(account(a.getAccountNumber())));
        check(m.retrieveAccounts().size() == 1 && m.retrieveAccount(a) == a, "duplicate leaves graph unchanged");
        fails(IllegalArgumentException.class, () -> m.addAccount(null));
    }
    private static void testHotelCreation() {
        HotelReservation r = hotel("R000000001");
        check(r.getKitchenette() && r.getNumberOfBeds() == 2 && r.getNumberOfBedrooms() == 1 && r.getNumberOfBathrooms() == 1, "hotel fields");
        check(r.getAccountNumber().equals("-99") && r.getStatus().equals("draft") && r.getPrice() == 0 && r.getMailingAddress() == null, "draft initialization");
        same(r, new HotelReservation(r.toString()));
        r.setKitchenette(false); check(!r.getKitchenette(), "hotel setter");
        for (int[] n : new int[][]{{1,1,1}, {2,2,1}, {2,1,2}}) fails(IllegalArgumentException.class, () -> new HotelReservation(null, "R000000001", address(), null, future(), 1, n[0], n[1], n[2], 400, false));
    }
    private static void testCabinCreation() {
        CabinReservation r = cabin("R000000002");
        check(r.getFullKitchen() && r.getLoft() && r.getNumberOfNights() == 2 && r.getNumberOfBeds() == 3 && r.getNumberOfBedrooms() == 2 && r.getNumberOfBathrooms() == 3 && r.getLodgingSize() == 1000, "cabin fields");
        same(r, new CabinReservation(r.toString()));
        r.setFullKitchen(false); r.setLoft(false); check(!r.getFullKitchen() && !r.getLoft(), "cabin setters");
    }
    private static void testHouseCreation() {
        HouseReservation r = house("R000000003"); check(r.getNumberOfFloors() == 2, "house floors"); same(r, new HouseReservation(r.toString()));
        r.setNumberOfFloors(5); check(r.getNumberOfFloors() == 5, "floor setter");
        for (int n : new int[]{0,-1}) {
            unchanged(r, IllegalArgumentException.class, () -> r.setNumberOfFloors(n));
            fails(IllegalArgumentException.class, () -> new HouseReservation(null, "R000000003", address(), null, future(), 1, 1, 1, 1, 900, n));
        }
    }
    private static void testAddingReservations() throws Exception {
        Manager m = new Manager(directory("adding")); Account a = account("A000000001"); m.addAccount(a);
        for (Reservation r : types()) {
            m.addReservation(a.getAccountNumber(), r);
            check(a.retrieveReservation(r.getReservationNumber()) == r && r.getAccountNumber().equals(a.getAccountNumber()), "add retains object and assigns owner");
        }
        a.getReservations().clear(); check(a.getReservations().size() == 3, "list structural copy");
        Reservation owned = hotel("R000000004"); account("A000000002").addReservation(owned);
        unchanged(owned, IllegalOperationException.class, () -> a.addReservation(owned)); check(a.getReservations().size() == 3, "foreign owner not appended");
        for (boolean completed : new boolean[]{false,true}) {
            Reservation r = hotel("R000000005"); if (completed) r.completeReservation(); else r.cancelReservation();
            unchanged(r, IllegalOperationException.class, () -> a.addReservation(r));
        }
        fails(IllegalArgumentException.class, () -> a.addReservation(null));
        fails(IllegalArgumentException.class, () -> m.addReservation("missing", hotel("R000000006")));
        fails(IllegalArgumentException.class, () -> m.addReservation(a.getAccountNumber(), null));
    }
    private static void testSystemWideDuplicates() throws Exception {
        Path dir = directory("global-duplicates"); Manager m = new Manager(dir);
        Account a = account("A000000001"), b = account("A000000002"); m.addAccount(a); m.addAccount(b);
        m.addReservation(a.getAccountNumber(), hotel("R000000001"));
        for (String id : new String[]{"R000000001", "r000000001"}) {
            Reservation candidate = hotel(id);
            unchanged(candidate, DuplicateObjectException.class, () -> m.addReservation(b.getAccountNumber(), candidate));
            unchanged(candidate, DuplicateObjectException.class, () -> b.addReservation(candidate));
            check(b.getReservations().isEmpty(), "both entry paths reject globally");
            Account prefilled = account("A000000003"); prefilled.addReservation(hotel(id));
            fails(DuplicateObjectException.class, () -> m.addAccount(prefilled));
            check(m.retrieveAccounts().size() == 2 && m.retrieveAccount(prefilled) == null, "prefilled duplicate account rejected");
        }
        unchanged(a.getReservations().get(0), DuplicateObjectException.class, () -> b.addReservation(a.getReservations().get(0)));
        m.updateAccountFiles(a.getAccountNumber()); m.updateAccountFiles(b.getAccountNumber());
        Manager restarted = new Manager(dir); Account restoredB = restarted.retrieveAccount(b.getAccountNumber());
        fails(DuplicateObjectException.class, () -> restoredB.addReservation(hotel("r000000001")));
        check(restoredB.getReservations().isEmpty(), "guard restored on reload");
        Account isolated = account("A000000004"); isolated.addReservation(hotel("r000000001")); isolated.save(dir);
        check(fails(IllegalLoadException.class, () -> new Manager(dir)).getCause() instanceof DuplicateObjectException, "loaded cross-account duplicate precise cause");
        Path local = directory("local-duplicates"); a.save(local); JsonObject data = JsonData.read(manifest(local,a));
        data.getAsJsonArray("reservationFiles").add(data.getAsJsonArray("reservationFiles").get(0));
        Files.writeString(manifest(local,a), data.toString());
        check(fails(IllegalLoadException.class, () -> new Manager(local)).getCause() instanceof DuplicateObjectException, "repeated manifest reference rejected");
        Manager separate = new Manager(directory("independent-manager"));
        Account independent = account("A000000005"); independent.addReservation(hotel("R000000001")); separate.addAccount(independent);
        check(separate.retrieveAccounts().size() == 1, "independent systems have no static registry");
        fails(IllegalArgumentException.class, () -> separate.addAccount(b));
        check(separate.retrieveAccounts().size() == 1, "account cannot silently switch managers");
    }
    private static void testRetrievingAccounts() throws Exception {
        Manager m = new Manager(directory("lookup")); Account a = account("A000000001"); m.addAccount(a);
        check(m.retrieveAccount("a000000001") == a && m.retrieveAccount(account(a.getAccountNumber())) == a, "both lookup overloads");
        check(m.retrieveAccount("missing") == null && m.retrieveAccount((String)null) == null, "unsuccessful lookups null");
        m.retrieveAccounts().clear(); check(m.retrieveAccounts().size() == 1, "account list structural copy");
        fails(IllegalArgumentException.class, () -> m.retrieveAccount((Account)null)); fails(IllegalArgumentException.class, () -> new Manager(null));
        Path absent = temp.resolve("absent");
        check(new Manager(absent).retrieveAccounts().isEmpty() && !Files.exists(absent), "empty install no write");
        check(new Manager().retrieveAccounts() != null, "default constructor");
    }
    private static void testRetrievingReservations() {
        Account a = account("A000000001"); Reservation r = hotel("R000000001"); a.addReservation(r);
        check(a.retrieveReservation("r000000001") == r && a.retrieveReservation("missing") == null && a.retrieveReservation(null) == null, "reservation lookups");
        for (Runnable action : List.<Runnable>of(() -> a.completeReservation("missing"), () -> a.cancelReservation("missing"), () -> a.calculatePricePerNight("missing"), () -> a.calculateTotalPrice("missing"))) fails(IllegalArgumentException.class, action);
    }
    private static void testUpdatingAccountInformation() {
        Account a = account("A000000001"); Reservation r = hotel("R000000001"); a.addReservation(r);
        Address value = new Address("New address"); a.updateAccount(value, "new phone", "new email");
        check(a.getPhoneNumber().equals("new phone") && a.getEmailAddress().equals("new email"), "contact update");
        check(a.getAccountNumber().equals("A000000001") && a.retrieveReservation(r.getReservationNumber()) == r, "identity and graph preserved");
        value.setAddress("alias"); check(a.getMailingAddress().getAddress().equals("New address"), "input copied");
        a.setMailingAddress(address()); a.setPhoneNumber("555-0200"); a.setEmailAddress("updated@example.invalid");
        check(a.getMailingAddress().getAddress().equals(address().getAddress()) && a.getPhoneNumber().equals("555-0200") && a.getEmailAddress().equals("updated@example.invalid"), "all setters");
        String before = a.toString();
        for (Runnable action : List.<Runnable>of(() -> a.updateAccount(null,"p","e"), () -> a.updateAccount(address()," ","e"), () -> a.updateAccount(address(),"p",null), () -> a.setMailingAddress(null), () -> a.setPhoneNumber(""), () -> a.setEmailAddress(null))) {
            fails(IllegalArgumentException.class, action); check(before.equals(a.toString()), "failed contact update atomic");
        }
        a.getMailingAddress().setAddress("alias"); check(before.equals(a.toString()), "output copied");
    }
    private static void testUpdatingReservations() throws Exception {
        Path dir = directory("updates"); Manager m = new Manager(dir); Account a = account("A000000001"); m.addAccount(a);
        List<Reservation> originals = types(); for (Reservation r : originals) a.addReservation(r);
        for (Reservation original : originals) {
            List<String> others = originals.stream().filter(r -> r != original).map(Reservation::toString).toList();
            Reservation candidate = Reservation.fromJson(original.toString());
            candidate.setPhysicalAddress(new Address("Updated lodging")); candidate.setMailingAddress(null);
            candidate.setStartDate(Date.from(Instant.parse("2099-02-01T00:00:00Z"))); candidate.setNumberOfNights(7); candidate.setLodgingSize(901);
            if (candidate instanceof HotelReservation h) h.setKitchenette(false);
            if (candidate instanceof CabinReservation c) { c.setFullKitchen(false); c.setLoft(false); c.setNumberOfBeds(4); c.setNumberOfBedrooms(3); c.setNumberOfBathrooms(2); }
            if (candidate instanceof HouseReservation h) { h.setNumberOfFloors(5); h.setNumberOfBeds(5); h.setNumberOfBedrooms(4); h.setNumberOfBathrooms(3); }
            m.updateReservation(a.getAccountNumber(), candidate);
            check(a.retrieveReservation(original.getReservationNumber()) == original, "update preserves stored object identity");
            same(candidate, original);
            check(others.equals(originals.stream().filter(r -> r != original).map(Reservation::toString).toList()), "other reservations unchanged");
            candidate.setNumberOfNights(9); check(original.getNumberOfNights() == 7, "candidate not retained as alias");
            a.updateReservation(original); check(original.getNumberOfNights() == 7, "self update works");
        }
        JsonObject lowercase = JsonData.object(originals.get(0).toString()); lowercase.addProperty("reservationNumber", "r000000001");
        a.updateReservation(Reservation.fromJson(lowercase.toString()));
        check(originals.get(0).getReservationNumber().equals("R000000001"), "case-insensitive update preserves exact ID");
        m.updateAccountFiles(a.getAccountNumber()); Account loaded = new Manager(dir).retrieveAccount(a);
        for (Reservation r : originals) same(r, loaded.retrieveReservation(r.getReservationNumber()));
    }
    private static void testCompletingReservations() throws Exception {
        Manager m = new Manager(directory("completion")); Account a = account("A000000001"); m.addAccount(a);
        for (Reservation r : types()) {
            a.addReservation(r); float expected = r.calculateTotalPrice(); m.completeReservation(a.getAccountNumber(), r.getReservationNumber());
            check(r.getStatus().equals("completed") && r.getPrice() == expected, "completion stores total"); same(r, Reservation.fromJson(r.toString()));
        }
        Reservation pastDraft = hotel("R000000004"); pastDraft.setStartDate(past()); pastDraft.completeReservation();
        check(pastDraft.getStatus().equals("completed"), "no invented completion date guard");
    }
    private static void testInvalidCompletionAttempts() {
        for (Reservation r : types()) {
            r.completeReservation(); unchanged(r, IllegalOperationException.class, r::completeReservation);
            Reservation c = hotel(r.getReservationNumber()); c.cancelReservation(); unchanged(c, IllegalOperationException.class, c::completeReservation);
        }
    }
    private static void testCancellingReservations() throws Exception {
        Manager m = new Manager(directory("cancellation")); Account a = account("A000000001"); m.addAccount(a);
        for (Reservation r : types()) {
            a.addReservation(r); float before = r.getPrice(); m.cancelReservation(a.getAccountNumber(), r.getReservationNumber());
            check(r.getStatus().equals("cancelled") && r.getPrice() == before, "cancellation preserves draft price"); same(r, Reservation.fromJson(r.toString()));
        }
    }
    private static void testInvalidCancellationAttempts() {
        for (Reservation r : types()) {
            r.completeReservation(); unchanged(r, IllegalOperationException.class, r::cancelReservation);
            Reservation c = hotel(r.getReservationNumber()); c.cancelReservation(); unchanged(c, IllegalOperationException.class, c::cancelReservation);
            Reservation p = hotel(r.getReservationNumber()); p.setStartDate(past()); unchanged(p, IllegalOperationException.class, p::cancelReservation);
        }
    }
    private static void testHotelPricing() throws Exception {
        HotelReservation r = hotel("R000000001"); check(r.calculatePricePerNight() == 180 && r.calculateTotalPrice() == 540, "hotel 180/540");
        r.setKitchenette(false); check(r.calculatePricePerNight() == 170, "no kitchenette");
        Manager m = new Manager(directory("delegated-pricing")); Account a = account("A000000001"); m.addAccount(a); a.addReservation(r);
        check(m.calculatePricePerNight(a.getAccountNumber(), r.getReservationNumber()) == 170 && m.calculateTotalPrice(a.getAccountNumber(), r.getReservationNumber()) == 510, "Manager/Account delegates");
        check(r.getPrice() == 0 && r.getStatus().equals("draft"), "calculation no mutation");
    }
    private static void testCabinPricing() {
        CabinReservation r = cabin("R000000002"); check(r.calculatePricePerNight() == 165 && r.calculateTotalPrice() == 330, "cabin 165/330");
        r.setLoft(false); check(r.calculatePricePerNight() == 165, "no loft charge");
        r.setFullKitchen(false); check(r.calculatePricePerNight() == 145, "no kitchen charge");
        r.setNumberOfBathrooms(1); check(r.calculatePricePerNight() == 135, "first bath included");
    }
    private static void testHousePricing() {
        HouseReservation r = house("R000000003"); check(r.calculatePricePerNight() == 135 && r.calculateTotalPrice() == 540, "house 135/540");
        r.setNumberOfFloors(8); check(r.calculatePricePerNight() == 135, "no floor charge");
    }
    private static void testPricingBoundaries() {
        for (int size : new int[]{899,900,901}) {
            float base = size > 900 ? 135 : 120;
            for (boolean kitchen : new boolean[]{false,true}) {
                HotelReservation h = hotel("R000000001"); h.setLodgingSize(size); h.setKitchenette(kitchen);
                check(h.calculatePricePerNight() == base + 50 + (kitchen ? 10 : 0), "hotel size boundary");
                for (int baths : new int[]{1,2,3}) {
                    CabinReservation c = cabin("R000000002"); c.setLodgingSize(size); c.setFullKitchen(kitchen); c.setNumberOfBathrooms(baths);
                    float expected = base + (kitchen ? 20 : 0) + 5 * (baths - 1);
                    check(c.calculatePricePerNight() == expected && c.calculateTotalPrice() == expected * 2, "cabin boundary and total");
                    c.setLoft(false); check(c.calculatePricePerNight() == expected, "no loft boundary charge");
                }
            }
            for (int floors : new int[]{1,5}) {
                HouseReservation h = house("R000000003"); h.setLodgingSize(size); h.setNumberOfFloors(floors);
                check(h.calculatePricePerNight() == base && h.calculateTotalPrice() == base * 4, "house boundary and total");
            }
        }
    }
    private static void testSavingFiles() throws Exception {
        Path dir = directory("saving"); Manager m = new Manager(dir); Account a = account("A000000001"); m.addAccount(a);
        List<Reservation> values = types(); for (Reservation r : values) a.addReservation(r); m.updateAccountFiles(a.getAccountNumber());
        check(Files.isRegularFile(manifest(dir,a)) && JsonData.read(manifest(dir,a)).equals(JsonData.object(a.toString())), "manifest creation/data");
        for (String name : JsonData.strings(JsonData.read(manifest(dir,a)), "reservationFiles")) {
            check(Files.isRegularFile(dir.resolve(name)), "reservation file creation");
            check(Files.readString(dir.resolve(name)).contains("Café"), "actual UTF-8 data");
        }
        a.setEmailAddress("changed@example.invalid"); values.get(0).setNumberOfNights(5); m.updateAccountFiles(a.getAccountNumber());
        Account loaded = new Manager(dir).retrieveAccount(a);
        check(loaded.getEmailAddress().equals(a.getEmailAddress()) && loaded.retrieveReservation("R000000001").getNumberOfNights() == 5, "account/reservation overwrite");
        try (var entries = Files.list(dir)) { check(entries.count() == 4, "no temporary write files"); }
        fails(IllegalArgumentException.class, () -> a.save(null)); fails(IllegalArgumentException.class, () -> m.updateAccountFiles("missing"));
        Path blocked = temp.resolve("blocked"); Files.writeString(blocked, "not a directory"); fails(UncheckedIOException.class, () -> a.save(blocked));
    }
    private static void testLoadingFiles() throws Exception {
        Path dir = directory("loading"); Account a = account("A000000001"); for (Reservation r : types()) a.addReservation(r); a.save(dir);
        Account file = new Account(manifest(dir,a)), json = new Account(a.toString(), dir);
        check(file.getReservations().size() == 3 && json.getReservations().size() == 3, "both account restore constructors");
        for (Reservation r : a.getReservations()) { same(r, file.retrieveReservation(r.getReservationNumber())); same(r, json.retrieveReservation(r.getReservationNumber())); }
        String first = JsonData.strings(JsonData.read(manifest(dir,a)), "reservationFiles").get(0);
        Files.delete(dir.resolve(first)); check(fails(IllegalLoadException.class, () -> new Manager(dir)).getCause() != null, "missing reservation retains cause");
        a.save(dir); Files.writeString(dir.resolve(first), "not JSON"); fails(IllegalLoadException.class, () -> new Manager(dir));
        a.save(dir); Files.writeString(manifest(dir,a), "{bad}"); fails(IllegalLoadException.class, () -> new Manager(dir));
        a.save(dir); JsonObject wrong = JsonData.read(dir.resolve(first)); wrong.addProperty("accountNumber", "A000000002");
        Files.writeString(dir.resolve(first), wrong.toString()); fails(IllegalLoadException.class, () -> new Manager(dir));
        a.save(dir); Files.copy(manifest(dir,a), dir.resolve("account-copy.json"));
        check(fails(IllegalLoadException.class, () -> new Manager(dir)).getCause() instanceof DuplicateObjectException, "loaded duplicate account rejected");
        Path blocked = temp.resolve("file-not-directory"); Files.writeString(blocked, "block"); fails(IllegalLoadException.class, () -> new Manager(blocked));
        fails(UncheckedIOException.class, () -> new Account(temp.resolve("missing-account.json")));
    }
    private static void testRoundTripConsistency() throws Exception {
        Path dir = directory("round-trip"); Account a = account("A000000001"); for (Reservation r : types()) a.addReservation(r); a.save(dir);
        Account loaded = new Manager(dir).retrieveAccount(a);
        check(JsonData.object(a.toString()).equals(JsonData.object(loaded.toString())), "all account fields/manifest order");
        for (Reservation r : a.getReservations()) same(r, loaded.retrieveReservation(r.getReservationNumber()));
        check(new Address(JsonData.object(address().toString())).getAddress().equals(address().getAddress()), "address JSON Unicode/escaping");
        Path expected = Path.of("lib/gson-2.13.2.jar").toRealPath();
        Path actual = Path.of(JsonObject.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
        check(expected.equals(actual), "real Gson loaded only from project JAR");
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(expected)));
        check(hash.equals("dd0ce1b55a3ed2080cb70f9c655850cda86c206862310009dcb5e5c95265a5e0"), "verified Gson 2.13.2 bytes");
    }
    private static void testCustomExceptions() throws Exception {
        for (RuntimeException ex : List.of(new IllegalLoadException("A000000001", "account.json"),
                new IllegalLoadException("A000000001", "R000000001", "reservation.json"), new DuplicateObjectException("A000000001"),
                new DuplicateObjectException("A000000001", "R000000001"), new IllegalOperationException("cancel", "A000000001", "R000000001", "reason")))
            check(ex.getMessage().contains("A000000001") && ex.toString().equals(ex.getMessage()), "exception context/format");
        Manager m = new Manager(directory("exceptions"));
        for (Runnable action : List.<Runnable>of(() -> m.completeReservation("missing", "R000000001"), () -> m.cancelReservation("missing", "R000000001"),
                () -> m.updateReservation("missing", hotel("R000000001")), () -> m.calculatePricePerNight("missing", "R000000001"),
                () -> m.calculateTotalPrice("missing", "R000000001"))) fails(IllegalArgumentException.class, action);
    }
    private static void testInvalidInputValidation() {
        for (String v : new String[]{null, "", " "}) fails(IllegalArgumentException.class, () -> new Address(v));
        Address a = address(); String before = a.toString(); fails(IllegalArgumentException.class, () -> a.setAddress(" "));
        check(a.toString().equals(before), "invalid address no mutation"); a.setAddress("valid"); check(a.getAddress().equals("valid"), "address setter");
        for (String id : new String[]{null, "", "R1", "R0000000001"}) fails(IllegalArgumentException.class, () -> hotel(id));
        fails(IllegalArgumentException.class, () -> new CabinReservation(null, "R000000001", null, null, future(), 1, 1, 1, 1, 1, false, false));
        fails(IllegalArgumentException.class, () -> new CabinReservation(null, "R000000001", address(), null, null, 1, 1, 1, 1, 1, false, false));
        for (int v : new int[]{0,-1}) {
            for (int index = 0; index < 5; index++) {
                int[] n = {1,1,1,1,1}; n[index] = v;
                fails(IllegalArgumentException.class, () -> new CabinReservation(null, "R000000001", address(), null, future(), n[0], n[1], n[2], n[3], n[4], false, false));
            }
            Reservation r = cabin("R000000002");
            for (Runnable action : List.<Runnable>of(() -> r.setNumberOfNights(v), () -> r.setNumberOfBeds(v), () -> r.setNumberOfBedrooms(v), () -> r.setNumberOfBathrooms(v), () -> r.setLodgingSize(v))) unchanged(r, IllegalArgumentException.class, action);
        }
        HotelReservation h = hotel("R000000001");
        for (Runnable action : List.<Runnable>of(() -> h.setNumberOfBeds(1), () -> h.setNumberOfBedrooms(2), () -> h.setNumberOfBathrooms(2), () -> h.setPhysicalAddress(null), () -> h.setStartDate(null))) unchanged(h, IllegalArgumentException.class, action);
        for (String json : new String[]{null, "{bad}", "{} {}", "{\"x\":1,\"x\":2}", "[]"}) fails(IllegalArgumentException.class, () -> Reservation.fromJson(json));
        for (String field : new String[]{"numberOfNights", "numberOfBeds", "numberOfBedrooms", "numberOfBathrooms", "lodgingSize", "schemaVersion"}) {
            JsonObject data = JsonData.object(h.toString()); data.addProperty(field, 0); fails(IllegalArgumentException.class, () -> Reservation.fromJson(data.toString()));
            data.addProperty(field, "1"); fails(IllegalArgumentException.class, () -> Reservation.fromJson(data.toString()));
        }
        for (String field : new String[]{"type", "status", "startDate"}) {
            JsonObject data = JsonData.object(h.toString()); data.addProperty(field, "invalid"); fails(IllegalArgumentException.class, () -> Reservation.fromJson(data.toString()));
        }
        for (Reservation r : types()) {
            r.getStartDate().setTime(0); r.getPhysicalAddress().setAddress("alias");
            check(r.getStartDate().equals(future()) && r.getPhysicalAddress().getAddress().equals(address().getAddress()), "mutable outputs copied"); r.completeReservation();
            for (Runnable action : List.<Runnable>of(() -> r.setPhysicalAddress(address()), () -> r.setMailingAddress(null), () -> r.setStartDate(future()),
                    () -> r.setNumberOfNights(1), () -> r.setNumberOfBeds(2), () -> r.setNumberOfBedrooms(1), () -> r.setNumberOfBathrooms(1), () -> r.setLodgingSize(900))) unchanged(r, IllegalStateException.class, action);
            if (r instanceof HotelReservation v) unchanged(r, IllegalStateException.class, () -> v.setKitchenette(false));
            if (r instanceof CabinReservation v) { unchanged(r, IllegalStateException.class, () -> v.setFullKitchen(false)); unchanged(r, IllegalStateException.class, () -> v.setLoft(false)); }
            if (r instanceof HouseReservation v) unchanged(r, IllegalStateException.class, () -> v.setNumberOfFloors(1));
        }
    }
    private static void testLifecyclePersistence() throws Exception {
        Path dir = directory("lifecycle"); Manager m = new Manager(dir); Account a = account("A000000001"); m.addAccount(a); int id = 1;
        for (Reservation template : types()) for (String state : new String[]{"draft", "completed", "cancelled"}) {
            JsonObject json = JsonData.object(template.toString()); json.addProperty("reservationNumber", String.format(java.util.Locale.ROOT, "R%09d", id++));
            Reservation r = Reservation.fromJson(json.toString()); a.addReservation(r);
            if (state.equals("completed")) a.completeReservation(r.getReservationNumber());
            if (state.equals("cancelled")) a.cancelReservation(r.getReservationNumber());
        }
        m.updateAccountFiles(a.getAccountNumber()); Account restored = new Manager(dir).retrieveAccount(a);
        check(restored.getReservations().size() == 9, "all subtype/state combinations");
        for (Reservation r : a.getReservations()) {
            Reservation loaded = restored.retrieveReservation(r.getReservationNumber()); same(r, loaded);
            if (!loaded.getStatus().equals("draft")) {
                unchanged(loaded, IllegalOperationException.class, loaded::completeReservation);
                unchanged(loaded, IllegalOperationException.class, loaded::cancelReservation);
                unchanged(loaded, IllegalStateException.class, () -> loaded.setNumberOfNights(1));
            }
        }
    }
    public static void main(String[] args) throws Exception {
        temp = Files.createTempDirectory("swen646-code3-");
        try {
            run("01 Account creation and validation", Test::testAccountCreationAndValidation);
            run("02 Duplicate account rejection", Test::testDuplicateAccounts);
            run("03 Hotel creation", Test::testHotelCreation);
            run("04 Cabin creation", Test::testCabinCreation);
            run("05 House creation", Test::testHouseCreation);
            run("06 Adding reservations", Test::testAddingReservations);
            run("07 System-wide duplicate rejection", Test::testSystemWideDuplicates);
            run("08 Retrieving accounts", Test::testRetrievingAccounts);
            run("09 Retrieving reservations", Test::testRetrievingReservations);
            run("10 Updating account information", Test::testUpdatingAccountInformation);
            run("11 Updating reservations", Test::testUpdatingReservations);
            run("12 Invalid reservation updates", Test::testInvalidReservationUpdates);
            run("13 Completing reservations", Test::testCompletingReservations);
            run("14 Invalid completion attempts", Test::testInvalidCompletionAttempts);
            run("15 Cancelling reservations", Test::testCancellingReservations);
            run("16 Invalid cancellation attempts", Test::testInvalidCancellationAttempts);
            run("17 Hotel pricing", Test::testHotelPricing);
            run("18 Cabin pricing", Test::testCabinPricing);
            run("19 House pricing", Test::testHousePricing);
            run("20 Pricing boundaries", Test::testPricingBoundaries);
            run("21 Saving files", Test::testSavingFiles);
            run("22 Loading files", Test::testLoadingFiles);
            run("23 Round-trip consistency and real Gson", Test::testRoundTripConsistency);
            run("24 Custom exceptions", Test::testCustomExceptions);
            run("25 Invalid inputs and encapsulation", Test::testInvalidInputValidation);
            run("26 Lifecycle persistence", Test::testLifecyclePersistence);
            System.out.println((failures == 0 ? "PASS" : "FAIL") + ": " + checks + " checks passed; " + (scenarios - failures) + "/" + scenarios + " scenarios passed.");
            if (failures != 0) throw new AssertionError(failures + " scenario(s) failed");
        } finally {
            try (var paths = Files.walk(temp)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
    }
    private static void testInvalidReservationUpdates() {
        Account a = account("A000000001"); Reservation original = hotel("R000000001"); a.addReservation(original);
        unchanged(original, IllegalArgumentException.class, () -> a.updateReservation(null));
        unchanged(original, IllegalArgumentException.class, () -> a.updateReservation(hotel("R000000009")));
        unchanged(original, IllegalOperationException.class, () -> a.updateReservation(cabin(original.getReservationNumber())));
        Reservation foreign = hotel(original.getReservationNumber()); account("A000000002").addReservation(foreign);
        unchanged(original, IllegalOperationException.class, () -> a.updateReservation(foreign));
        Reservation expired = hotel(original.getReservationNumber()); expired.setStartDate(past());
        unchanged(original, IllegalOperationException.class, () -> a.updateReservation(expired));
        Reservation invalid = hotel(original.getReservationNumber());
        invalid.numberOfNights = 0; // Deliberately corrupt test input to exercise whole-object validation.
        unchanged(original, IllegalArgumentException.class, () -> a.updateReservation(invalid));
        Reservation unassigned = hotel(original.getReservationNumber());
        unassigned.setNumberOfNights(5); a.updateReservation(unassigned);
        check(original.getNumberOfNights() == 5 && original.getAccountNumber().equals(a.getAccountNumber()),
                "unassigned draft update preserves stored ownership");
        for (boolean completed : new boolean[]{true,false}) {
            Reservation candidate = hotel(original.getReservationNumber()); if (completed) candidate.completeReservation(); else candidate.cancelReservation();
            unchanged(original, IllegalOperationException.class, () -> a.updateReservation(candidate));
        }
        original.completeReservation(); unchanged(original, IllegalOperationException.class, () -> a.updateReservation(hotel(original.getReservationNumber())));
        for (boolean expiredStored : new boolean[]{true,false}) {
            Account other = account("A000000003"); Reservation r = hotel("R000000002"); other.addReservation(r);
            if (expiredStored) r.setStartDate(past()); else r.cancelReservation();
            unchanged(r, IllegalOperationException.class, () -> other.updateReservation(hotel(r.getReservationNumber())));
        }
    }
}
