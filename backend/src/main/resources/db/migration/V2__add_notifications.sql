-- Notifications table
CREATE TABLE notifications (
    id                  BIGSERIAL       PRIMARY KEY,
    recipient_user_id   BIGINT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lift_id             BIGINT          REFERENCES lifts(id) ON DELETE CASCADE,
    maintenance_id      BIGINT          REFERENCES maintenances(id) ON DELETE CASCADE,
    title               VARCHAR(255)    NOT NULL,
    message             VARCHAR(1000)   NOT NULL,
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id);
CREATE INDEX idx_notifications_is_read ON notifications(recipient_user_id, is_read);
