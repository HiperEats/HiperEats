package pt.ua.hipereats.application.port.in;

import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/** Inbound port: the telemetry adapter hands every reading it receives to this use case. */
public interface RecordTelemetryUseCase {

    /** What happened to a reading. */
    enum Result {
        /** The reading is now the scooter's latest state. */
        RECORDED,
        /** Not newer than the latest reading (duplicated or out of order), so it was ignored. */
        IGNORED_STALE
    }

    Result record(TelemetryReading reading);
}
