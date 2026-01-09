package nvt.backend.model.order;

public enum OrderStatus {
    PENDING,        // Kreirana, čeka obradu
    CONFIRMED,      // Potvrđena, rezervisane zalihe
    PROCESSING,     // U obradi - priprema za isporuku
    SHIPPED,        // Poslata
    DELIVERED,      // Isporučena
    COMPLETED,      // Završena
    CANCELLED       // Otkazana
}
