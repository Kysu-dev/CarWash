-- Rename transfer table to transaction and modify structure for cash payment
-- This script should be run to migrate from transfer-based to transaction-based payment system

-- Step 1: Create new transaction table with improved structure
CREATE TABLE transactions (
    id_transaction BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_booking BIGINT NOT NULL,
    transaction_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH', 'TRANSFER', 'CARD', 'E_WALLET') NOT NULL DEFAULT 'CASH',
    payment_proof VARCHAR(255) NULL, -- Only for non-cash payments
    payment_status ENUM('PENDING', 'VALID', 'INVALID') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at DATETIME NULL,
    verified_by VARCHAR(100) NULL, -- Employee/Admin name who verified
    cashier_name VARCHAR(100) NULL, -- Employee name who handled cash payment
    notes TEXT NULL,
    
    FOREIGN KEY (id_booking) REFERENCES bookings(id_booking),
    UNIQUE KEY unique_booking_transaction (id_booking)
);

-- Step 2: Migrate existing transfer data to transaction table
INSERT INTO transactions (
    id_booking, 
    transaction_date, 
    amount, 
    payment_method, 
    payment_proof, 
    payment_status, 
    created_at, 
    verified_at, 
    verified_by, 
    notes
)
SELECT 
    id_booking,
    tanggal_transaksi,
    jumlah_transfer,
    'TRANSFER' as payment_method,
    bukti_transfer,
    status_pembayaran,
    created_at,
    verified_at,
    verified_by,
    notes
FROM transfer;

-- Step 3: Drop old transfer table (uncomment when ready)
-- DROP TABLE transfer;

-- Step 4: Add indexes for better performance
CREATE INDEX idx_transactions_booking ON transactions(id_booking);
CREATE INDEX idx_transactions_status ON transactions(payment_status);
CREATE INDEX idx_transactions_method ON transactions(payment_method);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
