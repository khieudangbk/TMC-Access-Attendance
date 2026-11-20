# Schema và quyền sở hữu dữ liệu

## 1. Nguyên tắc

- Mỗi schema thuộc đúng một miền; chỉ miền đó ghi.
- Không join/FK chéo schema. Tham chiếu bằng ID.
- Dữ liệu liên miền đi qua API hoặc sự kiện (outbox/inbox).
- Migration forward-only; không sửa migration đã áp dụng.
- Bảng append-only không cấp `UPDATE`/`DELETE` cho role ứng dụng.

## 2. Schema hiện có

### Central — `tmc_central`

| Schema | Chủ sở hữu | Bảng hiện có | Ghi chú |
|---|---|---|---|
| `ics` | ICS | — | Chưa có bảng; dự kiến `employee`, `credential`, `signing_key` |
| `aas` | AAS central | — | Dự kiến `ingested_event`, `attendance_day`, `adjustment` |
| `gss` | GSS | `factory`, `gate_station` | Đã seed 10 trạm |
| `ris` | RIS | — | Dự kiến `report_period`, `attendance_snapshot`, `hr_export_batch` |
| `ads` | ADS | — | Dự kiến `user_account`, `role`, `audit_record` |

`V1__create_service_schemas.sql` tạo schema + comment.
`V2__gss_station.sql` tạo `gss.factory`, `gss.gate_station`.
`V3__seed_gss_stations.sql` seed 2 nhà máy + 10 trạm.

### GSS hiện tại

```sql
gss.factory (
  factory_id    varchar(32) PK,
  name          varchar(128) NOT NULL,
  timezone      varchar(64)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  active        boolean      NOT NULL DEFAULT true,
  created_at    timestamptz  NOT NULL DEFAULT now(),
  updated_at    timestamptz  NOT NULL DEFAULT now()
)

gss.gate_station (
  station_id      varchar(32) PK,
  factory_id      varchar(32) NOT NULL REFERENCES gss.factory(factory_id),
  name            varchar(128) NOT NULL,
  direction       varchar(8)  CHECK (direction IN ('IN','OUT')),
  gate_id         varchar(64) NOT NULL,
  kiosk_id        varchar(64) NOT NULL,
  status          varchar(16) CHECK (status IN ('ACTIVE','INACTIVE','MAINTENANCE')),
  config_revision integer     NOT NULL DEFAULT 1 CHECK (> 0),
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),
  UNIQUE (factory_id, gate_id, direction),
  UNIQUE (kiosk_id)
)
```

Ràng buộc nghiệp vụ:

- Một kiosk chỉ gán cho một trạm.
- Một cổng + hướng không lặp trong cùng nhà máy.
- `config_revision` tăng mỗi lần đổi cấu hình; event lưu revision tại thời điểm quét.

### Edge — `tmc_edge`

Schema `aas_edge`:

| Bảng | Mục đích | Ghi chú |
|---|---|---|
| `access_event` | Sự kiện gốc của lượt quét | `UNIQUE (scan_id)`, append-only |
| `gate_intent` | Ý định điều khiển cổng | `UNIQUE` ngầm qua PK; FK nội schema tới `access_event(scan_id)` |
| `outbox` | Sự kiện chờ gửi central | Relay theo `status`, `next_attempt_at` |
| `inbox` | Khử trùng message đến | Chưa tạo; thêm khi có consumer |
| `gate_outcome` | Lịch sử outcome | Chưa tạo; hiện `gate_intent.state` |
| `policy_snapshot` | Snapshot policy/khóa | Chưa tạo |
| `lease` | Chặn split-brain | Chưa tạo |

`access_event`:

```sql
event_id            uuid PK,
scan_id             uuid NOT NULL UNIQUE,
station_id          varchar(32) NOT NULL,
employee_ref        varchar(64),
direction           varchar(8) CHECK IN/OUT,
decision            varchar(16) CHECK ALLOWED/DENIED,
reason              varchar(128),
qr_key_id           varchar(64),
payload_fingerprint char(64) NOT NULL,
received_at         timestamptz NOT NULL,
source_time         timestamptz,
schema_version      integer NOT NULL DEFAULT 1
```

`gate_intent`:

```sql
command_id  uuid PK,
scan_id     uuid NOT NULL REFERENCES aas_edge.access_event(scan_id),
gate_id     varchar(64) NOT NULL,
action      varchar(16) NOT NULL,
state       varchar(24) CHECK PENDING/SENT/CONFIRMED/REJECTED/FAILED/UNKNOWN,
created_at  timestamptz NOT NULL DEFAULT now(),
updated_at  timestamptz NOT NULL DEFAULT now()
```

`outbox`:

```sql
event_id        uuid PK,
aggregate_type  varchar(40) NOT NULL,
aggregate_id    varchar(64) NOT NULL,
event_type      varchar(80) NOT NULL,
payload         jsonb NOT NULL,
schema_version  integer NOT NULL DEFAULT 1,
status          varchar(16) CHECK PENDING/SENT/FAILED,
attempts        integer NOT NULL DEFAULT 0,
created_at      timestamptz NOT NULL DEFAULT now(),
next_attempt_at timestamptz,
sent_at         timestamptz
```

## 3. Quyền DB (production)

| Role | Quyền |
|---|---|
| `tmc_central_app` | `USAGE` trên schema central; `SELECT/INSERT/UPDATE` bảng nghiệp vụ; **không** `DELETE` trên append-only |
| `tmc_audit_writer` | `INSERT` trên `ads.audit_record`; `SELECT` tối thiểu |
| `tmc_audit_reader` | `SELECT` audit + checkpoint |
| `tmc_migrator` | DDL cho Flyway; không dùng runtime |
| `tmc_edge_app` | DML trên `aas_edge` |
| `tmc_readonly` | `SELECT` có kiểm soát cho vận hành |

- Không cấp `SUPERUSER` cho ứng dụng.
- Tạo role riêng cho từng môi trường; không dùng chung mật khẩu.
- `REVOKE UPDATE, DELETE` trên `access_event`, `audit_record`, snapshot đã xuất.

## 4. Index và hiệu năng

| Bảng | Index | Lý do |
|---|---|---|
| `gss.gate_station` | `(factory_id)` | Lọc theo nhà máy |
| `aas_edge.access_event` | `(received_at)` | Đối soát/backlog |
| `aas_edge.outbox` | `(status, next_attempt_at, created_at)` | Relay |
| `aas_edge.gate_intent` | `(state)` khi cần | Đối soát outcome |
| `aas.ingested_event` | `(scan_id)`, `(occurred_at)` | Tra cứu/stream |
| `ads.audit_record` | `(occurred_at)`, `(actor)` | Tra cứu |

Nguyên tắc: index phục vụ truy vấn đã biết; không thêm trước khi có kế hoạch truy vấn. Bảng lớn cân nhắc partition theo tháng khi dữ liệu tăng.

## 5. Retention

| Dữ liệu | Thời gian | Sau đó |
|---|---|---|
| Access event central | 5 năm | Archive/xóa theo chính sách |
| Audit | 5 năm | Archive; checkpoint giữ lâu hơn nếu cần |
| Edge event | 30 ngày sau đối soát | Xóa theo lô đã xác nhận |
| Snapshot/HR batch | 5 năm | Giữ để tái hiện |
| Log ứng dụng | 30–90 ngày | Xoay vòng |

Retention/xóa/ẩn danh phải được phê duyệt và ghi audit; áp dụng cả backup.

## 6. Checklist thay đổi schema

- [ ] Có requirement/ADR.
- [ ] Migration mới, không sửa migration cũ.
- [ ] Đúng schema sở hữu; không FK chéo.
- [ ] Constraint cho idempotency/append-only.
- [ ] Index có lý do.
- [ ] Quyền DB cập nhật.
- [ ] Test trên DB sạch + DB có dữ liệu.
- [ ] Cập nhật tài liệu này và `data-model.md`.
