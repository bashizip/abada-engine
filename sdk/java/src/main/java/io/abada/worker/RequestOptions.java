package io.abada.worker;

public record RequestOptions(String idempotencyKey, String traceParent, String traceState) {
    public static RequestOptions defaults() {
        return new RequestOptions(null, null, null);
    }
}
