package classes;

//import packages
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import oracle.jdbc.*;
import sun.rmi.runtime.Log;

//BEGIN
public class flsImCheck 
{
    //Log number
    String logNumber;
    //Log type
    String logType;
    Statement statement;
    File directory;
    String errors = "";
    Writer writer = null;
    //Class Constructor
    public flsImCheck(String logNo, String logT, Statement state, File dire)
    {
        //Log number
        logNumber = logNo;
        //Log type
        logType = logT;
        //Connection
        statement = state;
        //Directory (for file writing purposes)
        directory = dire;
    }
    //Call Response Desk data class
    //Main method
    public boolean releaseChecker() throws SQLException, IOException
    {
        //String variable ReqDoc
        String reqDoc;
        //Boolean variable Included
        boolean included;
        //Need to be able to print errors

        //IF Log type equals Fault
        if (logType == "Fault")
        {
            System.out.println("Checking for FLS Document");
            //ReqDoc equals “FLS”
            reqDoc = "FLS";
            //Check log in Response Desk to see if an FLS Document exists
            ResultSet document = statement.executeQuery("select attachment_name, attachment_type from ATTACHEDFILES where LOG_ID = " + logNumber + " and attachment_name like '%FLS%' and attachment_type ='D'");
            //If Document exists {Included = true} else {Included = false}
            if (!document.next())
            {
                included = false;
                errors = errors + "ERROR : FLS is either missing, incorrectly named or in the wrong location in ResponseDesk. FLS expected for a Fault." + System.lineSeparator();
                System.out.println("ERROR : FLS is either missing, incorrectly named or in the wrong location in ResponseDesk. FLS expected for a Fault.");
            }
            else
            {
                included = true;
            }
        }
        //ELSE IF Log type equals Change Request
        else if (logType == "Change Request")
        {
            System.out.println("Checking for IM Document");
            //ReqDoc equals “IM”
            reqDoc = "IM";
            //Check log in Response Desk to see if an IM Document exists
            //Check to see if it is listed in the Documents section
            ResultSet document = statement.executeQuery("select attachment_name from ATTACHEDFILES where LOG_ID = " + logNumber + " and (attachment_name like 'IM%' or attachment_name like 'Implementation Manual%') and attachment_type ='D'");
            //If Document exists {Included = true} else {Included = false]
            if (!document.next())
            {
                included = false;
                errors = errors + "ERROR : IM is either missing, incorrectly named or in the wrong location in ResponseDesk.  IM expected for a Change Request." + System.lineSeparator();
                System.out.println("ERROR : IM is either missing, incorrectly named or in the wrong location in ResponseDesk.  IM expected for a Change Request.");
            }
            else
            {
                included = true;
            }
        }
        //ELSE
        else
        {
            included = false;
            //Print custom error message:
            errors = errors + "ERROR : Log not recognised as either a Fault or a Change Request. Please check log type in Response Desk and make sure that it is either listed as a Fault or a Change Request, as required." + System.lineSeparator();
            System.out.println ("ERROR : Log not recognised as either a Fault or a Change Request. Please check log type in Response Desk and make sure that it is either listed as a Fault or a Change Request, as required.");
        }
        
        //Write errors out to text files
        try 
        {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/" + "flsimerrors.txt"), "utf-8"));
            writer.write(errors);
        } 
        catch (IOException ex) 
        {
            // Report
            System.out.println("Could not write to flsimerrors file.");
        } 
        finally 
        {
           try {writer.close();} catch (Exception ex) {/*ignore*/}
        }
        return included;
    }
}
//END