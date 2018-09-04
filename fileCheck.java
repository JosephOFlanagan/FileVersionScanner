package classes;
//In order for this class to work properly the File class needs to be imported.
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

//This class checks whether the files that PVCS thinks are in the log exist in the directory. It also adds the files to their own arrays for future use.
public class fileCheck 
{
    //declare variables
    String[] objectNames;
    String[] attachmentNames;
    String[] attachmentTypes;
    File[] attachmentFiles;
    File directory;
    int attachmentFileNumber = 0;
    boolean clientSide;
    Writer writer;
    //Class Constructor
    public fileCheck(responseDeskData obj, File dir, boolean cs)
    {
        //We are only interested in the names of the files, but still need to call the whole class to keep things consistent
        objectNames = obj.pvcsObjects;
        attachmentNames = obj.attachments;
        attachmentFiles = obj.attachmentFiles;
        attachmentTypes = obj.attachmentTypes;
        directory = dir;
        clientSide = cs;
    }  
    //This class
    boolean fileExists = false;
    boolean directoryExists = true;
    boolean form2xml = false;
    //Declare variables to check the directory
    File[] dir_contents;
  
    //This is the main method of the class.
    public String fileChecker()
    {
        //We need to log any missing files
        String missingFiles = "";
        dir_contents = directory.listFiles();
        
        if (dir_contents == null)
        {
            directoryExists = false;
            missingFiles = missingFiles + directory + System.lineSeparator();
            System.out.println("No folder named " + directory.getName() + " exists. Please ensure that the contents of the log that you wish to test are all within the directory " + directory + " before running Test Prep again.");
        }
        if (directoryExists == true)
        {
            //For all files in responseDeskData.pvcsobjects
            for (int i = 0; i < objectNames.length; i++)
            {
                fileExists = false;
                //File class is called with the value of the array
                File file = new File(objectNames[i]);
                //Declare variables to check the directory
      
                //Check against Directory to see if it exists      
                for(int j = 0; j < dir_contents.length;j++)
                {
                    if ((file.getName()).equals(dir_contents[j].getName()))
                    {
                        //File is confirmed to exist
                        fileExists = true;
                    }
                }
                if (fileExists == false)
                {
                    //There is clearly a missing file here so set the missingFiles variable to false and add it to the list of missing files
                    missingFiles = missingFiles + objectNames[i] + System.lineSeparator();
                }
            }
            //check all attachments in Response Desk are attached to this log.
            for (int i = 0; i < attachmentNames.length; i++)
            {
                boolean attached = false;
                for(int j = 0; j < dir_contents.length;j++)
                {
                    if ((attachmentNames[i]).equals(dir_contents[j].getName()))
                    {
                        //Object is attached.
                        attachmentFiles[i] = dir_contents[j];
                        attached = true;
                    }
                }
                if (attached == false && ("script").equals(attachmentTypes[i]))
                {
                    //If there are supposed to be scripts as part of this log, report so.
                    missingFiles = missingFiles + attachmentNames[i] + System.lineSeparator();
                    System.out.println(attachmentNames[i] + " is not attached to this log.");
                }
            }
        }
        //Write errors out to a missing files text file
        try 
        {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/" + "missingfiles.txt"), "utf-8"));
            writer.write(missingFiles);
        } 
        catch (IOException ex) 
        {
            // Report
            System.out.println("Could not write to missingfiles");
        } 
        finally 
        {
           try {writer.close();} catch (Exception ex) {/*ignore*/ ;}
        }
        //return result
        return missingFiles;
    }
}