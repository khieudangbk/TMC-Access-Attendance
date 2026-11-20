# ADR-008 — Central là modular monolith split-ready giai đoạn đầu

- Status: Accepted
- Date: 2026-11-20
- Deciders: Kiến trúc sư, chủ dự án
- Supersedes: —
- Superseded by: —

## Context

Thiết kế đích gồm 5 miền dịch vụ. Tuy nhiên đội ngũ nhỏ, tải hiện tại (~1.400 nhân viên, 2 nhà máy) chưa cần 5 deployment độc lập. Tách sớm làm tăng chi phí vận hành, test tích hợp và gỡ lỗi. Cần một cấu trúc vẫn giữ ranh giới để tách sau.

## Decision

1. Phase 1–2: central chạy như **một Spring Boot application** dùng **Spring Modulith**.
2. Mỗi miền là module: `identity`, `access`, `station`, `reporting`, `admin`, cộng `system`, `shared`.
3. Module chỉ giao tiếp qua public API/application port; phụ thuộc được khai báo và kiểm tra bằng `ModularityTests`.
4. Mỗi module vẫn sở hữu schema riêng (ADR-007).
5. **Edge luôn là deployment riêng** từ đầu.
6. Khi cần tách, tiêu chí: tải vượt ngưỡng, nhu cầu scale độc lập, hoặc ranh giới đội ngũ rõ. Việc tách không đổi API/event contract.

## Consequences

Tích cực:

- Một artifact triển khai, vận hành đơn giản.
- Transaction nội bộ dễ, test end-to-end nhanh.
- Ranh giới vẫn được kiểm tra tự động, giảm rủi ro “big ball of mud”.

Tiêu cực / rủi ro:

- Một process lỗi ảnh hưởng nhiều miền; cần tách worker/báo cáo và giới hạn tài nguyên.
- Module có thể lạm dụng gọi trực tiếp nếu không kỷ luật.
- Khi tách cần chuyển giao tiếp in-process sang HTTP/event.

## Alternatives considered

- **5 microservices ngay:** loại vì chi phí vận hành và phức tạp tích hợp.
- **Monolith không module:** loại vì khó tách, dễ coupling.
- **Tách RIS/ADS trước:** có thể xem xét ở phase báo cáo nếu tải tăng; chưa cần bây giờ.

## References

ADR-001, ADR-002, `ModularityTests`, `docs/02-architecture/overview.md`.
