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

//This is a program designed to run Test Preperation for the Arval Equus Software Factory. This program is to be used by developers to check whether the contents of their log meet the standards required to patch.
//This is the master class. All other classes will be run through this class.
//Version 1.0 Initialised Test Prep Process
//Version 1.1 Refined forms conversion to work for all forms
//Version 1.2 Refined version checking to include unchecked-in objects
//Version 1.3 Changed database connection methodology. Added Test Prep database connection
public class testPrep
{
    public testPrep()
    {
        super();
    }
    
    //Database connection for REsponse Desk database
    public static final String DBUSER = "rdreadonly";
    public static final String DBPASS = "RDREADONLY";
    protected scriptArray scripts; 
    protected static BufferedWriter writer;
  
    public static void main (String args[]) throws SQLException, IOException, InterruptedException
    {
        //Print out the program header
        System.out.println("Arval Equus Software Factory Test Preperation Process");
        System.out.println("-----------------------------------------------------");
        System.out.println("Version 1.0 Initialised Test Prep Process");
        System.out.println("Version 1.1 Refined forms conversion to work for all forms");
        System.out.println("Version 1.2 Refined version checking to include unchecked-in objects");
        System.out.println("Version 1.3 Changed database connection methodology. Added Test Prep database connection");
        System.out.println("Configuring values");
        //configure directory
        Config config = new Config();
        config.getPropValues();
        scriptArray script;
  	//Declare variables
  	boolean writeToTestPrep = false;
        //Check to see if there's a slash at the end of the directory variable, add one if there isn't
  	if (!("/").equals(config.directory.substring(config.directory.length() - 1)))
  	{
  	    config.directory = config.directory + "/";
  	}
        String logNumber = args[0].toString();
        String logType = "";
        File directory = new File(config.directory + logNumber);
        boolean testPrepClear = true;
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
                testPrepClear = false;
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
        boolean driveTSPDelete = new File(directory + "/" + "drivetsp.sql").delete();
        boolean extTSPDelete = new File(directory + "/" + "exttsp.sql").delete();
        boolean flsImErrorsDelete = new File(directory + "/" + "flsimerrors.txt").delete();
        boolean missingFilesDelete = new File(directory + "/" + "missingfiles.txt").delete();
        boolean releaseNoteErrorsDelete = new File(directory + "/" + "releasenoteerrors.txt").delete();
        boolean scriptErrorsDelete = new File(directory + "/" + "scripterrors.txt").delete();
        boolean versionErrorsDelete = new File(directory + "/" + "versionerrors.txt").delete();
        boolean wbpTSPDelete = new File(directory + "/" + "wbptsp.sql").delete();
        boolean zipVersionErrorsDelete = new File(directory + "/" + "zipearfileserrors.txt").delete();
        if (codingErrorsDelete || driveTSPDelete || extTSPDelete || flsImErrorsDelete || missingFilesDelete || releaseNoteErrorsDelete || scriptErrorsDelete || versionErrorsDelete || wbpTSPDelete || zipVersionErrorsDelete) 
        {
            System.out.println("Previous Test Prep result files deleted");
            //This if method is just here to ensure the files get deleted.
        }
        
        System.out.println("Attempting to connect to Response Desk Database");
        //For the next part we need to connect to the database
        try
        {
            //String [] data = config.getConnected("ARDMAI");
            System.setProperty("oracle.net.tns_admin","C:\\DevSuiteHome_1\\NETWORK\\ADMIN\\");
            //String DBURL = "jdbc:oracle:thin:" + DBUSER + "/" + DBPASS + "@//" + data[0] + ":" + data[1];
            DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:" + DBUSER + "/" + DBPASS + "@ARDMAI");
            //Data needs to be Scroll Insensitive in order to work in the way we want it to later in the program
            Statement statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
        
            //Run Extract Response Desk Data
            extractResponseDeskData extract = new extractResponseDeskData(logNumber, statement, directory);
            extract.extractData();
            if (extract.releaseNoteComment == false)
            {
                testPrepClear = false;
            }                
            //Check to see if all the files are included
            if (extract.included == false || extract.fileCheck == false)
            {
                testPrepClear = false;
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
                    testPrepClear = false;
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
                    testPrepClear = false;
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
                    testPrepClear = false;
                }
            }
            //need a way of checking whether the deployment worked
            boolean deploymentSuccess = true;
            //This part should only be run if the user is a member of the Arval Equus Patching Team
            if (config.DBTest != null)
            {
                if ((config.DBTest).equals("true"))
                {
                    writeToTestPrep = true;
                }
            }
            if (writeToTestPrep == true)
            {
                File tp_scriptsFolder = new File(config.directory + "\\tp_scripts");
                System.out.println("Running log against Test Prep Database");
                //Create an output file which will later be run against the database
            
                String testPrepDeployment = "";
                
                File [] dir_contents = directory.listFiles();
                testPrepDeployment = "SPOOL " + directory + "\\drivetsp.lst" + System.lineSeparator();
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals("sql"))
                    {
                        boolean extChecker = false;
                        if (extract.extIncluded == true)
                        {
                            String [] extScripts = extract.extScripts.toArray(new String[extract.extScripts.size()]);
                            for (int j = 0; j < extScripts.length; j++) 
                            {
                                if ((dir_contents[i].getName()).equals(extScripts[j])) 
                                {
                                    extChecker = true;
                                }
                            }
                        }
                        int tspChecker = dir_contents[i].getName().indexOf("tsp");
                        int buildChecker = dir_contents[i].getName().indexOf("build");
                        int utChecker = dir_contents[i].getName().indexOf("ut");
                        if (extChecker == false & tspChecker == -1 & buildChecker == -1 & utChecker == -1)
                        {
                            testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                        }
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("ott"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("typ"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("fnc"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("prc"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("psp"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("pbd"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("dpl"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("trg"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 2)).equals ("vw"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 2)).equals ("fix"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                for (int i = 0; i < dir_contents.length; i++)
                {
                    if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 2)).equals ("fix"))
                    {
                        testPrepDeployment = testPrepDeployment + "@" + dir_contents[i] + System.lineSeparator();
                    }
                }
                testPrepDeployment = testPrepDeployment + "@" + tp_scriptsFolder + "\\drvcomp" + System.lineSeparator() + "@" + tp_scriptsFolder + "\\drvcomp" + System.lineSeparator();
                testPrepDeployment = testPrepDeployment + "SPOOL OFF" + System.lineSeparator();
                testPrepDeployment = testPrepDeployment + "exit" + System.lineSeparator();
                
                try 
                {
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/drivetsp.sql"), "utf-8"));
                    writer.write(testPrepDeployment);
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
                String TPUSER = "drivetsp";
                String TPPASS = "drivetsp";
                
                String command = "sqlplus -L " + TPUSER +"/"+ TPPASS + "@DRG1DEV " + "@" + directory + "\\drivetsp.sql";
                
                Process process = Runtime.getRuntime().exec(command);
                
                process.waitFor(); 
                
                File lstFile = new File(directory + "\\drivetsp.lst");
                BufferedReader lstReader = new BufferedReader(new FileReader(lstFile));
                String line = "";
                String errors = "";
                
                while ((line = lstReader.readLine()) != null)
                {
                    line = line.toLowerCase(Locale.US);
                    
                    int errorFound = line.indexOf("ora-");
                    if (errorFound > -1)
                    {
                        testPrepClear = false;
                        System.out.println(line);
                        errors = errors + line + System.lineSeparator();
                    }
                    else 
                    {
                        errorFound = line.indexOf("sp2");
                        if (errorFound > -1)
                        {
                            testPrepClear = false;
                            System.out.println(line);
                            errors = errors + line + System.lineSeparator();
                        }
                        else 
                        {
                            errorFound = line.indexOf("warn");
                            if (errorFound > -1)
                            {
                                testPrepClear = false;
                                System.out.println(line);
                                errors = errors + line + System.lineSeparator();
                            }
                            else 
                            {
                                errorFound = line.indexOf("unable");
                                if (errorFound > -1)
                                {
                                    testPrepClear = false;
                                    System.out.println(line);
                                    errors = errors + line + System.lineSeparator();
                                }
                            }
                        }
                    }
                }
                if (extract.wbpIncluded == true)
                {
                    String testPrepDeploymentWBP = "";
                    testPrepDeploymentWBP = testPrepDeploymentWBP + "set define off" + System.lineSeparator();
                    testPrepDeploymentWBP = testPrepDeploymentWBP + "SPOOL " + directory + "\\wbptsp.lst" + System.lineSeparator();
                    for (int i = 0; i < dir_contents.length; i++)
                    {
                        if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("wtp"))
                        {
                            testPrepDeploymentWBP = testPrepDeploymentWBP + "@" + dir_contents[i] + System.lineSeparator();
                        }
                    }
                    for (int i = 0; i < dir_contents.length; i++)
                    {
                        if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("wpr"))
                        {
                            testPrepDeploymentWBP = testPrepDeploymentWBP + "@" + dir_contents[i] + System.lineSeparator();
                        }
                    }
                    for (int i = 0; i < dir_contents.length; i++)
                    {
                        if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("wsp"))
                        {
                            testPrepDeploymentWBP = testPrepDeploymentWBP + "@" + dir_contents[i] + System.lineSeparator();
                        }
                    }
                    for (int i = 0; i < dir_contents.length; i++)
                    {
                        if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("wbd"))
                        {
                            testPrepDeploymentWBP = testPrepDeploymentWBP + "@" + dir_contents[i] + System.lineSeparator();
                        }
                    }
                    testPrepDeploymentWBP = testPrepDeploymentWBP + "@" + tp_scriptsFolder + "\\drvcomp" + System.lineSeparator() + "@" + tp_scriptsFolder + "\\drvcomp" + System.lineSeparator();
                    testPrepDeploymentWBP = testPrepDeploymentWBP + "SPOOL OFF" + System.lineSeparator();
                    testPrepDeploymentWBP = testPrepDeploymentWBP + "exit" + System.lineSeparator();
                    try 
                    {
                        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/wbptsp.sql"), "utf-8"));
                        writer.write(testPrepDeploymentWBP);
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
                    
                    TPUSER = "wbptsp";
                    TPPASS = "wbptsp";
                    
                    command = "sqlplus -L " + TPUSER +"/"+ TPPASS + "@DRG1DEV " + "@" + directory + "\\wbptsp.sql";
                    
                    Process processWBP = Runtime.getRuntime().exec(command);
                    
                    processWBP.waitFor(); 
                    
                    File lstFileWBP = new File(directory + "\\wbptsp.lst");
                    BufferedReader wbpLstReader = new BufferedReader(new FileReader(lstFileWBP));
                    String wbpLine = "";
                    
                    while ((wbpLine = wbpLstReader.readLine()) != null) 
                    {
                        wbpLine = wbpLine.toLowerCase(Locale.US);
                        
                        int errorFound = wbpLine.indexOf("ora-");
                        if (errorFound > -1)
                        {
                            testPrepClear = false;
                            System.out.println(wbpLine);
                            errors = errors + wbpLine + System.lineSeparator();
                        }
                        else 
                        {
                            errorFound = wbpLine.indexOf("sp2");
                            if (errorFound > -1)
                            {
                                testPrepClear = false;
                                System.out.println(wbpLine);
                                errors = errors + wbpLine + System.lineSeparator();
                            }
                            else 
                            {
                                errorFound = wbpLine.indexOf("warn");
                                if (errorFound > -1)
                                {
                                    testPrepClear = false;
                                    System.out.println(wbpLine);
                                    errors = errors + wbpLine + System.lineSeparator();
                                }
                                else 
                                {
                                    errorFound = wbpLine.indexOf("unable");
                                    if (errorFound > -1)
                                    {
                                        testPrepClear = false;
                                        System.out.println(wbpLine);
                                        errors = errors + wbpLine + System.lineSeparator();
                                    }
                                }
                            }
                        }
                    }
                }
                if (extract.extIncluded == true)
                {
                    String testPrepDeploymentEXT = "";
                    testPrepDeploymentEXT = testPrepDeploymentEXT + "set define off";
                    testPrepDeploymentEXT = testPrepDeploymentEXT + "SPOOL " + directory + "\\exttsp.lst" + System.lineSeparator();
                    String [] extScripts = extract.extScripts.toArray(new String[extract.extScripts.size()]);
                    for (int i = 0; i < dir_contents.length; i++)
                    {   
                        if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("sql"))
                        {
                            boolean extChecker = false;
                            for (int j = 0; j < extScripts.length; j++) 
                            {
                                if ((dir_contents[i].getName()).equals(extScripts[j])) 
                                {
                                    extChecker = true;
                                }
                            }
                            if (extChecker == true)
                            {
                                testPrepDeploymentEXT = testPrepDeploymentEXT + "@" + dir_contents[i] + System.lineSeparator();
                            }
                        }
                    }
                    for (int i = 0; i < dir_contents.length; i++)
                    {   
                        if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("xsp"))
                        {
                            testPrepDeploymentEXT = testPrepDeploymentEXT + "@" + dir_contents[i] + System.lineSeparator();
                        }
                        }                    
                    for (int i = 0; i < dir_contents.length; i++)
                    {   
                        if ((dir_contents[i].getName().substring(dir_contents[i].getName().length() - 3)).equals ("xbd"))
                        {
                            testPrepDeploymentEXT = testPrepDeploymentEXT + "@" + dir_contents[i] + System.lineSeparator();
                        }
                    }
                    testPrepDeploymentEXT = testPrepDeploymentEXT + "@" + tp_scriptsFolder + "\\drvcomp" + System.lineSeparator() + "@" + tp_scriptsFolder + "\\drvcomp" + System.lineSeparator();
                    testPrepDeploymentEXT = testPrepDeploymentEXT + "SPOOL OFF" + System.lineSeparator();
                    testPrepDeploymentEXT = testPrepDeploymentEXT + "exit" + System.lineSeparator();
                    try 
                    {
                        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/exttsp.sql"), "utf-8"));
                        writer.write(testPrepDeploymentEXT);
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
                    
                    TPUSER = "exttsp";
                    TPPASS = "exttsp";
                    
                    command = "sqlplus -L " + TPUSER +"/"+ TPPASS + "@DRG1DEV " + "@" + directory + "\\exttsp.sql";
                    
                    Process processEXT = Runtime.getRuntime().exec(command);
                    
                    processEXT.waitFor(); 
                    
                    File lstFileEXT = new File(directory + "\\exttsp.lst");
                    BufferedReader extLstReader = new BufferedReader(new FileReader(lstFileEXT));
                    String extLine = "";
                    
                    while ((extLine = extLstReader.readLine()) != null) 
                    {
                        extLine = extLine.toLowerCase(Locale.US);
                        
                        int errorFound = line.indexOf("ora-");
                        if (errorFound > -1)
                        {
                            testPrepClear = false;
                            System.out.println(extLine);
                            errors = errors + extLine + System.lineSeparator();
                        }
                        else 
                        {
                            errorFound = extLine.indexOf("sp2");
                            if (errorFound > -1)
                            {
                                testPrepClear = false;
                                System.out.println(extLine);
                                errors = errors + extLine + System.lineSeparator();
                            }
                            else 
                            {
                                errorFound = extLine.indexOf("warn");
                                if (errorFound > -1)
                                {
                                    testPrepClear = false;
                                    System.out.println(extLine);
                                    errors = errors + extLine + System.lineSeparator();
                                }
                                else 
                                {
                                    errorFound = extLine.indexOf("unable");
                                    if (errorFound > -1)
                                    {
                                        testPrepClear = false;
                                        System.out.println(extLine);
                                        errors = errors + extLine + System.lineSeparator();
                                    }

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
            if (testPrepClear == true)
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