package classes;
//import readers to allow java to read text and File to read files
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import oracle.jdbc.*;
import java.nio.file.*;

import java.util.Locale;

//This program is to be used by developers to check whether the contents of their log meet standards.
//This is the master class. All other classes will be run through this class.
//Version 1.0 Initialised Test Scan Process
//Version 1.1 Refined forms conversion to work for all forms
//Version 1.2 Refined version checking to include unchecked-in objects
//Version 1.3 Changed database connection methodology. Added database connection
public class testScan
{
    public testScan()
    {
        super();
    }
    
    //Database connection for Main database
    public static final String DBUSER = "username";
    public static final String DBPASS = "password";
    protected scriptArray scripts; 
    protected static BufferedWriter writer;
  
    public static void main (String args[]) throws SQLException, IOException, InterruptedException
    {
        //Print out the program header
        System.out.println("Test Scan Process");
        System.out.println("-----------------------------------------------------");
        System.out.println("Version 1.0 Initialised Test Scan Process");
        System.out.println("Version 1.1 Refined forms conversion to work for all forms");
        System.out.println("Version 1.2 Refined version checking to include unchecked-in objects");
        System.out.println("Version 1.3 Changed database connection methodology. Added Test Prep database connection");
        System.out.println("Configuring values");
        //configure directory
        Config config = new Config();
        config.getPropValues();
        scriptArray script;
        //Declare variables
        boolean writeToTestScan = false;
        //Check to see if there's a slash at the end of the directory variable, add one if there isn't
        if (!("/").equals(config.directory.substring(config.directory.length() - 1)))
        {
            config.directory = config.directory + "/";
        }
        String logNumber = args[0].toString();
        String logType = "";
        File directory = new File(config.directory + logNumber);
        boolean testScanClear = true;
        fmbPllConvert fmbpll;
        fmbxmlPld clientSide;
        //If the config value was left blank for whatever reason, check to see if another parameter has been entered and use that directory
        if (config.directory == null)
        {
            try
            {
                directory = new File(args[1] + logNumber);
            }
            catch (NullPointerException e) 
            {
                //Display message to prompt user to enter the directory after the log number
                System.out.println("The system could not find the correct Configuration file or any directory. Please enter the required directory name after the log number");
                testScanClear = false;
            }
        }
        //Check for errors in user input
        if (args[0] == null) 
        {
            //Display error message and exit the program as the user forgot to put in a log number.
            System.out.println("ERROR: No log number specified. Program needs a log number in order to run correctly");
            System.exit(1);
        }
        //Check if directory exists
        if (!directory.exists()) 
        {
            //Display error and exit program.
            System.out.println("ERROR: Invalid directory specified in config file");
            System.exit(1);
        }
        //Delete previous error files
        boolean codingErrorsDelete = new File(directory + "/" + "codingerrors.txt").delete();
        boolean databaseDelete = new File(directory + "/" + "database.sql").delete();
        boolean flsImErrorsDelete = new File(directory + "/" + "flsimerrors.txt").delete();
        boolean missingFilesDelete = new File(directory + "/" + "missingfiles.txt").delete();
        boolean releaseNoteErrorsDelete = new File(directory + "/" + "releasenoteerrors.txt").delete();
        boolean scriptErrorsDelete = new File(directory + "/" + "scripterrors.txt").delete();
        boolean versionErrorsDelete = new File(directory + "/" + "versionerrors.txt").delete();
        boolean zipVersionErrorsDelete = new File(directory + "/" + "zipearfileserrors.txt").delete();
        if (codingErrorsDelete || databaseDelete || flsImErrorsDelete || missingFilesDelete || releaseNoteErrorsDelete || scriptErrorsDelete || versionErrorsDelete || zipVersionErrorsDelete) 
        {
            System.out.println("Previous Test Scan result files deleted");
            //This if method is just here to ensure the files get deleted.
        }
        
        System.out.println("Attempting to connect to Database");
        //For the next part we need to connect to the database
        try
        {
            System.setProperty("oracle.net.tns_admin","DIRECTORY");
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:" + DBUSER + "/" + DBPASS + "@database");
            //Data needs to be Scroll Insensitive in order to work in the way we want it to later in the program
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
        
            //Run Extract Response Desk Data
            extractData extract = new extractData(logNumber, statement, directory);
            extract.extractData();
            if (extract.releaseNoteComment == false)
            {
                testScanClear = false;
            }                
            //Check to see if all the files are included
            if (extract.included == false || extract.fileCheck == false)
            {
                testScanClear = false;
            }
            //Convert FMBs and PLLs for easier reading
            if (extract.clientSideIncluded == true && extract.fileCheck == true)
            {
                //This will call external programs if there any forms or plls attached to the log
                fmbpll = new fmbPllConvert(extract.rDData.pvcsObjects, directory, config);
                clientSide = fmbpll.conversion();
            }
            else 
            {
                //We need to do this to stop the program checking forms or plls if there are none
                clientSide = null;
            }
            if (extract.attachedObjects == true && extract.fileCheck == true)
            {
                //Check Versioning for all forms and packages
                versionCheck vCheck = new versionCheck(clientSide, extract, logNumber, directory);
                boolean verCheck = vCheck.versionChecking();
                //The variable above will return true if all the logs passed test Prep, if they didn't
                if (verCheck == false)
                {
                    testScanClear = false;
                }
            }
            //Check all scripts
            if (extract.scriptsAttached == true && extract.fileCheck == true) 
            {
                //If there are scrips on this log and if they are included in the file
                checkScripts cScript = new checkScripts(extract.scripts,logNumber, directory);
                boolean chScript = cScript.scriptCheck();
                //If the scripts passed all their checks, test Prep was a success, but if one failed then test Prep needs to be marked as failed
                if (chScript == false)
                {
                    testScanClear = false;
                }
            }
    
            //Check Zip Files and Ear Files
            if (extract.zipFilesIncluded == true && extract.fileCheck == true)
            {
                //If Zip files are included we need to check them for version numbers, but due to their different nature they require an entirely different method
                zipEarFiles zef = new zipEarFiles(extract.rDData, directory, extract.versionNumbers);
                boolean zipChecking = zef.zipCheck();
                //If the version numbers were incorrect and if certain files were not extracted at the correct level the program should return false
                if (zipChecking == false) 
                {
                    testScanClear = false;
                }
            }
            //need a way of checking whether the deployment worked
            boolean deploymentSuccess = true;
            //This part should only be run if the user is a member of the Arval Equus Patching Team
            if (config.DBTest != null)
            {
                if ((config.DBTest).equals("true"))
                {
                    writeToTestScan = true;
                }
            }
            if (writeToTestScan == true)
            {
                File tp_scriptsFolder = new File(config.directory + "\\tp_scripts");
                System.out.println("Running log against Test Prep Database");
                //Create an output file which will later be run against the database
            
                String testScanDeployment = "";
                
                File [] dir_contents = directory.listFiles();
                testScanDeployment = "SPOOL " + directory + "\\database.lst" + System.lineSeparator();
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals("sql"))
                    {
                        int databaseChecker = dir_contents[i].getName().indexOf("database");
                        int buildChecker = dir_contents[i].getName().indexOf("build");
                        int utChecker = dir_contents[i].getName().indexOf("ut");
                        if (databaseChecker == -1 & buildChecker == -1 & utChecker == -1)
                        {
                            testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                        }
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("ott"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("typ"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("fnc"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("prc"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("psp"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("pbd"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("dpl"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("trg"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 2)).equals ("vw"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 2)).equals ("fix"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 2)).equals ("fix"))
                    {
                        testScanDeployment = testScanDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                testScanDeployment = testScanDeployment + "SPOOL OFF" + System.lineSeparator();
                testScanDeployment = testScanDeployment + "exit" + System.lineSeparator();
                
                try 
                {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/database.sql"), "utf-8"));
                    writer.write(testScanDeployment);
                }
                catch (IOException ex) 
                {
                    // Report
                    System.out.println("Failed to create output file");
                    deploymentSuccess = false;
                } 
                finally 
                {
                    try {writer.close();} catch (Exception ex) {/*ignore*/}
                }
                String TPUSER = "username";
                String TPPASS = "password";
                
                String command = "sqlplus -L " + TPUSER +"/"+ TPPASS + "@database " + "@" + directory + "\\database.sql";
                
                Process process = Runtime.getRuntime().exec(command);
                
                process.waitFor(); 
                
                File lstFile = new File(directory + "\\database.lst");
                BufferedReader lstReader = new BufferedReader(new FileReader(lstFile));
                String line = "";
                String errors = "";
                
                while ((line = lstReader.readLine()) != null)
                {
                    line = line.toLowerCase(Locale.US);
                    
                    int errorFound = line.indexOf("ora-");
                    if (errorFound > -1)
                    {
                        testScanClear = false;
                        System.out.println(line);
                        errors = errors + line + System.lineSeparator();
                    }
                    else 
                    {
                        errorFound = line.indexOf("sp2");
                        if (errorFound > -1)
                        {
                            testScanClear = false;
                            System.out.println(line);
                            errors = errors + line + System.lineSeparator();
                        }
                        else 
                        {
                            errorFound = line.indexOf("warn");
                            if (errorFound > -1)
                            {
                                testScanClear = false;
                                System.out.println(line);
                                errors = errors + line + System.lineSeparator();
                            }
                            else 
                            {
                                errorFound = line.indexOf("unable");
                                if (errorFound > -1)
                                {
                                    testScanClear = false;
                                    System.out.println(line);
                                    errors = errors + line + System.lineSeparator();
                                }
                            }
                        }
                    }
                }
                if ((errors).equals(""))
                {
                    System.out.println("No Errors in log coding");
                }
                try 
                {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/" + "codingerrors.txt"), "utf-8"));
                    writer.write(errors);
                }
                catch (IOException ex) 
                {
                    // Report
                    System.out.println("Could not write to codingerrors file.");
                } 
                finally 
                {
                   try {writer.close();} catch (Exception ex) {/*ignore*/}
                }
                
            }
            //A well prepared log should still be showing as true by this point. If everything passed Test Prep, then this log was a success, so print out to the user as such
            if (testScanClear == true)
            {
                System.out.println("SUCCESS");
            }
            else
            {
                //Log cannot move on to QA Prep if it is in such a condition
                System.out.println("FAILURE - Please see Error file");
            }
        }
        catch (SQLException e) 
        {
            //Oracle JDBC programs are a bit finnicky, need to allow for the possibility that the connection failed
            System.out.println("Could not connect to Response Desk database");
        }
    }
}
