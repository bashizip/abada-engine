package io.abada.worker;

public class WorkerProtocolException extends RuntimeException {
    private final int status;
    private final String code;

    public WorkerProtocolException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() { return status; }
    public String code() { return code; }
}
