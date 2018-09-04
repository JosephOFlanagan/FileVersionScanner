package classes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.jdbc.*;

public class extractResponseDeskData 
{
    String lNumber;
    Statement statement;
    File directory;
  
    //Class Constructor
    public extractResponseDeskData(String lNum, Statement state, File dir)
    {
        //Read Log number here
        lNumber = lNum;
        statement = state;
        directory = dir;
    }
  
    //Declare variables (PVCS Objects Array, Log Type, Attachments Array, Release Notes and all other required variables)
    String[] pvcsObjectArray;
    String[] attachmentsArray;
    File[] attachmentFiles;
    List<String> allScripts;
    String[] allScriptsArray;
    List<File> scriptFiles;
    String logType;
    ResultSet releaseNotes;
    BigDecimal[] versionNumbers;
    String[] attachmentTypes;
    int scriptNumbers = 0;
    protected boolean included = false;
    protected boolean fileCheck;
    protected boolean releaseNoteComment = true;
    ResultSet query;
    ResultSet query2;
    ResultSet query3;
    scriptArray scripts;
    Writer writer;
    String errors = "";
    protected boolean scriptsAttached = true;
    protected boolean zipFilesIncluded = false;
    protected boolean fmbIncluded = false;
    protected boolean pllIncluded = false;
    protected boolean clientSideIncluded = false;
    protected boolean attachedObjects = true;
    protected boolean wbpIncluded = false;
    protected boolean extIncluded = false;
    protected List<String> extScripts;
  
    //Create a protected instance of ResponseDeskData
    protected responseDeskData rDData;
    
    public void extractData() throws SQLException, IOException
    {
        System.out.println("Extracting Data from Response Desk");
        //The following code identifies Faults and Change Requests  
        
        query = statement.executeQuery("select log_log_type_id from log where LOG_ID = " + lNumber);

        while (query.next())
        {
            //Check the log type, it will be numerical depending on whether it's a Fault or a Change Request
            int queryresult = query.getInt("log_log_type_id");
            if (queryresult == 1)
            {
                logType = "Fault";
            }
            else if (queryresult == 2)
            {
                logType = "Change Request";
            }
            else
            {
                //More log types exist than those two in Response Desk, if the log is something else then set log Type to blank, we will deal with this later
                logType = "";
            }
        }

        //Check if the release note comment exists
        releaseNotes = statement.executeQuery("select ltr_log_id from log_transaction where LTR_TITLE = 'COMMENT: RELEASE NOTES' and ltr_log_id = " + lNumber); /*Get the value from Response Desk*/

        if (!releaseNotes.next())
        {
            //The release note is clearly missing, so print out such a message
            errors = errors + "ERROR: RELEASE NOTE COMMENT is either missing, incorrectly named or in the wrong location in ResponseDesk. RELEASE NOTE COMMENT is expected for all logs." + System.lineSeparator();
            System.out.println("ERROR: RELEASE NOTE COMMENT is either missing, incorrectly named or in the wrong location in ResponseDesk. RELEASE NOTE COMMENT is expected for all logs.");
            releaseNoteComment = false;
        }
        int noOfObjects = 0;
        try
        {
            //Fills the array with the names of objects and their expected Response Desk Revision.
            ResultSet countObjects = statement.executeQuery("select count(*) from modules_amended ma where log_id = " + 
            lNumber + " and CONVERT_TO_LEADING_ZEROS(CHECKED_OUT_REVISION) = (select max(CONVERT_TO_LEADING_ZEROS(ma2.checked_out_revision)) from modules_amended ma2 where ma2.log_id = ma.log_id and ma2.object_name= ma.object_name)" +
            " and CONVERT_TO_LEADING_ZEROS(CHECKED_IN_REVISION) = (select max(CONVERT_TO_LEADING_ZEROS(ma2.checked_in_revision)) from modules_amended ma2 where ma2.log_id = ma.log_id and ma2.object_name= ma.object_name) and OBJECT_NAME NOT LIKE '%htv%'");
            while (countObjects.next()) 
            {
                    noOfObjects = countObjects.getInt("COUNT(*)");
            }
            countObjects.close();
        }
        catch (SQLException e) 
        {
            //Print out message if the files cannot be found in Response Desk
            System.out.println("ERROR: Could not find files in Response Desk");
            attachedObjects = false;
        }

        try
        {
            //We need to get the actual Response Desk data for use in the program
            query2 = statement.executeQuery("select object_name, ma.CHECKED_OUT_REVISION, ma.CHECKED_IN_REVISION from modules_amended ma where log_id = " +
            lNumber + " and CONVERT_TO_LEADING_ZEROS(CHECKED_OUT_REVISION) = (select max(CONVERT_TO_LEADING_ZEROS(ma2.checked_out_revision)) from modules_amended ma2 where ma2.log_id = ma.log_id and ma2.object_name= ma.object_name)" +
            " and CONVERT_TO_LEADING_ZEROS(CHECKED_IN_REVISION) = (select max(CONVERT_TO_LEADING_ZEROS(ma2.checked_in_revision)) from modules_amended ma2 where ma2.log_id = ma.log_id and ma2.object_name= ma.object_name) and OBJECT_NAME NOT LIKE '%htv%'");
        }
        catch (SQLException e) 
        {
            System.out.println("ERROR: Could not find files in Response Desk");
            attachedObjects = false;
        }
        //If we know for sure that this log has objects attached to it, then we run this section
        if (attachedObjects == true) 
        {
            //initialise arrays based on how many objects there are attached to the log
            pvcsObjectArray = new String[noOfObjects];
            versionNumbers = new BigDecimal[noOfObjects];

            while (query2.next())
            {
                int a = query2.getRow();
                int i = a - 1;
                pvcsObjectArray[i] = query2.getString("OBJECT_NAME");
                //Prioritise any objects that have a Checked in Revision just so that the patching team can run this as well as the developers
                if (query2.getBigDecimal("CHECKED_IN_REVISION") != null)
                {
                    versionNumbers[i] = query2.getBigDecimal("CHECKED_IN_REVISION");
                }
                else 
                {
                    //Unchecked in versions will normally be the next one in the sequence, method needs to calculate this.
                    BigDecimal versionCalculator = query2.getBigDecimal("CHECKED_OUT_REVISION");
                    Double d = versionCalculator.doubleValue();
                    String[] splitter = versionCalculator.toString().split("\\.");
                    int x = splitter[1].length();
                    double q = Math.pow(10, x);
                    double y = d*q;
                    Double yd = new Double(y);
                    String yString = yd.toString();
                    Pattern yPattern = Pattern.compile("7[9]+.0");
                    Matcher yMatch  = yPattern.matcher(yString);
                    String yEnd = yString.substring(yString.length() - 3);
                    //Version Number needs to go from 7.9 to 7.10, we need to calculate this as this is now normally how the code will work
                    if ((yEnd).equals("9.0")) 
                    {
                        double ydr = y - ((q/10)*9);
                        int b = x;
                        //The method will be differen for 7.9 and 7.19 and so forth so different calculations need to be made
                        if (yMatch.matches())
                        {
                            ydr = ((y + 1) - q) + (q/10);
                            ydr = ydr/q;
                            b = x + 1;
                        }
                        else
                        {
                            ydr = ydr + 1;
                            ydr = ydr/q;
                        }
                        //We should have the correct value now but we need to make sure it has the correct amount of 0s as the end.
                        yd = ydr;
                        yString = yd.toString();
                        BigDecimal yRound = new BigDecimal(yString);
                        yRound = yRound.setScale(x+1);
                        versionNumbers[i] = yRound;
                    }
                    else
                    {
                        //The equation here is simple as we are just adding one onto the version number
                        y +=1;
                        double z = y/q;
                        Double u = new Double(z);
                        String strNumber = u.toString();
                        BigDecimal bdReturn = new BigDecimal(strNumber);
                        versionNumbers[i] = bdReturn;
                    }
                }
            }            
        }
        else 
        {
            //For future reference these need to be set anyway, but if we know there are no objects on this log then they can be set to null
            pvcsObjectArray = new String[1];
            versionNumbers = new BigDecimal[1];
            pvcsObjectArray[0] = "";
            versionNumbers[0] = null;
        }
        //In the next section we will try and check for this log's attachments and find the scripts
        int noOfAttachments = 0;
        
        try
        {
            //Try and find out how many attachments there are
            ResultSet countAttachments = statement.executeQuery("select count(*) from ATTACHEDFILES where LOG_ID = " + lNumber);
            while (countAttachments.next())
            {
                noOfAttachments = countAttachments.getInt("COUNT(*)");
            }
            countAttachments.close();
        }
        catch (SQLException e) 
        {
            //Should this query fail, print out an error message
            System.out.println("Error when counting attachments");
        }
        //Now to call the actual data
        query3 = statement.executeQuery("select attachment_name, attachment_type, schema from ATTACHEDFILES where LOG_ID = " + lNumber);

        //Fills the attachments Array
        
        attachmentsArray = new String[noOfAttachments];
        attachmentFiles = new File[noOfAttachments];
        //Attachment types need to be stored for future reference
        attachmentTypes = new String[noOfAttachments];
        //Due to the functinality of Java, the following variables need to be declared as lists rather than variables
        allScripts = new ArrayList<String>();
        scriptFiles = new ArrayList<File>();
        while (query3.next())
        {
            //Gets the row of the data and adapts it for array use
            int a = query3.getRow();
            int i = a - 1;
            //Assign the attachments
            attachmentsArray[i] = query3.getString("ATTACHMENT_NAME");
            //If Attachment is a script
            if (("S").equals(query3.getString("ATTACHMENT_TYPE")) )
            {
                //set attachment types to script and add the attachment to a script array
                attachmentTypes[i] = "script";
                scriptNumbers++;
            }
            else
            {
                //Leave attachments blank
                attachmentTypes[i] = "";
            }
            //Checks if the attachment is a script
            if (("script").equals (attachmentTypes[i]))
            {
                if (("EXT").equals(query3.getString("SCHEMA")))
                {
                    extScripts.add(attachmentsArray[i]);
                    extIncluded = true;
                }
                allScripts.add(attachmentsArray[i]);
            }
        }
        try
        {
            //We need to work with arrays, not lists
            allScriptsArray = allScripts.toArray(new String[allScripts.size()]);
        }
        catch (Exception e)
        {
            //if there are no scripts attached then the following varaible should be set as false so that the program doesn't check scripts later on
            scriptsAttached = false;
        }

        //Response Desk files have their own file suffixes, so they need to be converted.
        if (attachedObjects == true)
        {
            for (int i = 0; i < pvcsObjectArray.length; i++)
            {
                //$filenameorig = $filenameorig -replace(".spv",".spl")
                if (("spv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "spl";
                }
                //$filenameorig = $filenameorig -replace(".usv",".usp")
                else if (("usv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "usp";
                }
                //$filenameorig = $filenameorig -replace(".ubv",".ubd")
                else if (("ubv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "ubd";
                }
                //$filenameorig = $filenameorig -replace(".eav",".ear")
                else if (("eav").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    //This clears the way to run the Zip File class
                    zipFilesIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "ear";
                }
                //$filenameorig = $filenameorig -replace(".xbv",".xbd")
                else if (("xbv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    extIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "xbd";
                }
                //$filenameorig = $filenameorig -replace(".xsv",".xsp")
                else if (("xsv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    extIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "xsp";
                }
                //$filenameorig = $filenameorig -replace(".wtv",".wtp")
                else if (("wtv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    wbpIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "wtp";
                }
                //$filenameorig = $filenameorig -replace(".wbv",".wbd")
                else if (("wbv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    wbpIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "wbd";
                }
                //$filenameorig = $filenameorig -replace(".wsv",".wsp")
                else if (("wsv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    wbpIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "wsp";
                }
                //$filenameorig = $filenameorig -replace(".otv",".ott")
                else if (("otv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "ott";
                }
                //$filenameorig = $filenameorig -replace(".mmv",".mmx")
                else if (("mmv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "mmx";
                }
                //$filenameorig = $filenameorig -replace(".tyv",".typ")
                else if (("tyv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "typ";
                }
                //$filenameorig = $filenameorig -replace(".ziv",".zip")
                else if (("ziv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    //This clears the way to run the Zip File class
                    zipFilesIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "zip";
                }
                //$filenameorig = $filenameorig -replace(".exv",".exe")
                else if (("exv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3) == "exv"))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring((pvcsObjectArray[i].length() - 3)) + "exe";
                }
                //$filenameorig = $filenameorig -replace(".psv",".psp")
                else if (("psv").equals (pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "psp";
                }
                //$filenameorig = $filenameorig -replace(".ldv",".ldr")
                else if (("ldv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "ldr";
                }
                //$filenameorig = $filenameorig -replace(".trv",".trg")
                else if (("trv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "trg";
                }   
                //$filenameorig = $filenameorig -replace(".fmv",".fmb")
                else if (("fmv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    //Client side included
                    clientSideIncluded = true;
                    //Fmbs are included, so make the variable true
                    fmbIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "fmb";
                }
                //$filenameorig = $filenameorig -replace(".plv",".pll")
                else if (("plv").equals (pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    //Client side included
                    clientSideIncluded = true;
                    //PLLs are included, so make the variable true
                    pllIncluded = true;
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "pll";
                }
                //$filenameorig = $filenameorig -replace(".pbv",".pbd")
                else if (("pbv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0, pvcsObjectArray[i].length() - 3) + "pbd";
                }
                //$filenameorig = $filenameorig -replace(".rdv",".rep")
                else if (("rdv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0, pvcsObjectArray[i].length() - 3) + "rep";
                }
                //$filenameorig = $filenameorig -replace(".prv",".prc")
                else if (("prv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0, pvcsObjectArray[i].length() - 3) + "prc";
                }
                //$filenameorig = $filenameorig -replace(".xtv",".xtp")
                else if (("xtv").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,(pvcsObjectArray[i].length() - 3)) + "xtp";
                }
                //$filenameorig = $filenameorig -replace(".jav",".jar")
                else if (("jav").equals(pvcsObjectArray[i].substring(pvcsObjectArray[i].length() - 3)))
                {
                    pvcsObjectArray[i] = pvcsObjectArray[i].substring(0,pvcsObjectArray[i].length() - 3) + "jar";
                }
            }    
        }
        //Store all the values in a public instance of the Response Desk data class
        rDData = new responseDeskData(pvcsObjectArray, attachmentsArray, logType, attachmentFiles, attachmentTypes);
        //The log now needs to be checked for FLS or IM Documents depending on it's type.
        flsImCheck release = new flsImCheck(lNumber, logType, statement, directory);
        included = release.releaseChecker();
 	//Check if all files exist
        fileCheck fCheck = new fileCheck(rDData, directory, clientSideIncluded);
        System.out.println("Checking if all Files Exist");
        String filesInc = fCheck.fileChecker();
        if (!(filesInc).equals("")) 
        {
            System.out.println("The following files could not be found:" + System.lineSeparator() + filesInc);
            fileCheck = false;
        }
        else 
        {
            fileCheck = true;   
        }
        //The file checker above will populate the attachmentFiles array, so they need to be put back in line with each other.
        rDData.attachmentFiles = fCheck.attachmentFiles;

        //If Scripts are attadched we need to assign them to a Script Array Class
        if (scriptsAttached == true)
        {
            for (int i = 0; i < allScriptsArray.length; i++)
            {
                //Run through the directory and check that the name matches what is found in the directory, add that file to our class
                for (int j = 0; j < fCheck.dir_contents.length; j++)
                {
                    String scriptName = allScriptsArray[i].toString();
                    if ((scriptName).equals(fCheck.dir_contents[j].getName()))
                    {
                        scriptFiles.add(fCheck.dir_contents[j]);
                    }
                }
            }
            //Declare the script files array variable
            File[] scriptFilesArray = scriptFiles.toArray(new File[scriptFiles.size()]);
            //Since the list has been compiled, we can now store it for future use
            scripts = new scriptArray(allScriptsArray, scriptFilesArray);
        }
        //Write release note errors out to text file
        try 
        {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/" + "releasenoteerrors.txt"), "utf-8"));
            writer.write(errors);
        } 
        catch (IOException ex) 
        {
            // Report
            System.out.println("Could not write to releasenoteerrors file.");
        } 
        finally 
        {
           try {writer.close();} catch (Exception ex) {/*ignore*/}
        }
    }
}