-- Create reviews table
CREATE TABLE IF NOT EXISTS reviews (
    id_review BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_booking BIGINT NOT NULL,
    id_user BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    komentar TEXT,
    tanggal_review TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_booking (id_booking),
    INDEX idx_user (id_user),
    INDEX idx_rating (rating),
    INDEX idx_tanggal (tanggal_review),
    UNIQUE KEY unique_booking_review (id_booking),
    FOREIGN KEY (id_booking) REFERENCES bookings(id_booking) ON DELETE CASCADE,
    FOREIGN KEY (id_user) REFERENCES users(user_id) ON DELETE CASCADE
);
