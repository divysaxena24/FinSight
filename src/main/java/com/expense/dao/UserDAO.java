package com.expense.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.expense.model.User;
import com.expense.util.DBConnection;

public class UserDAO 
{

    // Register User
    public boolean registerUser(User user) 
    {
    	boolean status = false;

        try
        {
            Connection con = DBConnection.getConnection();

            String sql = "INSERT INTO user(name,email,password) VALUES(?,?,?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());

            int row = ps.executeUpdate();

            if (row > 0)
                status = true;

        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return status;
    }

    // Login User
    public User loginUser(String email, String password)
    {

        User user = null;

        try
        {

            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM user WHERE email=? AND password=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next())
            {

                user = new User();

                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setEmail(rs.getString("email"));

            }

        } 
        catch (Exception e) 
        {

            e.printStackTrace();

        }

        return user;
    }
}