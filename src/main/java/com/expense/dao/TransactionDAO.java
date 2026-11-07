package com.expense.dao;

import java.sql.Connection;
//import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.expense.model.Transaction;
import com.expense.util.DBConnection;

public class TransactionDAO
{
	// Add Transaction
    public boolean addTransaction(Transaction transaction)
    {

        boolean status = false;
        try 
        {
            Connection con = DBConnection.getConnection();

            String sql = "INSERT INTO transactions(user_id,type,category,amount,description,transaction_date) VALUES(?,?,?,?,?,?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, transaction.getUserId());
            ps.setString(2, transaction.getType());
            ps.setString(3, transaction.getCategory());
            ps.setDouble(4, transaction.getAmount());
            ps.setString(5, transaction.getDescription());
            ps.setDate(6, transaction.getTransactionDate());

            status = ps.executeUpdate() > 0;

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }

        return status;
    }

    // View Transactions
    public List<Transaction> getTransactionsByUser(int userId) 
    {

        List<Transaction> list = new ArrayList<>();

        try 
        {

            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM transactions WHERE user_id=? ORDER BY transaction_date DESC";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) 
            {

                Transaction t = new Transaction();

                t.setId(rs.getInt("id"));
                t.setUserId(rs.getInt("user_id"));
                t.setType(rs.getString("type"));
                t.setCategory(rs.getString("category"));
                t.setAmount(rs.getDouble("amount"));
                t.setDescription(rs.getString("description"));
                t.setTransactionDate(rs.getDate("transaction_date"));

                list.add(t);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return list;
    }

    // Update Transaction
    public boolean updateTransaction(Transaction transaction) 
    {

        boolean status = false;

        try 
        {

            Connection con = DBConnection.getConnection();

            String sql = "UPDATE transactions SET type=?,category=?,amount=?,description=?,transaction_date=? WHERE id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, transaction.getType());
            ps.setString(2, transaction.getCategory());
            ps.setDouble(3, transaction.getAmount());
            ps.setString(4, transaction.getDescription());
            ps.setDate(5, transaction.getTransactionDate());
            ps.setInt(6, transaction.getId());

            status = ps.executeUpdate() > 0;

        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return status;
    }

    // Delete Transaction
    public boolean deleteTransaction(int id) 
    {

        boolean status = false;

        try 
        {

            Connection con = DBConnection.getConnection();

            String sql = "DELETE FROM transactions WHERE id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, id);

            status = ps.executeUpdate() > 0;

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }

        return status;
    }
    public Transaction getTransactionById(int id) {

        Transaction transaction = null;

        try {

            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM transactions WHERE id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                transaction = new Transaction();

                transaction.setId(rs.getInt("id"));
                transaction.setUserId(rs.getInt("user_id"));
                transaction.setType(rs.getString("type"));
                transaction.setCategory(rs.getString("category"));
                transaction.setAmount(rs.getDouble("amount"));
                transaction.setDescription(rs.getString("description"));
                transaction.setTransactionDate(rs.getDate("transaction_date"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return transaction;
    }

    // Total Income
    public double getTotalIncome(int userId) 
    {

        double total = 0;

        try 
        {

            Connection con = DBConnection.getConnection();

            String sql = "SELECT SUM(amount) FROM transactions WHERE user_id=? AND type='Income'";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) 
            {
                total = rs.getDouble(1);
            }

        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }

        return total;
    }

    // Total Expense
    public double getTotalExpense(int userId) 
    {

        double total = 0;

        try 
        {

            Connection con = DBConnection.getConnection();

            String sql = "SELECT SUM(amount) FROM transactions WHERE user_id=? AND type='Expense'";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) 
            {
                total = rs.getDouble(1);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return total;
    }

}