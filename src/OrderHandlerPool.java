import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class OrderHandlerPool extends HttpServlet {
        public void init(ServletConfig config) throws ServletException
        {
            super.init(config);
            try {
                Class.forName("oracle.jdbc.driver.OracleDriver");
            } catch (ClassNotFoundException e) {
                throw new UnavailableException(this, "Couldn't load OracleDriver");
            }
        }
        public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            res.setContentType("text/plain");
            PrintWriter out = res.getWriter();
            HttpSession session = req.getSession(true);
            // Try getting the connection holder for this client
            ConnectionHolder holder = (ConnectionHolder) session.getValue("servletapp.connection");
            // Create (and store) a new connection and holder if necessary
            if (holder == null) {
                try {
                    holder = new ConnectionHolder(DriverManager.getConnection( "jdbc:oracle:oci7:ordersdb", "user", "passwd"));
                    session.putValue("servletapp.connection", holder);
                } catch (SQLException e) {
                    getServletContext().log(e, "Couldn't get db connection");
                }
            } // Get the actual connection from the holder
            Connection con = holder.getConnection();
            // Now use the connection
            try {
                Statement stmt = con.createStatement();
                stmt.executeUpdate( "UPDATE INVENTORY SET STOCK = (STOCK - 10) WHERE PRODUCTID = 7");
                stmt.executeUpdate( "UPDATE SHIPPING SET SHIPPED = (SHIPPED + 10) WHERE PRODUCTID = 7");
                // Charge the credit card and commit the transaction in another servlet
                res.sendRedirect("/servlet/CreditCardHandler");
            } catch (Exception e) {
                // Any error is grounds for rollback
                try {
                    con.rollback();
                    session.removeValue("servletapp.connection");
                } catch (Exception ignored) {
                    out.println("Order failed. Please contact technical support.");
                }
            }
        }
    }

