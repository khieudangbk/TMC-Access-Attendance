/**
 * Planned scan pipeline: C05 Scan Endpoint, C06 QR Verifier, C07 Access Policy Engine,
 * C08 Attendance Recorder, C09 Gate Commander, C10 Policy/Credential Sync, C11 Event Forwarder.
 *
 * Invariants (see docs/02-architecture/scan-pipeline.md):
 * - event + intent + outbox commit before any gate command;
 * - scan_id unique, retries return the stored result;
 * - timeout is UNKNOWN, never success and never blindly retried.
 */
package com.tmc.aacs.edge.scan;
