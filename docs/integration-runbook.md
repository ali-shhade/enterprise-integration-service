
# Enterprise Integration Service
# Operational Runbook

## 1. Document Information

| Item | Value |
|---|---|
| Project | Enterprise Integration Service |
| Purpose | Receive, transform and deliver business orders |
| Application | Spring Boot + Apache Camel |
| Java version | Java 21 |
| Spring Boot version | 4.1.1 |
| Apache Camel version | 4.14.0 |
| Database | MySQL 8.4 (local lab) |
| Environment | Local development / WSL Ubuntu |
| Document status | Training runbook |
| Last reviewed | 2026-11-08 |

### Status definitions

- VERIFIED: Tested successfully in the current environment.
- IMPLEMENTED - NOT VERIFIED: Code/configuration exists but runtime testing is pending.
- PLANNED: Concept or implementation has been designed but is not confirmed.

This document is not a production deployment approval.

---

## 2. Integration Overview

The application demonstrates an enterprise order integration workflow.

Its responsibilities include:

1. Receiving orders from files or external systems.
2. Parsing CSV and other structured formats.
3. Transforming data into Java order objects.
4. Validating incoming business information.
5. Sending orders to REST APIs.
6. Tracking processing status.
7. Handling integration errors.
8. Monitoring application and integration activity.

### Main technologies

- Java 21
- Spring Boot 4.1.1
- Apache Camel 4.14.0
- Linux / Bash
- REST / HTTP
- CSV / JSON / XML
- SSH / SFTP
- MySQL 8.4
- Spring Boot Actuator
- Micrometer

---

## 3. Architecture and Data Flow

### General integration flow

Partner System
    |
    v
File / SFTP / HTTP Consumer
    |
    v
Apache Camel Route
    |
    v
Parsing and Transformation
    |
    v
Java Order Model
    |
    v
Validation
    |
    +----------------------+
    |                      |
    v                      v
Invalid Data          Valid Data
    |                      |
    v                      v
REJECTED              Database / REST API
                           |
                           v
                   SUCCESS or FAILED

### Main components

| Component | Responsibility |
|---|---|
| Camel consumer | Receives incoming data |
| Parser | Reads CSV / EDI / other formats |
| Java model | Represents the internal order |
| Validator | Checks required fields and business rules |
| Serializer | Converts objects to JSON or XML |
| REST producer | Sends data to API endpoints |
| Database service | Stores order and processing information |
| Monitoring | Reports operational status |

---

## 4. Subscription and Integration Inventory

A subscription represents a configured relationship with an external
partner or data source. The exact meaning depends on the platform.

### Partner A - Incoming orders

| Setting | Configuration |
|---|---|
| Subscription ID | PARTNER_A_ORDERS |
| Partner | PARTNER_A |
| Protocol | SFTP |
| Host | localhost (local lab only) |
| Port | 22 |
| Remote input | sftp-lab/incoming |
| Expected format | CSV / EDIFACT |
| Authentication | SSH public-key authentication |
| Destination | Internal Camel order pipeline |
| Status | IMPLEMENTED - NOT VERIFIED end-to-end |

### Local file integrations

Previously observed Camel routes:

| Route ID | Source | Purpose |
|---|---|---|
| csv-order-integration | data/day3-final/input | CSV order processing |
| csv-to-rest-api-route | data/day4/csv-input | CSV to REST API |
| csv-to-json-route | data/csv-input | CSV to JSON |
| csv-to-xml-route | data/csv-xml-input | CSV to XML |
| file-integration-route | data/incoming | File validation and routing |
| send-order-to-api | data/day4/http-input | JSON to REST API |

Verify the current route classes, endpoint locations and enabled routes
before treating this inventory as the current deployment configuration.

### SFTP outbound

| Setting | Configuration |
|---|---|
| Source | data/day5/sftp-outgoing |
| Protocol | SFTP |
| Host | localhost (local lab only) |
| Port | 22 |
| Remote destination | sftp-lab/processed |
| Authentication | SSH public key |
| Status | IMPLEMENTED - NOT VERIFIED end-to-end |

---

## 5. Jobs and Scheduling

### Integration monitoring job

| Setting | Value |
|---|---|
| Job ID | INTEGRATION_MONITOR |
| Script | scripts/monitor-integration.sh |
| Schedule | Every 5 minutes |
| Cron | */5 * * * * |
| Output | logs/monitor.log |
| Failure handling | Non-zero exit code and system log message |
| Status | CONFIGURED IN LESSON - VERIFY |

### Archive cleanup job

| Setting | Value |
|---|---|
| Job ID | ARCHIVE_CLEANUP |
| Script | scripts/cleanup-archive.sh |
| Schedule | Daily at 02:00 |
| Cron | 0 2 * * * |
| Target | data/day5/archive |
| Retention | 7 days |
| Status | CONFIGURED IN LESSON - VERIFY |

### File polling

The sample SFTP inbound route uses a polling delay of 5000 milliseconds.

This is a file-consumer polling interval, not a separate cron job.

---

## 6. Properties and Secrets

### Application configuration

| Property | Purpose | Configuration source |
|---|---|---|
| integration.api.url | Destination API endpoint | application properties / environment |
| integration.api.token | API authentication | Secret configuration |
| integration.sftp.host | SFTP server hostname | Environment / properties |
| integration.sftp.port | SFTP server port | Properties |
| integration.sftp.username | SFTP account | Secure configuration |
| integration.sftp.private-key | SSH private-key path | Protected file |
| integration.sftp.known-hosts | Trusted SSH host keys | Protected file |
| integration.sftp.remote-directory | Remote input directory | Properties |
| DB_URL | Database JDBC URL | Environment |
| DB_USERNAME | Database user | Environment |
| DB_PASSWORD | Database password | Secret configuration |

### Security rules

- Never commit passwords, tokens or private keys.
- Never print credentials in logs.
- Do not send secrets inside monitoring reports.
- Use separate credentials for each environment.
- Restrict access to secret files.
- Rotate credentials according to provider policy.
- Verify server host keys and TLS certificates.
- Replace example local credentials before any real deployment.

Actual secret values must not appear in this runbook.

---

## 7. Data Mapping

### CSV to internal Order model

| Source field | Internal field | Rule |
|---|---|---|
| ORDER_ID | orderId | Required; trim whitespace |
| STATUS | status | Validate allowed values |
| CUSTOMER | customer | Required; trim whitespace |
| AMOUNT | amount | Convert to BigDecimal; positive |
| Partner configuration | sourceSystem | Assign partner identifier |
| Currency configuration | currency | Validate currency code |

### EDIFACT mapping

| EDIFACT source | Internal field |
|---|---|
| BGM order reference | orderId |
| NAD+BY | customer |
| MOA+203 | amount |
| MOA currency component | currency |
| NAD+SU | supplier |
| LIN product identifier | productCode |
| QTY | quantity |

The training EDIFACT parser is not a complete implementation
of the EDIFACT standard and must not be treated as production-ready.

### Invalid data

- Missing required fields: reject the order.
- Invalid numeric values: reject the affected record.
- Unsupported format: route to the appropriate error/rejection process.
- Duplicate external order ID: detect using the database uniqueness rule.

---

## 8. Order Processing Statuses

| Status | Meaning |
|---|---|
| RECEIVED | Order was received |
| VALIDATED | Required fields and rules passed |
| PROCESSING | Delivery or persistence is underway |
| SUCCESS | Required processing completed successfully |
| REJECTED | Data or business validation failed |
| FAILED | Technical processing failed |

### Expected lifecycle

RECEIVED
    |
    v
VALIDATED
    |
    v
PROCESSING
    |
    +----------+
    |          |
    v          v
 SUCCESS     FAILED

Invalid input may go directly from RECEIVED to REJECTED.

The database is intended to store the current order state
and its processing history.

Database integration status: IMPLEMENTED IN CODE DESIGN;
JDBC CONNECTIVITY REQUIRES VERIFICATION.

---

## 9. Retry and Error Handling

### Known API route policy

Previously implemented policy for the CSV-to-API route:

| Failure | Handling |
|---|---|
| HTTP 503 | Up to 2 retries, 1 second delay |
| Other HTTP failures | Route to API rejection directory |
| Invalid CSV row | Route to rejected processing |
| Temporary network error | Apply an appropriate bounded retry policy |

Verify the current route configuration before relying on these settings.

### Important distinctions

- Transient errors may be retried.
- Permanent validation failures should be rejected.
- Authentication errors require credential/configuration investigation.
- Duplicate orders should not be processed again.
- Retry exhaustion must result in a visible failure record.

### Data protection

Keep original failed files until a defined recovery or retention
procedure allows them to be archived or removed.

---

## 10. Monitoring and Logs

### Application health

Health endpoint:

http://localhost:8080/actuator/health

Expected local result:

HTTP 200 with status UP.

### Monitoring responsibilities

- Application availability
- Camel route availability
- Successful order processing
- Failed and rejected orders
- API errors and response codes
- Database connectivity
- SFTP transfer status
- Disk and memory availability
- Certificate expiration
- Scheduled-job execution

### Log location

logs/integration-service.log

Monitoring output:

logs/monitor.log

### Useful commands

Check application:

curl -i http://localhost:8080/actuator/health

Inspect logs:

tail -f logs/integration-service.log

Inspect errors:

grep "ERROR" logs/integration-service.log

Inspect order status:

SELECT status, COUNT(*)
FROM integration_orders
GROUP BY status;

Monitoring scripts and structured JSON logging:
IMPLEMENTED IN LESSON - RUNTIME VERIFICATION REQUIRED.

---

## 11. Troubleshooting Procedures

### Application unavailable

1. Check the health endpoint.
2. Check whether port 8080 is listening.
3. Inspect application logs.
4. Check Java process and startup errors.
5. Confirm that required configuration is available.

### SFTP connection failure

1. Verify the SFTP server is running.
2. Check host and port.
3. Verify SSH username.
4. Verify private-key file permissions.
5. Verify known_hosts.
6. Test using the SFTP command-line client.
7. Inspect SSH server logs.

### REST API failure

1. Check the configured API URL.
2. Verify network connectivity.
3. Check HTTP response status.
4. Check token configuration without printing the token.
5. Inspect the correlation ID in logs.
6. Apply the documented retry or rejection policy.

### Database connection failure

1. Check MySQL container or server status.
2. Verify the database port.
3. Confirm DB_URL and username.
4. Confirm that the password is available to Spring Boot.
5. Test database login directly.
6. Inspect the deepest JDBC exception.
7. Check transaction rollback and database availability.

### Disk space warning

1. Run df -h.
2. Find large directories using du -sh.
3. Inspect log files and archives.
4. Apply approved retention procedures.
5. Never delete unprocessed or failed order files without recovery approval.

---

## 12. Recovery and Security Procedures

### Order recovery

1. Identify the affected order using its source system and order ID.
2. Search logs using its correlation ID.
3. Check the current database status.
4. Inspect the processing history.
5. Identify whether the error is transient or permanent.
6. Correct the underlying issue.
7. Retry only when safe.
8. Confirm that duplicate protection is effective.
9. Record the recovery result.

### Security procedures

- Use SSH public-key authentication for SFTP.
- Maintain trusted SSH host keys.
- Verify HTTPS certificate chain and hostname.
- Protect API and database credentials.
- Restrict file and directory permissions.
- Never expose Actuator management endpoints publicly
  without appropriate access controls.
- Keep audit and processing logs.
- Apply approved credential rotation procedures.

---

## 13. Deployment and Configuration Changes

Before deploying a configuration change:

1. Confirm the target environment.
2. Validate all required properties.
3. Verify secret availability.
4. Confirm database and external-system connectivity.
5. Compile and test the application.
6. Deploy the application.
7. Verify Camel routes started.
8. Test a representative order.
9. Check application and integration health.
10. Document the deployment result.

For rollback:

1. Stop or roll back to the previous known-good application version.
2. Restore the previous non-secret configuration.
3. Verify secret compatibility.
4. Recheck application and integration health.
5. Preserve files and orders that have not completed processing.

---

## 14. Change History

| Date | Change | Status |
|---|---|---|
| 2026-11-08 | Initial operational runbook | Training draft |
