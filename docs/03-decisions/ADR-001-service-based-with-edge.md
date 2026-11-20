# ADR-001 — Service-based architecture với edge agent tại nhà máy

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư, chủ dự án TMC-AACS
- Supersedes: —
- Superseded by: —

## Context

TMC có hai nhà máy (Bình Dương ~800 người, Long An ~600 người), mỗi nhà máy có nhiều trạm cổng. Yêu cầu:

- Phản hồi kiosk ≤ 2 giây ở 800 lượt/30 phút/nhà máy.
- Không mất dữ liệu chấm công khi WAN gián đoạn.
- Tách biệt đường quét khỏi báo cáo/HR/admin.
- Có thể mở rộng trạm và thay adapter thiết bị/HR mà không sửa nghiệp vụ.
- Đội ngũ nhỏ; chưa cần microservices/Kubernetes.

## Decision

Áp dụng **Service-Based Architecture**:

1. Năm miền nghiệp vụ lớn: ICS, AAS, GSS, RIS, ADS, ánh xạ C01–C26.
2. **Edge Access Agent** là deployment unit riêng, chạy tại mỗi nhà máy, sở hữu đường quét đồng bộ và kho dữ liệu cục bộ.
3. Central xử lý danh tính, cấu hình, ingestion, chiếu chấm công, báo cáo, xuất HR, audit.
4. Giao tiếp nội bộ qua API và sự kiện (outbox/inbox); edge → central dùng lô mTLS.
5. Mỗi miền sở hữu schema PostgreSQL riêng; không join/FK chéo.

## Consequences

Tích cực:

- Đường quét không phụ thuộc WAN/central/HR.
- Ranh giới miền rõ, có thể tách deployment khi tải tăng.
- Adapter cô lập turnstile/HR; contract test thay thế dễ.

Tiêu cực / rủi ro:

- Vận hành thêm edge node, backup, monitoring tại nhà máy.
- Đồng bộ policy/credential và đối soát outbox/inbox phức tạp hơn monolith thuần.
- Cần xử lý split-brain và clock skew.

## Alternatives considered

- **Monolith tập trung, kiosk gọi trực tiếp central:** loại vì WAN gián đoạn làm tê liệt cổng và vi phạm NFR-A1.
- **Microservices + Kafka/K8s:** loại ở giai đoạn này vì chi phí vận hành và độ phức tạp không tương xứng; có thể xem xét khi tải/đội ngũ tăng.
- **Chỉ edge, không central:** loại vì báo cáo, HR export, audit tập trung và quản trị cần central.

## References

FR-A1–A7, FR-R1–R5, NFR-A1, ADR-008.
