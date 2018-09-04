package classes;

//import packages
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oracle.net.aso.i;

public class versionCheck 
{
    //FMXPLLText
    fmbxmlPld fpt;
    //PVCS Object Array
    String[] pvcsObjects;
    String logNumber;
    BigDecimal[] versionNumbers;
    String logType;
    File directory;
    File[] files;
    String[] fmbxmlStrings;
    String[] pldStrings;
    File[] fmbxmlFiles;
    File[] pldFiles;
    responseDeskData response;
    boolean clientSide;
    boolean fmbInc;
    boolean pllInc;
    boolean[] fmbExists;
    boolean[] pllExists;
    Writer writer;
    String errors = "";
  
    //Class Constructor
    public versionCheck(fmbxmlPld fp, extractResponseDeskData ext, String logNo, File dir)
    {
        //FMXPLLText
        fpt = fp;
  	//PVCS Object Array
        pvcsObjects = ext.rDData.pvcsObjects();
        //Log Number
        logNumber = logNo;
        //Version Number
        versionNumbers = ext.versionNumbers;
        //Directory
        directory = dir;
        //Client Side
        clientSide = ext.clientSideIncluded;
        //FMBs
        fmbInc = ext.fmbIncluded;
        //PLLs
        pllInc = ext.pllIncluded;
    }
  
    public boolean versionChecking() throws IOException
    {
        //We need to be able to reference the files to check versions
        files = directory.listFiles();
        //If there are no problems Test Prep passed should be true, so intialise the variable as such.
        boolean testPrepPassed = true;
        //Is there a client side? If so then a special process is needed to check version numbers
        if (clientSide == true)
        {
            //Need to check specifically if FMB files were in this log
            boolean fmbxmlFilesExist = false;
            if (fmbInc == true)
            {
                //Populate the arrays with data
                fmbxmlStrings = new String[fpt.fmbxmlArray.size()];
                fmbxmlFiles = new File[fpt.fmbxmlArray.size()];
                fmbExists = new boolean[fpt.fmbxmlArray.size()];
                fmbxmlStrings = fpt.fmbxmlArray.toArray(fmbxmlStrings);
                //For all FMBs
                for (int i = 0; i < fmbxmlStrings.length; i++) 
                {
                    //Before we continue any further it is important that the data we are running doesn't get mixed up, so we need to align it with the directory.
                    int fmbVsXmlCheck = 0;
                    File[] dir_contents = directory.listFiles();
                    for (int j = 0; j < dir_contents.length; j++) 
                    {
                        //Double check just to make sure that the matching fmb file is included
                        if ((fmbxmlStrings[i].substring(0,fmbxmlStrings[i].length() - 8) + ".fmb").equals(dir_contents[j].getName()))
                        {
                            fmbVsXmlCheck++;
                        }
                        if ((fmbxmlStrings[i]).equals(dir_contents[j].getName()))
                        {
                            //We have a match so lets add it to the array
                            fmbxmlFiles[i] = dir_contents[j];
                            fmbVsXmlCheck++;
                        }
                    }
                    //The two checks have been run to ensure that both the fmb and the xml file exist, so let's set the Exists varaible to equal true
                    if (fmbVsXmlCheck >= 2)
                    {
                        fmbxmlFilesExist = true;
                    }
                    if (fmbVsXmlCheck == 1) 
                    {
                        //We're missing a file otherwise so report false.
                        testPrepPassed = false;
                        errors = errors + "ERROR: " + fmbxmlFiles[i].getName() + " could not be found inside subfolder." + System.lineSeparator();
                        System.out.println("ERROR: " + fmbxmlFiles[i].getName() + " could not be found inside subfolder.");
                    }
                }
            }
            //Now let's do the same with Plds
            boolean pldFilesExist = false;
            //Only run the following section if plls are included on the log
            if (pllInc == true)
            {            
                //Populate the arrays with data
                pldStrings = new String[fpt.pldArray.size()];
                pldFiles = new File[fpt.pldArray.size()];
                pllExists = new boolean[fpt.pldArray.size()];
                pldStrings = fpt.pldArray.toArray(pldStrings);
                //For all PLDs
                for (int i = 0; i < pldStrings.length; i++) 
                {
                    //Before we continue any further it is important that the data we are running doesn't get mixed up, so we need to align it with the directory.
                    int pllVsPldCheck = 0;
                    File[] dir_contents = directory.listFiles();
                    for (int j = 0; j < dir_contents.length; j++) 
                    {
                        //Double check just to make sure that the matching pll file is included
                        if ((pldStrings[i].substring(0, pldStrings[i].length() - 3) + "pll").equals(dir_contents[j].getName()))
                        {
                            pllVsPldCheck++;
                        }
                        if ((pldStrings[i]).equals(dir_contents[j].getName()))
                        {
                            //We have a match so lets add it to the array
                            pldFiles[i] = dir_contents[j];
                            pllVsPldCheck++;
                        }
                    }
                    //The two checks have been run to ensure that both the pll and the pld file exist, so let's set the Exists varaible to equal true
                    if (pllVsPldCheck >= 2)
                    {
                        pldFilesExist = true;
                    }
                    if (pllVsPldCheck == 1) 
                    {
                        //We're missing a file otherwise so report false.
                        testPrepPassed = false;
                        errors = errors + "ERROR: " + pldFiles[i].getName() + " could not be found inside subfolder." + System.lineSeparator();
                        System.out.println("ERROR: " + pldFiles[i].getName() + " could not be found inside subfolder.");
                    }
                }
            }
            //Now that we have our arrays it's time to check for their version numbers
            if (fmbxmlFilesExist == true)
            {
                System.out.println("Checking form versions");
                //For every FMBXML
                for (int i = 0; i < fmbxmlFiles.length; i++)
                {
                    //Need to make sure that specific file exists.
                    if (fmbxmlFiles[i] != null)
                    {
                        //Declare a variable that will be used to store the version number (you'll see why in a moment).
                        BigDecimal versionFmbXml = null;
                        //Go through all the pvcsObjects in order to align the correct version number
                        for (int j = 0; j < pvcsObjects.length; j++) 
                        {
                            //If we find a matching xml and fmb
                            if ((fmbxmlFiles[i].getName()).equals(pvcsObjects[j].substring(0,pvcsObjects[j].length() - 4) + "_fmb.xml")) 
                            {
                                versionFmbXml = versionNumbers[j];
                            }
                        }
                        String fileName = fmbxmlFiles[i].getName();
                        //The following lines of code will read the file for errors
                        BufferedReader bReader = new BufferedReader(new FileReader(fmbxmlFiles[i]));
                        int lineCount = 0;
                        String line;
                        boolean revCommentFound = false;
                        boolean revVariableFound = false;
                        boolean logNumberFound = false;
                        boolean revisionFound = false;
                        while ((line = bReader.readLine()) != null)
                        {
                            //declare variable to only be used here
                            lineCount++;
                            int xmlCheck1 = -1;
                            int xmlCheck1a = -1;
                            int xmlCheck2 = -1;
                            int xmlCheck2a = -1;
                            int xmlCheck3 = -1;
                            int xmlCheck3a = -1;
                            //Check module.formmodule.programunit for revision variable, the version number in a comment and the log number.
                            Pattern pCheck1 = Pattern.compile("revision\\s*\\w*\\s*varchar2\\(\\d*\\)\\s*:=\\s*\\w*\\s*'" + versionFmbXml.toString(), Pattern.CASE_INSENSITIVE);
                            Matcher mCheck1 = pCheck1.matcher(line);
                            Pattern pCheck2 = Pattern.compile(versionFmbXml.toString() + "\\s*\\d*", Pattern.CASE_INSENSITIVE);
                            Matcher mCheck2 = pCheck2.matcher(line);
                            Pattern pCheck3 = Pattern.compile("[0-9][0-9][0-9][0-9][0-9]", Pattern.CASE_INSENSITIVE);
                            Matcher mCheck3 = pCheck3.matcher(line);
                           //The following code checks matches with patterns and content.
                            if (mCheck1.find())
                            { 
                                xmlCheck1 = lineCount;
                            }
                            xmlCheck1a = line.indexOf("'" + versionFmbXml.toString() + "'");
                            if (mCheck2.find())
                            {
                                xmlCheck2 = lineCount;
                            }
                            xmlCheck2a = line.indexOf(versionFmbXml.toString());
                        
                            xmlCheck3 = line.indexOf(logNumber);
                            
                            if (mCheck3.find())
                            {
                                xmlCheck3a = lineCount;
                            }
                            //Check for revision variable
                            if (xmlCheck1 > -1 & xmlCheck1a > - 1)
                            {
                                revVariableFound = true;
                            }
                            //Check for the revision comment
                            if (xmlCheck2 > -1 & xmlCheck2a > -1)
                            {
                                revCommentFound = true;
                            }
                            //Check for the log number
                            if (xmlCheck3 > -1 & xmlCheck3a > -1) 
                            {
                                logNumberFound = true;
                            }
                        }
                        //If the revision comment and revision variable bothreturn true, we have passed the revision test
                        if (revCommentFound == true & revVariableFound == true) 
                        {
                            revisionFound = true;
                        }
                        //If there is not a match 
                        if (revisionFound == false)
                        {                        
                            //ERROR: ‘Filename’: Version numbering incorrect. Expecting version number ‘Revision’ as comment and also as a revision variable
                            testPrepPassed = false;
                            errors = "ERROR: " + fileName.substring(0,(fileName.length() - 8)) + ".fmb" +   ": Version numbering incorrect. Expecting version number " + versionFmbXml.toString() + " as comment and also as a revision variable" + System.lineSeparator();
                            System.out.println("ERROR: " + fileName.substring(0,(fileName.length() - 8)) + ".fmb" +   ": Version numbering incorrect. Expecting version number " + versionFmbXml.toString() + " as comment and also as a revision variable");
                        }
                        //Check if the log number was found
                        if (logNumberFound == false)
                        {
                            //ERROR: ‘Filename’: Version numbering incorrect. Expecting version number ‘Revision’ as comment and also as a revision variable
                            testPrepPassed = false;
                            errors = errors + "ERROR: " + fileName.substring(0,(fileName.length() - 8)) + ".fmb" + ": Log number incorrect. Expecting log number " + logNumber + " as comment" + System.lineSeparator();
                            System.out.println("ERROR: " + fileName.substring(0,(fileName.length() - 8)) + ".fmb" + ": Log number incorrect. Expecting log number " + logNumber + " as comment");
                        }
                    }
                }
            }
            //It's possible that the FMB conversion failed, so need to allow for that.
            else if (fmbxmlFilesExist == false && fmbInc == true)
            {
                testPrepPassed = false;
                errors = errors + "ERROR: No FMX Files were converted, could not find any inside subfolder." + System.lineSeparator();
                System.out.println("ERROR: No FMX Files were converted, could not find any inside subfolder.");  
            }
            //Likewise for the Plds
            if (pldFilesExist == true)
            {
                System.out.println("Checking pll versions");
                //For every PLD
                for (int i = 0; i < pldFiles.length; i++)
                {
                    //Need to make sure that specific file exists.
                    if (pldFiles[i] != null)
                    {
                        //Declare a variable that will be used to store the version number (you'll see why in a moment).
                        BigDecimal versionPld = null;
                        //Go through all the pvcsObjects in order to align the correct version number
                        for (int j = 0; j < pvcsObjects.length; j++) 
                        {
                            //If we find a matching pld and pll
                            if ((pldFiles[i].getName().substring(0,pldFiles[i].getName().length() - 3) + "pll").equals(pvcsObjects[j])) 
                            {
                                versionPld = versionNumbers[j];
                            }
                        }
                        
                        String fileName = pldFiles[i].getName();
                        //The following lines of code will read the file for errors
                        BufferedReader bReader = new BufferedReader(new FileReader(pldFiles[i]));
                        int lineCount = 0;
                        String line;
                        boolean logNoFound = false;
                        boolean revisionFound = false;
                        boolean commentFound = false;
                        while ((line = bReader.readLine()) != null)
                        {
                            lineCount++;
                            //Check file for: “($revision.*$RDlog)” and also “(revision.*$revision)”
                            int logNoCheck = line.indexOf(logNumber);
                            int revisionCheck = line.indexOf(versionPld.toString());
                            int commentCheck = - 1;
                            Pattern revCheck = Pattern.compile("revision\\s*varchar2\\(\\d*\\)\\s*:=\\s*'", Pattern.CASE_INSENSITIVE);
                            Matcher revMatch = revCheck.matcher(line);
                            Pattern revisionPrescence = Pattern.compile("\\d\\.\\d*");
                            Matcher preMatch = revisionPrescence.matcher(line);
                            if (revMatch.find()) 
                            {
                                commentCheck = lineCount;
                            }

                            //Check if log number and revision are on the same line
                            if (logNoCheck > -1 & revisionCheck > -1)
                            {
                                commentFound = true;
                            }
                            //Check if the revision was included in the revision variable
                            if (revisionCheck > -1 & commentCheck > -1)
                            {
                                revisionFound = true;
                            }
                            //Check if the log number was on the same line as a version number
                            if (logNoCheck > -1 & preMatch.find()) 
                            {
                                logNoFound = true;
                            }
                        }
                        //If there is not a match
                        if (commentFound == false) 
                        {
                            testPrepPassed = false;
                            //Print Error
                            errors = errors + "ERROR: " + pldFiles[i].getName().substring(0,(pldFiles[i].getName().length() - 3)) + "pll" + " Comment incorrect. Expecting version number and RD log number on the same line." + System.lineSeparator();
                            System.out.println("ERROR: " + pldFiles[i].getName().substring(0,(pldFiles[i].getName().length() - 3)) + "pll" + " Comment incorrect. Expecting version number and RD log number on the same line.");                      
                        }
                        if (revisionFound == false)
                        {
                            testPrepPassed = false;
                            //ERROR: $Filename: Version numbering incorrect. Expecting ‘Revision’ followed by the version number
                            errors = errors + "ERROR: " + pldFiles[i].getName().substring(0,(pldFiles[i].getName().length() - 3)) + "pll" + " Version numbering incorrect. Expecting ‘Revision’ followed by the version number." + System.lineSeparator();
                            System.out.println("ERROR: " + pldFiles[i].getName().substring(0,(pldFiles[i].getName().length() - 3)) + "pll" + " Version numbering incorrect. Expecting ‘Revision’ followed by the version number.");
                        }
                        if (logNoFound == false) 
                        {
                            testPrepPassed = false;
                            //PrintError
                            errors = errors + "ERROR: " + pldFiles[i].getName().substring(0,(pldFiles[i].getName().length() - 3)) + "pll" + " Log number incorrect. Expecting log number "+ logNumber + "." + System.lineSeparator();
                            System.out.println("ERROR: " + pldFiles[i].getName().substring(0,(pldFiles[i].getName().length() - 3)) + "pll" + " Log number incorrect. Expecting log number "+ logNumber);  
                        }
                    }
                }
            }
            //It's possible that the PLL conversion failed, so need to allow for that.
            else if (pldFilesExist == false && pllInc == true)
            {
                testPrepPassed = false;
                errors = errors + "ERROR: No PLD Files were converted, could not find any inside subfolder.\n";
                System.out.println("ERROR: No PLD Files were converted, could not find any inside subfolder.");  
            }
        }
        //The following code deals with all other objects. We will need to list the entire directory contents
        File[] dir_contents = directory.listFiles();
        //List an array of files based on Response Desk data
        files = new File[pvcsObjects.length];
        //Test to see if Database Objects are included
        boolean databaseObjectsIncluded = false;
        
        for (int i = 0; i < dir_contents.length; i++)
        {
            for (int j = 0; j < pvcsObjects.length; j++)
            {
                //Need to filter file types, not all of these should be going through this method
                if (!("sql").equals (pvcsObjects[j].substring(pvcsObjects[j].length() - 3)))
                {
                    if (!("fmb").equals (pvcsObjects[j].substring(pvcsObjects[j].length() - 3)))
                    {
                        if (!("xml").equals (pvcsObjects[j].substring(pvcsObjects[j].length() - 3)))
                        {
                            if (!("pll").equals (pvcsObjects[j].substring(pvcsObjects[j].length() - 3)))
                            { 
                                if (!("zip").equals (pvcsObjects[j].substring(pvcsObjects[j].length() - 3)))
                                {
                                    if (!("ear").equals (pvcsObjects[j].substring(pvcsObjects[j].length() - 3)))
                                    { 
                                        //If there's a match between Response Desk and Directory
                                        if ((pvcsObjects[j].equals (dir_contents[i].getName())))
                                        {
                                            //We can safely assign the two vin line with each other.
                                            files[j] = dir_contents[i];
                                            databaseObjectsIncluded = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        //For every other type of file (except scripts)
        if (databaseObjectsIncluded == true)
        {
            for (int i = 0; i < pvcsObjects.length; i++)
            {
                //For SPL files we need to make sure the matching PLD is also in the directory.
                boolean pldFound = false;
                if (("spl").equals (pvcsObjects[i].substring(pvcsObjects[i].length() - 3)))
                {
                    for (int j = 0; j < dir_contents.length; j++)
                    {
                        if ((dir_contents[j].getName()).equals(pvcsObjects[i].substring(0, pvcsObjects[i].length() - 3) + "pld")) 
                        {
                            //Matching PLD has been found
                            pldFound = true;
                        }
                    
                    }
                    //If the PLD hasn't been found
                    if (pldFound == false) 
                    {
                        //Report error for this filename
                        String fileName = files[i].getName();
                        testPrepPassed = false;
                        errors = errors + "ERROR: Matching PLD for " + fileName + " not found in file folder." + System.lineSeparator();
                        System.out.println("ERROR: Matching PLD for " + fileName + " not found in file folder.");
                    }
                }
            }
            //Now the the initialisation and filtering is over we can check for versions
            System.out.println("Checking database object versions");
            for (int i = 0; i < files.length; i++)
            {
                if (files[i] != null)
                {
                    //Initialise a variable to hold version numbers so that the object can be aligned correctly with Response Desk
                    BigDecimal versionDat = null;
                    for (int j = 0; j < pvcsObjects.length; j++) 
                    {
                        if ((files[i].getName()).equals(pvcsObjects[j])) 
                        {
                            versionDat = versionNumbers[j];
                        }

                    }
                    String fileName = files[i].getName();
                    //The following lines of code will read the file for errors
                    BufferedReader bReader = new BufferedReader(new FileReader(files[i]));
                    int lineCount = 0;
                    String line;
                    boolean lineFound = false;
                    boolean versionFound = false;
                    boolean logNoFound = false;
                    //Check file for following expression: #($revision.*$RDlog)”
                    while ((line = bReader.readLine()) != null)
                    {
                        //Initialise while loop only variables
                        lineCount++;
                        int posFound = line.indexOf(versionDat.toString());
                        int posFound2 = line.indexOf(logNumber);
                        Pattern logCheck = Pattern.compile("\\d\\d\\d\\d\\d");
                        Matcher logMatch = logCheck.matcher(line);
                        Pattern revisionPrescence = Pattern.compile("\\d\\.\\d*");
                        Matcher preMatch = revisionPrescence.matcher(line);
                        //If there is a match
                        if (posFound > -1)
                        {
                            if (posFound2 > -1)
                            {
                                lineFound = true;
                            }
                        }
                        //Check for version number
                        if (posFound > -1 & logMatch.find()) 
                        {
                            versionFound = true;
                        }
                        //Check for log number
                        if (posFound2 > -1 & preMatch.find()) 
                        {
                            logNoFound = true;
                        }
                    }
                    //If there is not a match
                    if (lineFound == false)
                    {
                        testPrepPassed = false;
                        //ERROR: $Filename: Version numbering incorrect. Expecting version number
                        //and RD log number on the same line
                        errors = errors + "ERROR: " + fileName + " Comment incorrect. Expecting version number and RD log number on the same line" + System.lineSeparator();
                        System.out.println("ERROR: " + fileName + " Comment incorrect. Expecting version number and RD log number on the same line");     
                    }
                    if (versionFound == false)
                    {
                        testPrepPassed = false;
                        //ERROR: $Filename: Version numbering incorrect. Expecting version number
                        //and RD log number on the same line
                        errors = errors + "ERROR: " + fileName + " Version numbering incorrect. Expecting version number " + versionDat.toString() + System.lineSeparator();
                        System.out.println("ERROR: " + fileName + " Version numbering incorrect. Expecting version number " + versionDat.toString());
                    }
                    if (logNoFound == false)
                    {
                        testPrepPassed = false;
                        //ERROR: $Filename: Version numbering incorrect. Expecting version number
                        //and RD log number on the same line
                        errors = errors + "ERROR: " + fileName + " log numbering incorrect. Expecting log number " + logNumber + System.lineSeparator();
                        System.out.println("ERROR: " + fileName + " log numbering incorrect. Expecting log number " + logNumber);
                    }
                }
            }
        }
        //Write errors out to a version errors text file
        try 
        {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/" + "versionerrors.txt"), "utf-8"));
            writer.write(errors);
        }
        catch (IOException ex) 
        {
            // Report
            System.out.println("Could not write to versionerrors file.");
        } 
        finally 
        {
           try {writer.close();} catch (Exception ex) {/*ignore*/}
        }
        //The overall result should now be returned
        return testPrepPassed;
    }
}