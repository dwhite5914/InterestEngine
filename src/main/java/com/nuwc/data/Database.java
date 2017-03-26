package com.nuwc.data;

import com.nuwc.interestengine.Utils;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Database
{
    private Connection connection;

    public Database()
    {
        // Initialize the embedded database driver.
        initDriver();

        // Open a new connection to the database.
        openConnection();
    }

    private void initDriver()
    {
        // Initialize the embedded database driver.
        try
        {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Failed to find driver.");
        }
    }

    private void openConnection()
    {
        try
        {
            // Create root directory for database if it doesn't exist.
            File dbDir = new File(Utils.getResource("") + "/database");
            if (!dbDir.exists())
            {
                dbDir.mkdir();
            }

            // Get path to database.
            String path = new File(dbDir.getAbsolutePath() + "/ships").getAbsolutePath();
            String url = String.format("jdbc:derby:%s;create=true", path);

            // Attempt connection to database.
            connection = DriverManager.getConnection(url);
        }
        catch (SQLException e)
        {
            System.out.println("Failed to connect to database.");
        }
    }

    public void runStatement(String sql)
    {
        // Run an sql statement on the database.
        try
        {
            connection.createStatement().execute(sql);
        }
        catch (SQLException e)
        {
            System.out.println("Failed to execute statment.");
        }
    }

    public ResultSet runQuery(String sql)
    {
        // Run a query on the database and return the result.
        ResultSet result = null;
        try
        {
            result = connection.createStatement().executeQuery(sql);
        }
        catch (SQLException e)
        {
            System.out.println("Failed to execute query.");
        }

        return result;
    }

    public boolean exists()
    {
        String sql = "SELECT * FROM VesselData\n";

        try
        {
            connection.createStatement().executeQuery(sql);
        }
        catch (SQLException e)
        {
            return false;
        }

        return true;
    }

    public int getTableSize(String tableName)
    {
        // Build a query to count the elements in the given table.
        String sql = String.format("SELECT COUNT(*) FROM %s\n", tableName);

        // Run the query and store the result.
        ResultSet result = null;
        try
        {
            result = connection.createStatement().executeQuery(sql);
        }
        catch (SQLException e)
        {
            System.out.println("Failed to execute query.");
        }

        // Return 0 if result is null.
        if (result == null)
        {
            return 0;
        }

        // Retrive the size of the table from the result.
        int size = 0;
        try
        {
            while (result.next())
            {
                size = result.getInt(1);
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to read SQL result.");
        }

        return size;
    }

    public List<Object[]> getTableData()
    {
        List<Object[]> points = new ArrayList<>();

        try
        {
            String sql = "SELECT * FROM VesselData\n";
            ResultSet result = runQuery(sql);

            while (result.next())
            {
                Object[] point = new Object[8];
                point[0] = result.getInt("id");
                point[1] = result.getInt("mmsi");
                point[2] = result.getFloat("lat");
                point[3] = result.getFloat("lon");
                point[4] = result.getFloat("sog");
                point[5] = result.getFloat("cog");
                point[6] = result.getString("shipType");
                point[7] = result.getLong("timestamp");

                points.add(point);
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to read dynamic data.");
        }

        return points;
    }

    public List<DataPoint> getVesselData()
    {
        List<DataPoint> points = new ArrayList<>();

        try
        {
            String sql = "SELECT * FROM VesselData";
            ResultSet result = runQuery(sql);

            while (result.next())
            {
                DataPoint point = new DataPoint();
                point.mmsi = result.getInt("mmsi");
                point.lat = result.getFloat("lat");
                point.lon = result.getFloat("lon");
                point.sog = result.getFloat("sog");
                point.cog = result.getFloat("cog");
                point.shipType = result.getString("shipType");
                point.timestamp = result.getLong("timestamp");

                points.add(point);
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to read vessel data.");
        }

        return points;
    }
}
