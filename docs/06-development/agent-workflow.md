# Agent workflow

## 1. Trước khi code

1. Đọc `AGENTS.md`.
2. Xác định mã yêu cầu (`FR-*`, `NFR-*`, `US-*`) hoặc ADR.
3. Mở tài liệu theo [agent-context](../08-agents/agent-context.md).
4. Kiểm tra [roadmap](roadmap.md): hạng mục có trong phase hiện tại không?
5. Nếu thay đổi chạm bất biến/bảo mật/schema/API: tìm ADR; nếu chưa có, đề xuất ADR trước khi implement.
6. Nêu giả định và phạm vi thay đổi.

## 2. Trong khi code

- Giữ diff nhỏ, đúng module/layer.
- Viết test cùng lúc với hành vi, gồm failure path.
- Không sửa migration đã áp dụng; tạo migration mới.
- Không thêm dependency/module nếu chưa có lý do + cập nhật tài liệu.
- Không log raw token/PII; dùng ID tương quan.
- Không thêm bypass tạm thời.
- Nếu phát hiện docs/code lệch: dừng, ghi rõ, cập nhật cùng thay đổi.

## 3. Sau khi code

```bash
make verify
```

Checklist:

- [ ] `ModularityTests` pass.
- [ ] Test mới cho success + failure.
- [ ] OpenAPI/docs cập nhật.
- [ ] Không secret/PII/raw QR.
- [ ] UI đủ loading/empty/error.
- [ ] Migration forward-only.
- [ ] Không đổi file không liên quan.

## 4. Khi nào dừng và hỏi

- Thay đổi bất biến §3, ranh giới schema, ADR đã accepted.
- Breaking API hoặc hợp đồng tích hợp mới.
- Đánh đổi bảo mật (TTL, cửa sổ offline, retention, PII).
- Thao tác dữ liệu phá hủy/cấu hình production.
- Yêu cầu xung đột.

Không tự quyết các mục trên.

## 5. Mẫu báo cáo cuối

```markdown
## Đã làm
- ...

## Vì sao
- Requirement/ADR: ...

## Test
- `make verify` → pass
- Thêm test: ...

## Docs
- ...

## Rủi ro / việc tiếp theo
- ...
```

## 6. Quy tắc giao tiếp

- Trả lời ngắn, nêu file/thay đổi cụ thể.
- Không tuyên bố “đã xong” nếu chưa chạy test.
- Nêu rõ phần chưa làm/giả định.
- Với thay đổi rủi ro cao, giải thích trade-off và đề xuất bước kiểm chứng.

## 7. Cấm

- Tự ý xóa/sửa dữ liệu, migration, audit.
- Tắt test/lint để pass.
- Commit secret/dữ liệu thật.
- Thêm microservice/Kafka/K8s/JPA vào đường quét khi chưa có ADR.
- Gọi central/HR đồng bộ từ scan path.
