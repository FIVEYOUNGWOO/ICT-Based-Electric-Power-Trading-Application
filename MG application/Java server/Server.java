
// import java buffer
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

// import bluetooth
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// import sqlitev3
import java.sql.*;
import org.sqlite.JDBC;

// import Calendar
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Server 
{
	public static void main(String[] args) 
	{
		// A local device is an android or other types of phone
		// *IOS has not been tested*
		LocalDevice local = null;
		try 
		{
			local = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e2) {}

		// Display console logger
		log("address: " + local.getBluetoothAddress());
		log("name: " + local.getFriendlyName());

		// Turn on thread and server channel
		Runnable r = new ServerRunable();
		Thread thread = new Thread(r);
		thread.start();
	}

	// Print of the message
	private static void log(String msg) 
	{
		System.out.println("[" + (new Date()) + "] " + msg);
	}
}

class ServerRunable implements Runnable 
{
	// UUID for SPP
	final UUID uuid = new UUID("0000110100001000800000805F9B34FB", false);
	final String CONNECTION_URL_FOR_SPP = "btspp://localhost:" + uuid + ";name=SPP Server";

	// SPP server connection reader
	private StreamConnectionNotifier mStreamConnectionNotifier = null;
	private StreamConnection mStreamConnection = null;
	private int count = 0;

	@Override
	public void run() 
	{
		try 
		{
			// UUID-based connect to SPP server with android (BLE)
			mStreamConnectionNotifier = (StreamConnectionNotifier) Connector.open(CONNECTION_URL_FOR_SPP);
		} catch (IOException e) {log("Could not open connection: " + e.getMessage()); return;}
		
		log("Server is now running.");

		while (true) 
		{
			log("wait for client requests...");
			
			try 
			{
				mStreamConnection = mStreamConnectionNotifier.acceptAndOpen();
			} catch (IOException e1) {log("Could not open connection: " + e1.getMessage());}
			
			// Count for accessed client
			count++;
			log("현재 접속 중인 클라이언트 수: " + count);
			new Receiver(mStreamConnection).start();
		}
	}

	class Receiver extends Thread 
	{
		// Initiate the stream buffer
		private InputStream mInputStream = null;
		private OutputStream mOutputStream = null;
		private String mRemoteDeviceString = null;
		private StreamConnection mStreamConnection = null;

		// Operating SPP server receiver
		Receiver(StreamConnection streamConnection) 
		{
			mStreamConnection = streamConnection;

			try 
			{
				mInputStream = mStreamConnection.openInputStream();
				mOutputStream = mStreamConnection.openOutputStream();
				
				log("Open streams...");
				
			} catch (IOException e) {log("Couldn't open Stream: " + e.getMessage()); Thread.currentThread().interrupt(); return;}
			
			try 
			{
				RemoteDevice remoteDevice = RemoteDevice.getRemoteDevice(mStreamConnection);
				mRemoteDeviceString = remoteDevice.getBluetoothAddress();
				
				log("Remote device");
				log("address: " + mRemoteDeviceString);
				
			} catch (IOException e1) { log("Found device, but couldn't connect to it: " + e1.getMessage());return; }
			
			log("Client is connected...");
		}

		@Override
		public void run() 
		{
			try 
			{	
				// SQLite3 DB connector
				Connection con = null;
				Statement stmt = null;
				ResultSet rs = null;
				String remain = null;
				
				// Connect to Database URL
				String dbFileUrl = "jdbc:sqlite:PowerUsage.db";

				Reader mReader = new BufferedReader(new InputStreamReader(mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));
				boolean isDisconnected = false;
				
				try 
				{
					// Access to specific User-defined Table
					Class.forName("org.sqlite.JDBC");
					con = DriverManager.getConnection(dbFileUrl);
					stmt = con.createStatement();
					rs = stmt.executeQuery("select Usable_Power from PowerUsage_data;");

					// Execution of query statement and return DB values
					if (rs.next()) 
					{
						do 
						{
							remain = rs.getString(1);
						} while (rs.next());
					}
					
					// Add the token key(R:) string
					// SPP server send the DB value to the connected device by using the Sender function()
					remain = ("R:" + remain); Sender(remain);} catch (Exception e) {System.out.println(e);}

				while (true) 
				{
					StringBuilder stringBuilder = new StringBuilder();
					int c = 0;
					
					// Checking disconnect by using ("\n") and control the multiple thread
					while ('\n' != (char) (c = mReader.read())) 
					{
						if (c == -1) 
						{
							log("Client has been disconnected");
							count--;
							log("현재 접속 중인 클라이언트 수: " + count);
							isDisconnected = true;
							Thread.currentThread().interrupt();
							break;
						}
						stringBuilder.append((char) c);
					}

					if (isDisconnected) break;

					// Get received message from connected phone
					String recvMessage = stringBuilder.toString();
					log(mRemoteDeviceString + ": " + recvMessage);


					// When click the Update button
					if (recvMessage.contains("refresh")) 
					{
						// SQLite3 DB connector
						con = null;
						stmt = null;
						rs = null;
						remain = null;
						
						// Connect to Database URL
						dbFileUrl = "jdbc:sqlite:PowerUsage.db";
						
							// Access to specific User-defined Table
							try {Class.forName("org.sqlite.JDBC");} catch (ClassNotFoundException e) 
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							con = DriverManager.getConnection(dbFileUrl);
							stmt = con.createStatement();
							rs = stmt.executeQuery("select Usable_Power from PowerUsage_data;");

							// Execution of query statement and return DB values
							if (rs.next()) 
							{
								do 
								{
									remain = rs.getString(1);
								} while (rs.next());
							}
							
							// Add the token key(R:) string
							// SPP server send the DB value to the connected device by using the Sender function()
							remain = ("R:" + remain); 
							Sender(remain);
							
					}
					
					// When click the charge button
					else if (recvMessage.contains("charge_log"))
					{
						// SQLite3 DB connector
						String charge_recv = recvMessage;
						String dbFileUrl_p = "jdbc:sqlite:charge.db";
						Connection con_p = null; // 커넥터
						Statement stmt_p = null;
						ResultSet rs_p = null;

						try
						{
							// Access to specific User-defined Table
							Class.forName("org.sqlite.JDBC");
							con_p = DriverManager.getConnection(dbFileUrl_p);
							stmt_p = con_p.createStatement();
							rs_p = stmt_p.executeQuery("select * from charge_log;");
							stmt_p.executeUpdate(charge_recv);
						} catch (Exception e) {System.out.println(e);}
					}

					// When click the User_info button
					else if (recvMessage.contains("MYUS")) 
					{
						// SQLite3 DB connector
						Connection con_u = null;
						Statement stmt_u = null;
						ResultSet rs_u = null;
						String user_info = null;
						String dbFileUrl_u = "jdbc:sqlite:User.db";
						try
						{
							// Access to specific User-defined Table
							Class.forName("org.sqlite.JDBC");
							con_u = DriverManager.getConnection(dbFileUrl_u);
							stmt_u = con_u.createStatement();
							rs_u = stmt_u.executeQuery("select * from user_data;");

							// Get all row (TIMESTAMP, Device_ID, Name, Option1, Option2, Option3)
							// rs_u.getString(1) pointing the TIMESTAMP columns
							// rs_u.getString(2) pointing the Device_ID columns
							// rs_u.getString(3) pointing the Name columns
							// rs_u.getString(4) pointing the Option 1 columns
							// rs_u.getString(5) pointing the Option 2 columns
							// rs_u.getString(6) pointing the Option 3 columns
							if (rs_u.next()) 
							{
								do 
								{
									user_info = (rs_u.getString(1) + "," + rs_u.getString(2) + "," + rs_u.getString(3) + "," + rs_u.getString(4) + "," + rs_u.getString(5) + "," + rs_u.getString(6));
								} while (rs_u.next());
							}
							// Add the token key(U:) string
							// SPP server send the DB value to the connected device by using the Sender function()
							user_info = ("U:" + user_info);
							Sender(user_info);
						} catch (Exception e) {System.out.println(e);}
					}

					// When click the User_info update button
					else if (recvMessage.contains("user_data"))
					{
						// SQLite3 DB connector
						String user_recv = recvMessage;
						String dbFileUrl_u = "jdbc:sqlite:User.db";
						Connection con_u = null; // 커넥터
						Statement stmt_u = null;
						ResultSet rs_u = null;

						try 
						{
							// Access to specific User-defined Table
							Class.forName("org.sqlite.JDBC");
							con_u = DriverManager.getConnection(dbFileUrl_u);
							stmt_u = con_u.createStatement();
							rs_u = stmt_u.executeQuery("select * from user_data;");
							stmt_u.executeUpdate(user_recv);
						} catch (Exception e) {System.out.println(e);}
					}
					
					// When click the calendar button-based date inquiry
					else if (recvMessage.contains("|and|")) 
					{
						// Replace the specific string key("|and|") convert to "and"
						String TMP_Mess = recvMessage;
						TMP_Mess = TMP_Mess.replace("|and|", "and");
						String[] INPUT_RANGE = TMP_Mess.split("and");

						// Initialization count, and i value
						int count = 0;
						int i = 0;

						// For get x-axis (start date to end date)
						String XaxisSTART = "";
						String XaxisEND = "";
						
						// For convert date-form 
						String INIT_START = "";
						String INIT_END = "";
						String s1 = "";
						String s2 = "";
						
						// Combine the query statements
						String Start_Qry_Unit = "select count(*) from PowerUsage_data where TIMESTAMP between";
						String End_Qry_Unit = "and Power NOT NULL and Usable_Power NOT NULL;";

						// Separate the start date - end date
						for (int j = 0; j < INPUT_RANGE.length; j++)
						{
							// Get start date
							INIT_START = INPUT_RANGE[0];
							
							// Get end date
							INIT_END = INPUT_RANGE[1];
						}
						// Input each s1, s2 parameters
						s1 = INIT_START;
						s2 = INIT_END;

						// To temporary store the start, end values
						String TMP_STORE_START = "";
						String TMP_STORE_END = "";
						
						// To get c1 value
						Date Comb = null;
						
						// To calculation of the user-defined range
						String dateToStr = "";
						String DUP_Range = "";

						// Define the date to format (year-month-day)
						DateFormat dataToformat = new SimpleDateFormat("yyyy-MM-dd");

						// To convert input(s1, s2) to date-form (d1, d2)
						Date d1 = dataToformat.parse(s1);
						Date d2 = dataToformat.parse(s2);

						// Get instance by using calendar function()
						Calendar c1 = Calendar.getInstance();
						Calendar c2 = Calendar.getInstance();

						// To temporary store Power, Usable, and Combined query statements
						String TMP_Sum_Power = "";
						String TMP_Sum_Usable = "";
						String TMP_COMBI_DATE_QRY = "";
						
						// To cumulative calculation of Power, and Usable
						String TOTAL_SUM_POWER = "";
						String TOTAL_SUM_USABLE = "";
						
						// To add 1 day at a time with the add() method
						c1.setTime(d1);
						c2.setTime(d2);

						// To calculate the difference in milliseconds and days between two user-input date ranges
						long diff = (d2.getTime() - d1.getTime());
						long diffDays = (diff / (24 * 60 * 60 * 1000) + 1);

						// If the interval entered by the user is less than 7 days
						if (diffDays != 0 && diffDays <= 7) 
						{
							// The data increases by 1 in the starting interval
							// Calculation of cumulative calculated date range
							while (c1.compareTo(c2) != 1) 
							{
								count++;
								Comb = (c1.getTime());
								dateToStr = dataToformat.format(Comb);
								dateToStr = (dateToStr + ",");
								DUP_Range += (dateToStr);
								c1.add(Calendar.DATE, 1);
							}

							// Separate the calculated date range
							String[] StartDatearr = DUP_Range.split(",");

							for (i = 0; i < count; i++) 
							{
								// Generate of user input-based date range
								TMP_STORE_START = (StartDatearr[i] + " 00:00:00");
								TMP_STORE_END = (StartDatearr[i] + " 23:59:59");
								
								// Calculation of X-axis (start index to end index)
								XaxisSTART += (TMP_STORE_START + ","); 
								XaxisEND += (TMP_STORE_END + ","); 
								
								// Generate of query statements
								TMP_COMBI_DATE_QRY += (Start_Qry_Unit + "'" + TMP_STORE_START + "'" + " and " + "'" + TMP_STORE_END + "'" + End_Qry_Unit + "\n");
							}
							
							// Separate the calculated date range
							String[] D_TOTAL_RANGE = TMP_COMBI_DATE_QRY.split("\n");

							// Operating query statements
							for (i = 0; i < D_TOTAL_RANGE.length; i++) 
							{
								String temp = D_TOTAL_RANGE[i];
								if (temp.contains("count(*)")) 
								{
									// Replace the query statements 
									String Count_COMM = temp;
									String Extract_COMM = Count_COMM.replace("count(*)" , "avg(Power), avg(Usable_Power)");
									
									// Access to specific User-defined Table
									Connection con_t = null;
									Statement stmt_t = null;
									ResultSet rs_z = null;
									String dbFileUrl_t = "jdbc:sqlite:PowerUsage.db";

									try
									{
										Class.forName("org.sqlite.JDBC");
										con_t = DriverManager.getConnection(dbFileUrl_t);
										stmt_t = con_t.createStatement();

										// Execution of the Extract query statements
										rs_z = stmt_t.executeQuery(Extract_COMM);

										// Calculation of cumulative calculated Power and Expected value
										while (rs_z.next())
										{
											String Temp_Power = (rs_z.getString(1) + ","); TMP_Sum_Power += Temp_Power;
											String Temp_EXPECTED = (rs_z.getString(2) + ","); TMP_Sum_Usable += Temp_EXPECTED;
										}

									} catch (Exception e) {System.out.println(e);}
								}
							}
							// Replace "NULL" to "0"
							TMP_Sum_Power = TMP_Sum_Power.replaceAll("null", "0");
							TMP_Sum_Usable = TMP_Sum_Usable.replaceAll("null", "0");

							// Separate the Power and Expected value by using token key(",")
							String[] KEEP_POWER_ZERO_INT = TMP_Sum_Power.split(",");
							String[] KEEP_EXPECTED_ZERO_INT = TMP_Sum_Usable.split(",");

							// Convert String to Double types
							for (int q = 0; q < KEEP_POWER_ZERO_INT.length; q++) 
							{
								String COMBI_Power = (((int) Double.parseDouble(KEEP_POWER_ZERO_INT[q])) + ","); TOTAL_SUM_POWER += COMBI_Power;
								String COMBI_EXPECTED = (((int) Double.parseDouble(KEEP_EXPECTED_ZERO_INT[q])) + ","); TOTAL_SUM_USABLE += COMBI_EXPECTED;
							}
							
							// Add Token("XS -> X-axis start, XE -> X-axis end, P -> Power, EX -> Expected") string
							XaxisSTART = ("XS:" + XaxisSTART);
							XaxisEND = ("XE:" + XaxisEND);
							TOTAL_SUM_POWER = ("P:" + TOTAL_SUM_POWER);
							TOTAL_SUM_USABLE = ("EX:" + TOTAL_SUM_USABLE);
							
							// Broadcast the result of query
							Sender(XaxisSTART); 
							Sender(XaxisEND); 
							Sender(TOTAL_SUM_POWER); 
							Sender(TOTAL_SUM_USABLE); 
						}

						// Between 7 and 30
						else if (diffDays > 7 && diffDays <= 30)
						{
							// The data increases by 7 in the starting interval
							// Calculation of cumulative calculated date range
							while (c1.compareTo(c2) != 1) 
							{
								count++;
								Comb = (c1.getTime());
								dateToStr = dataToformat.format(Comb);
								dateToStr = (dateToStr + ",");
								DUP_Range += (dateToStr);
								c1.add(Calendar.DATE, 6);
							}
							// Separate the calculated date range
							String[] StartDatearr = DUP_Range.split(",");
							
							for (i = 0; i < count - 1; i++) 
							{
								// generate of user input-based date range
								TMP_STORE_START = (StartDatearr[i] + " 00:00:00");
								TMP_STORE_END = (StartDatearr[i+1] + " 23:59:59");
								
								// calculation of X-axis (start index to end index)
								XaxisSTART += (TMP_STORE_START + ","); 
								XaxisEND += (TMP_STORE_END + ","); 
								
								// generate of query statements
								TMP_COMBI_DATE_QRY += (Start_Qry_Unit + "'" + TMP_STORE_START + "'" + " and " + "'" + TMP_STORE_END + "'" + End_Qry_Unit + "\n");
							}
							
							String[] W_TOTAL_RANGE = TMP_COMBI_DATE_QRY.split("\n");

							for (i = 0; i < W_TOTAL_RANGE.length; i++) 
							{
								String temp = W_TOTAL_RANGE[i];
								if (temp.contains("count(*)")) 
								{
									// Replace the query statements 
									String Count_COMM = temp;
									String Extract_COMM = Count_COMM.replace("count(*)", "avg(Power), avg(Usable_Power)");

									// Access to specific User-defined Table
									Connection con_t = null; 
									Statement stmt_t = null;
									ResultSet rs_z = null;
									String dbFileUrl_t = "jdbc:sqlite:PowerUsage.db";

									try 
									{
										Class.forName("org.sqlite.JDBC");
										con_t = DriverManager.getConnection(dbFileUrl_t);
										stmt_t = con_t.createStatement();
										
										// Execution of the Extract query statements
										rs_z = stmt_t.executeQuery(Extract_COMM);

										// Calculation of cumulative calculated Power and Expected value
										while (rs_z.next()) 
										{
											String Temp_Power = (rs_z.getString(1) + ","); TMP_Sum_Power += Temp_Power;
											String Temp_EXPECTED = (rs_z.getString(2) + ","); TMP_Sum_Usable += Temp_EXPECTED;
										}

									} catch (Exception e) {System.out.println(e);}
								}
							}
							// Replace "NULL" to "0"
							TMP_Sum_Power = TMP_Sum_Power.replaceAll("null", "0");
							TMP_Sum_Usable = TMP_Sum_Usable.replaceAll("null", "0");

							// Separate the Power and Expected value by using token key(",")
							String[] KEEP_POWER_ZERO_INT = TMP_Sum_Power.split(",");
							String[] KEEP_EXPECTED_ZERO_INT = TMP_Sum_Usable.split(",");

							for (int q = 0; q < KEEP_POWER_ZERO_INT.length; q++) 
							{
								// Convert String to Double types
								String COMBI_Power = (((int) Double.parseDouble(KEEP_POWER_ZERO_INT[q])) + ","); TOTAL_SUM_POWER += COMBI_Power;
								String COMBI_EXPECTED = (((int) Double.parseDouble(KEEP_EXPECTED_ZERO_INT[q])) + ","); TOTAL_SUM_USABLE += COMBI_EXPECTED;
							}
							// Add Token("XS -> X-axis start, XE -> X-axis end, P -> Power, EX -> Expected") string
							XaxisSTART = ("XS:" + XaxisSTART);
							XaxisEND = ("XE:" + XaxisEND);
							TOTAL_SUM_POWER = ("P:" + TOTAL_SUM_POWER);
							TOTAL_SUM_USABLE = ("EX:" + TOTAL_SUM_USABLE);
							
							// Broadcast the result of query
							Sender(XaxisSTART); 
							Sender(XaxisEND); 
							Sender(TOTAL_SUM_POWER); 
							Sender(TOTAL_SUM_USABLE); 
						}
						
						// Between 30 and 365
						else if (diffDays > 30 && diffDays <= 365) 
						{
							while (c1.compareTo(c2) != 1) 
							{
								count++;
								Comb = (c1.getTime());
								dateToStr = dataToformat.format(Comb);
								dateToStr = (dateToStr + ",");
								DUP_Range += (dateToStr);
								c1.add(Calendar.DATE, 30);
							}

							String[] StartDatearr = DUP_Range.split(",");
							// get Start
							for (i = 0; i < count - 1; i++) 
							{
								// generate of user input-based date range
								TMP_STORE_START = (StartDatearr[i] + " 00:00:00");
								TMP_STORE_END = (StartDatearr[i+1] + " 23:59:59");
								
								// calculation of X-axis (start index to end index)
								XaxisSTART += (TMP_STORE_START + ","); 
								XaxisEND += (TMP_STORE_END + ","); 
								
								// generate of query
								TMP_COMBI_DATE_QRY += (Start_Qry_Unit + "'" + TMP_STORE_START + "'" + " and " + "'" + TMP_STORE_END + "'" + End_Qry_Unit + "\n");
							}

							String[] M_TOTAL_RANGE = TMP_COMBI_DATE_QRY.split("\n");

							for (i = 0; i < M_TOTAL_RANGE.length; i++) 
							{
								String temp = M_TOTAL_RANGE[i];
								if (temp.contains("count(*)")) 
								{
									String Count_COMM = temp;
									String Extract_COMM = Count_COMM.replace("count(*)", "avg(Power), avg(Usable_Power)");
									Connection con_t = null; 
									Statement stmt_t = null;
									
									// Execution of the Extract query statements
									ResultSet rs_z = null;

									String Temp_Power = "";
									String Temp_EXPECTED = "";
									String dbFileUrl_t = "jdbc:sqlite:PowerUsage.db";

									try 
									{
										Class.forName("org.sqlite.JDBC");
										con_t = DriverManager.getConnection(dbFileUrl_t);
										stmt_t = con_t.createStatement();
										rs_z = stmt_t.executeQuery(Extract_COMM);
										
										// Calculation of cumulative calculated Power and Expected value
										while (rs_z.next())
										{
											Temp_Power = (rs_z.getString(1) + ","); TMP_Sum_Power += Temp_Power;
											Temp_EXPECTED = (rs_z.getString(2) + ","); TMP_Sum_Usable += Temp_EXPECTED;
										}

									} catch (Exception e) {System.out.println(e);}
								}
							}
							// Replace "NULL" to "0"
							TMP_Sum_Power = TMP_Sum_Power.replaceAll("null", "0");
							TMP_Sum_Usable = TMP_Sum_Usable.replaceAll("null", "0");

							// Separate the Power and Expected value by using token key(",")
							String[] KEEP_POWER_ZERO_INT = TMP_Sum_Power.split(",");
							String[] KEEP_EXPECTED_ZERO_INT = TMP_Sum_Usable.split(",");

							for (int q = 0; q < KEEP_POWER_ZERO_INT.length; q++) 
							{
								// Convert String to Double types
								String COMBI_Power = (((int) Double.parseDouble(KEEP_POWER_ZERO_INT[q])) + ","); TOTAL_SUM_POWER += COMBI_Power;
								String COMBI_EXPECTED = (((int) Double.parseDouble(KEEP_EXPECTED_ZERO_INT[q])) + ","); TOTAL_SUM_USABLE += COMBI_EXPECTED;
							}
							
							// Add Token("XS -> X-axis start, XE -> X-axis end, P -> Power, EX -> Expected") string
							XaxisSTART = ("XS:" + XaxisSTART);
							XaxisEND = ("XE:" + XaxisEND);
							TOTAL_SUM_POWER = ("P:" + TOTAL_SUM_POWER);
							TOTAL_SUM_USABLE = ("EX:" + TOTAL_SUM_USABLE);
							
							// Broadcast the result of query
							Sender(XaxisSTART); 
							Sender(XaxisEND); 
							Sender(TOTAL_SUM_POWER); 
							Sender(TOTAL_SUM_USABLE); 
						}
					}
					
					// When click the time button-based date inquiry
					else if (recvMessage.contains("*and*")) 
					{
						// Replace the specific string key("*and*") convert to "and"
						String TMP_Mess = recvMessage;
						TMP_Mess = TMP_Mess.replace("*and*", "and");
						String[] INPUT_RANGE = TMP_Mess.split("and");

						// Initialization count, and i value
						int count = 0;
						int i = 0;

						// For convert date-form 
						String INIT_START = "";
						String INIT_END = "";
						String s1 = "";
						String s2 = "";
						
						// For get x-axis (start date to end date)
						String XaxisSTART = "";
						String XaxisEND = "";
						
						String TMP_STORE_START = "";
						String TMP_STORE_END = "";
						String TMP_COMBI_DATE_QRY = "";
						
						// Combine the query statements
						String Start_Qry_Unit = "select count(*) from PowerUsage_data where TIMESTAMP between";
						String End_Qry_Unit = "and Power NOT NULL and Usable_Power NOT NULL;";

						// Separate the start date - end date
						for (int j = 0; j < INPUT_RANGE.length; j++) 
						{
							// Get start time
							INIT_START = INPUT_RANGE[0];
							
							// Get end time
							INIT_END = INPUT_RANGE[1];
						}
						// Input each s1, s2 parameters
						s1 = INIT_START;
						s2 = INIT_END;

						// To calculation of the user-defined range
						String TMP_STORE_RANGE = "";
						Date Comb = null;
						String dateToStr = "";
						String DUP_Range = "";

						// Define the date to format (year-month-day)
						DateFormat dataToformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

						// To convert input(s1, s2) to date-form (d1, d2)
						Date d1 = dataToformat.parse(s1);
						Date d2 = dataToformat.parse(s2);

						// Get instance by using calendar function()
						Calendar c1 = Calendar.getInstance();
						Calendar c2 = Calendar.getInstance();

						// To temporary store Power, Usable, and Combined query statements
						String TMP_Sum_Power = "";
						String TMP_Sum_Usable = "";

						// To cumulative calculation of Power, and Usable
						String TOTAL_SUM_POWER = "";
						String TOTAL_SUM_USABLE = "";

						// To add 1 day at a time with the add() method
						c1.setTime(d1);
						c2.setTime(d2);

						while (c1.compareTo(c2) != 1) 
						{
							// The minute increases by 10 in the starting interval
							// Calculation of cumulative calculated date range
							count++;
							Comb = (c1.getTime());
							dateToStr = dataToformat.format(Comb);
							dateToStr = (dateToStr + ",");
							DUP_Range += (dateToStr);
							c1.add(Calendar.MINUTE, 10);
						}

						// Separate the calculated date range
						String[] StartDatearr = DUP_Range.split(",");

						for (i = 0; i < count - 1; i++) 
						{
							
							// generate of user input-based date range
							TMP_STORE_START = (StartDatearr[i]);
							TMP_STORE_END = (StartDatearr[i+1]);
							
							// calculation of X-axis (start index to end index)
							XaxisSTART += (TMP_STORE_START + ","); 
							XaxisEND += (TMP_STORE_END + ","); 
							
							// generate of query
							TMP_COMBI_DATE_QRY += (Start_Qry_Unit + "'" + TMP_STORE_START + "'" + " and " + "'" + TMP_STORE_END + "'" + End_Qry_Unit + "\n");
						}
						String[] T_TOTAL_RANGE = TMP_COMBI_DATE_QRY.split("\n");

						for (i = 0; i < T_TOTAL_RANGE.length; i++) 
						{
							String temp = T_TOTAL_RANGE[i];
							if (temp.contains("count(*)")) 
							{
								
								String Count_COMM = temp;
								String Extract_COMM = Count_COMM.replace("count(*)", "avg(Power), avg(Usable_Power)");

								Connection con_t = null; // 커넥터
								Statement stmt_t = null;
								
								// Execution of the Extract query statements
								ResultSet rs_z = null;
								String dbFileUrl_t = "jdbc:sqlite:PowerUsage.db";

								try 
								{
									Class.forName("org.sqlite.JDBC");
									con_t = DriverManager.getConnection(dbFileUrl_t);
									stmt_t = con_t.createStatement();
									rs_z = stmt_t.executeQuery(Extract_COMM);

									while (rs_z.next()) 
									{
										// Calculation of cumulative calculated Power and Expected value
										String Temp_Power = (rs_z.getString(1) + ","); TMP_Sum_Power += Temp_Power;
										String Temp_EXPECTED = (rs_z.getString(2) + ","); TMP_Sum_Usable += Temp_EXPECTED;
									}

								} catch (Exception e) {System.out.println(e);}
							}
						}
						// Replace "NULL" to "0"
						TMP_Sum_Power = TMP_Sum_Power.replaceAll("null", "0");
						TMP_Sum_Usable = TMP_Sum_Usable.replaceAll("null", "0");

						// Separate the Power and Expected value by using token key(",")
						String[] KEEP_POWER_ZERO_INT = TMP_Sum_Power.split(",");
						String[] KEEP_EXPECTED_ZERO_INT = TMP_Sum_Usable.split(",");

						for (int q = 0; q < KEEP_POWER_ZERO_INT.length; q++) 
						{
							// Convert String to Double types
							String COMBI_Power = (((int) Double.parseDouble(KEEP_POWER_ZERO_INT[q])) + ","); TOTAL_SUM_POWER += COMBI_Power;
							String COMBI_EXPECTED = (((int) Double.parseDouble(KEEP_EXPECTED_ZERO_INT[q])) + ","); TOTAL_SUM_USABLE += COMBI_EXPECTED;
						}
						// Add Token("XS -> X-axis start, XE -> X-axis end, P -> Power, EX -> Expected") string
						XaxisSTART = ("XS:" + XaxisSTART);
						XaxisEND = ("XE:" + XaxisEND);
						TOTAL_SUM_POWER = ("P:" + TOTAL_SUM_POWER);
						TOTAL_SUM_USABLE = ("EX:" + TOTAL_SUM_USABLE);
						
						// Broadcast the result of query
						Sender(XaxisSTART); 
						Sender(XaxisEND); 
						Sender(TOTAL_SUM_POWER); 
						Sender(TOTAL_SUM_USABLE); 
					}

				}
			} catch (IOException e) {log("Receiver closed" + e.getMessage());} catch (SQLException e) {e.printStackTrace();
			} catch (ParseException e1) {/*TODO Auto-generated catch block*/ e1.printStackTrace();}
		}

		void Sender(String msg) 
		{
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mOutputStream, Charset.forName(StandardCharsets.UTF_8.name()))));
			printWriter.write(msg + "\n");
			printWriter.flush();
			log("Me : " + msg);
		} // end of Sender class

	} // end of Receiver class

	private static void log(String msg) 
	{
		System.out.println("[" + (new Date()) + "] " + msg);
	}
} // end of Server class