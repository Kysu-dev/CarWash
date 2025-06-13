package UASPraktikum.CarWash.controller;

// Replace wildcard import with explicit imports
import UASPraktikum.CarWash.model.Booking;
import UASPraktikum.CarWash.model.PaymentMethod;
import UASPraktikum.CarWash.model.PaymentStatus;
import UASPraktikum.CarWash.model.Transaction;
import UASPraktikum.CarWash.model.User;
import UASPraktikum.CarWash.service.TransactionService;
import UASPraktikum.CarWash.service.BookingService;
import UASPraktikum.CarWash.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/admin/transaction-management")
public class AdminTransactionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;

    /**
     * Display list of all transactions
     */
    @GetMapping("")
    public String listTransactions(Model model) {
        List<Transaction> transactions = transactionService.getAllTransactions();
        model.addAttribute("transactions", transactions);
        model.addAttribute("section", "transactions");
        return "admin/transaction/list";
    }

    /**
     * Show form to create a new transaction
     */
    @GetMapping("/form")
    public String showCreateForm(Model model) {
        // Get all completed bookings that don't have transactions yet
        List<Booking> availableBookings = bookingService.getBookingsWithoutTransactions();
        
        model.addAttribute("bookings", availableBookings);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "admin/transaction/form";
    }

    /**
     * Handle creation of a new transaction
     */    @PostMapping("/create")
    public String createTransaction(
            @RequestParam("bookingId") Long bookingId,
            @RequestParam("paymentMethod") String paymentMethodStr,
            @RequestParam(value = "cashierName", required = false) String cashierName,
            @RequestParam(value = "paymentProof", required = false) MultipartFile paymentProof,
            @RequestParam(value = "notes", required = false) String notes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        // Convert string to enum
        PaymentMethod paymentMethod;        try {
            logger.info("Converting payment method string: {}", paymentMethodStr);
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr);
            logger.info("Successfully converted to enum: {}", paymentMethod);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to convert payment method: {}", paymentMethodStr, e);
            redirectAttributes.addFlashAttribute("error", "Invalid payment method: " + paymentMethodStr);
            return "redirect:/admin/transaction-management/form";
        }
        
        try {
            // Get the booking
            Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
            if (bookingOpt.isEmpty()) {
                throw new RuntimeException("Booking not found");
            }
            
            Booking booking = bookingOpt.get();
            Transaction transaction = null;
            
            // Get service price as the transaction amount
            BigDecimal amount = booking.getService().getPrice();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Invalid service price");
            }
            
            // Create transaction based on payment method
            if (paymentMethod == PaymentMethod.CASH) {
                transaction = transactionService.createCashPayment(booking, amount, cashierName);
            } else if (paymentMethod == PaymentMethod.CARD) {
                transaction = transactionService.createCardPayment(booking, amount, cashierName);
            } else if (paymentMethod == PaymentMethod.TRANSFER) {
                if (paymentProof != null && !paymentProof.isEmpty()) {
                    transaction = transactionService.createTransferPayment(booking, amount, paymentProof);
                } else {
                    throw new RuntimeException("Payment proof is required for transfer payments");
                }
            }
            
            // Add notes if provided
            if (transaction != null && notes != null && !notes.trim().isEmpty()) {
                transaction.setNotes(notes);
                transactionService.saveTransaction(transaction);
            }
            
            // Get admin user info from session
            User admin = userService.getCurrentUser(session);
            if (admin != null) {
                // If admin creates the transaction, mark it as verified immediately
                transactionService.verifyTransaction(transaction.getIdTransaction(), admin.getUsername());
            }
            
            redirectAttributes.addFlashAttribute("success", "Transaction created successfully");
            return "redirect:/admin/transaction-management";
            
        } catch (Exception e) {
            logger.error("Error creating transaction", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create transaction: " + e.getMessage());
            return "redirect:/admin/transaction-management/form";
        }
    }

    /**
     * Show form to edit an existing transaction
     */
    @GetMapping("/form/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Transaction> transactionOpt = transactionService.getTransactionById(id);
        if (transactionOpt.isEmpty()) {
            return "redirect:/admin/transaction-management";
        }
        
        Transaction transaction = transactionOpt.get();
        model.addAttribute("transaction", transaction);
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        return "admin/transaction/form";
    }

    /**
     * Handle update of an existing transaction
     */
    @PostMapping("/edit/{id}")
    public String updateTransaction(
            @PathVariable("id") Long id,
            @RequestParam("paymentStatus") PaymentStatus paymentStatus,
            @RequestParam(value = "notes", required = false) String notes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<Transaction> transactionOpt = transactionService.getTransactionById(id);
            if (transactionOpt.isEmpty()) {
                throw new RuntimeException("Transaction not found");
            }
            
            Transaction transaction = transactionOpt.get();
            
            // Update payment status
            if (transaction.getPaymentStatus() != paymentStatus) {
                // If status is changing to VALID, verify the transaction
                if (paymentStatus == PaymentStatus.VALID && transaction.getPaymentStatus() != PaymentStatus.VALID) {
                    User admin = userService.getCurrentUser(session);
                    if (admin != null) {
                        transactionService.verifyTransaction(id, admin.getUsername());
                    } else {
                        transactionService.updatePaymentStatus(id, paymentStatus);
                    }
                } else {
                    transactionService.updatePaymentStatus(id, paymentStatus);
                }
            }
            
            // Update notes if provided
            if (notes != null) {
                transactionService.updateNotes(id, notes);
            }
            
            redirectAttributes.addFlashAttribute("success", "Transaction updated successfully");
            return "redirect:/admin/transaction-management";
            
        } catch (Exception e) {
            logger.error("Error updating transaction", e);
            redirectAttributes.addFlashAttribute("error", "Failed to update transaction: " + e.getMessage());
            return "redirect:/admin/transaction-management/form/" + id;
        }
    }

    /**
     * Show transaction details
     */
    @GetMapping("/details/{id}")
    public String showTransactionDetails(@PathVariable("id") Long id, Model model) {
        Optional<Transaction> transactionOpt = transactionService.getTransactionById(id);
        if (transactionOpt.isEmpty()) {
            return "redirect:/admin/transaction-management";
        }
        
        model.addAttribute("transaction", transactionOpt.get());
        return "admin/transaction/details";
    }
}
