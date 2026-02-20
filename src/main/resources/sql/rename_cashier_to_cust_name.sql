-- Migration to rename cashier_name to cust_name in transactions table
-- This script changes the column name to better reflect that it stores customer name
-- for online bookings and employee name for cash payments

ALTER TABLE transactions RENAME COLUMN cashier_name TO cust_name;

-- Add comment to clarify the column usage
COMMENT ON COLUMN transactions.cust_name IS 'Customer name for online bookings, Employee name for cash payments';
