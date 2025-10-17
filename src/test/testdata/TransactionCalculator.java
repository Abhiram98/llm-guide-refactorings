package org.boulderse.ijserver.testdata;

public class TransactionCalculator {
    public static void main(String[] args) {
        try {
            long startTime = System.currentTimeMillis();
            double totalAmount = 0;
            double taxRate = 0.07;
            double discountRate = 0.1;
            int customerType = 2; // 1 for regular, 2 for premium, 3 for VIP
            double[] prices = {19.99, 5.49, 10.00, 15.75, 3.50, 8.99};
            int[] quantities = {3, 10, 5, 2, 4, 6};
            double sum = 0;

            for (int i = 0; i < prices.length; i++) {
                double price = prices[i];
                int quantity = quantities[i];
                double itemTotal = price * quantity;
                if (customerType == 1) {
                    itemTotal *= (1 - discountRate);
                } else if (customerType == 2) {
                    itemTotal *= (1 - (discountRate / 2));
                } else if (customerType == 3) {
                    itemTotal *= (1 - (discountRate * 2));
                }
                sum += itemTotal;
            }

            totalAmount = sum + (sum * taxRate);
            System.out.println("Total Amount: " + totalAmount);

            String[] inventory = {"Shirt", "Pants", "Hat", "Shoes", "Socks", "Gloves"};
            double inventorySum = 0;
            for (int i = 0; i < inventory.length; i++) {
                if (inventory[i].equals("Shirt")) {
                    inventorySum += 19.99;
                } else if (inventory[i].equals("Pants")) {
                    inventorySum += 29.99;
                } else if (inventory[i].equals("Hat")) {
                    inventorySum += 9.99;
                } else if (inventory[i].equals("Shoes")) {
                    inventorySum += 49.99;
                } else if (inventory[i].equals("Socks")) {
                    inventorySum += 4.99;
                } else if (inventory[i].equals("Gloves")) {
                    inventorySum += 12.99;
                }
            }

            System.out.println("Inventory Total: " + inventorySum);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("Execution time: " + duration + "ms");
        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }
}
