package classes;

//import packages
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class zipEarFiles 
{
    //Objects
    String[] objects;
    //Directory
    File directory;
    Writer writer;
    String errors = "";
    BigDecimal[] versionNumbers;
    private static final int BUFFER_SIZE = 4096;

    public zipEarFiles(fileData obj, File dir, BigDecimal[] vN)
    {
        //Objects
        objects = obj.objects;
	//Directory
        directory = dir;
        versionNumbers = vN;
    }
    boolean extracted = false;
    boolean testScanPassed = true;
    //CurrentObject variable
    String currentObject;
    //filePath variable
    File filePath;
    File[] file;
    ZipFile currentZipFile;
    File systemVersion = null;
    String systemFile = null;
    boolean versionFileFound = false;
    InputStream systemFileStream = null;
    OutputStream newFileLocation = null;
    
    public boolean zipCheck() throws IOException //Main method
    {
        //For all Objects in Array
        for (int i = 0; i < objects.length; i++)
        {
            currentObject = objects[i];
            filePath = directory;
            //If Object is .ZIP type
            if (("zip").equals(currentObject.substring((currentObject.length() - 3))))
            {
                //If Object contains **text
                if (currentObject.indexOf("company name") > - 1)
                {
                    //For all Objects in Array
                    File [] contents = filePath.listFiles();
                    for (int j = 0; j < contents.length; j++)
                    {
                        //If Object is not .Zip type
                        if (("zip").equals(contents[j].getName().substring(contents[j].getName().length() - 3)))
                        {
                            currentZipFile =  new ZipFile(contents[j]);
                            String searchFileName = currentObject.substring(0,pvcsObjects[i].length() - 4);
                            //If Object equals currentObject
                            Enumeration e = currentZipFile.entries();
                            while (e.hasMoreElements())
                            {
                                ZipEntry entry = (ZipEntry)e.nextElement();
                                if (entry.getName().indexOf(searchFileName) != -1) 
                                {
                                    //Print Error message
                                    errors = errors + "ERROR: " + currentObject + " Folder compressed at the wrong level. Sub-folder exists within the zip file." + System.lineSeparator();
                                    System.out.println("ERROR: " + currentObject + " Folder compressed at the wrong level. Sub-folder exists within the zip file.");
                                    //Break Method
                                    testPrepPassed = false;
                                    break;
                                }
                            }
                            currentZipFile.close();
                        }
                    }
                }
                //If Object equals adrive_cre
                if ((currentObject.substring(0,pvcsObjects[i].length() - 4)).equals ("**java program name"))
                {
                    File[] dir_contents = filePath.listFiles();
                    for (int j = 0; j < dir_contents.length; j++)
                    {
                        //What happens here depends on whether the jar has already been extracted or not
                        if (("zip").equals(dir_contents[j].getName().substring(dir_contents[j].getName().length() - 3)))
                        {
                            currentZipFile = new ZipFile(dir_contents[j]);
                            boolean found = searchZipPath(currentZipFile, "SystemVersion.class");
                            if (found == true)
                            {
                                //The jar file has been extracted, so we know that SystemVersion exists
                                System.out.println("Version File found Inside Zip File: " + currentZipFile.getName());
                                extracted = true;
                            }
                            else 
                            {
                                //Set extracted to equal false so that the jar file can get extracted
                                extracted = false;
                            }
                        }
                        else if (("jar").equals(dir_contents[j].getName().substring(dir_contents[j].getName().length() - 3))) 
                        {
                            if (extracted == false) 
                            {
                                //We need to extract the jar file, so this needs to be set up
                                InputStream adrive = new FileInputStream (directory + "/**java program name.jar");
                                OutputStream adrive_creExtract = new FileOutputStream(directory + "/**java program name.zip");
                                //Call extractFile to extract the jar
                                extractFile(adrive, adrive_creExtract, directory);
                                systemVersion = dir_contents[j];
                            }
                        }
                    }
                    //With the file extracted, it should now appear in the primary directory, we can now check the version number, but just in case the check didn't work the first time we need to do it again
                    if (systemFileStream != null || systemVersion.getName() == "**java program name.jar")
                    {
                        File extractedFile = new File(directory + "/SystemVersion.class");
                        newFileLocation = new FileOutputStream(extractedFile);
                        extractFile(systemFileStream, newFileLocation, directory);
                        //Search for the version number
                        boolean versionFound = versionSearch(extractedFile, versionNumbers[i]);
                        if (versionFound == false) 
                        {
                            //ERROR: $Filename: Version numbering incorrect. Expecting ‘mSystemVersion’ followed by the version number.
                            errors = errors + "ERROR: " + currentObject + " Version numbering incorrect. Expecting ‘mSystemVersion’ followed by the version number." + System.lineSeparator();
                            System.out.println("ERROR: " + currentObject + " Version numbering incorrect. Expecting ‘mSystemVersion’ followed by the version number.");
                            testPrepPassed = false;
                        }
                    }
                }
                //resacar is another special case as we need to check that this hasn't been compressed at the wrong level
                else if (currentObject.indexOf("***text") > - 1) 
                {
                    //Search for a jar file of the same name
                    if (("jar").equals(currentObject.substring((currentObject.length() - 3))))
                    {
                        File jarFile = new File(filePath + "/" + currentObject);
                        //search for the version number
                        boolean versionFound = versionSearch(jarFile, versionNumbers[i]);
                        if (versionFound == false)
                        {
                            //If the version number doesn’t match
                    
                            //ERROR: $Filename: Version numbering incorrect. Expecting ‘mSystemVersion’ followed by the version number.
                            errors = errors + "ERROR: " + currentObject + "Version numbering incorrect. Expecting "+ versionNumbers[i] + "followed by the version number." + System.lineSeparator();
                            System.out.println("ERROR: " + currentObject + "Version numbering incorrect. Expecting "+ versionNumbers[i] + "followed by the version number.");
                            testPrepPassed = false;
                        }           
                    }
                }
                //Else
                else
                {
                    if (versionFileFound == false)
                    {
                        //Object = currentObject
                        pvcsObjects[i] = currentObject;
                        //refine file path in case of mulitple ear files
                        file = directory.listFiles();
                        for (int j = 0; j < file.length; j++)
                        {
                            //Align files
                            if ((currentObject).equals (file[j].getName()))
                            {
                                currentZipFile = new ZipFile(file[j]);
                                //Set Value to filePath
                                filePath = file[j];
                                versionFileFound = true;
                                break;
                            }
                        }
                        //Retrieve ClsSystemVersion.java
                        boolean found = searchZipPath(currentZipFile, "ClsSystemVersion.java");
                        if (found == true)
                        {
                            System.out.println("Version File found Inside Zip File: " + currentZipFile.getName());
                        }
                        else 
                        {
                            System.out.println("File : ClsSystemVersion.java Not Found Inside Zip File: " + currentZipFile.getName());
                            testPrepPassed = false;
                        }
                        //If the file was found successfully, extract and check the new file
                        if (systemFileStream != null)
                        {
                            File extractedFile = new File(directory + "/ClsSystemVersion.java");
                            newFileLocation = new FileOutputStream(extractedFile);
                            //extract file to parent directory
                            extractFile(systemFileStream, newFileLocation, directory);
                            //Search for version number
                            boolean versionFound = versionSearch(extractedFile, versionNumbers[i]);
                            if (versionFound == false) 
                            {
                                //If the version number doesn’t match
                            
                                //ERROR: $Filename: Version numbering incorrect. Expecting ‘mSystemVersion’ followed by the version number.
                                errors = errors + "ERROR: " + currentObject + " Version numbering incorrect. Expecting mSystemVersion followed by the version number " + versionNumbers[i] + System.lineSeparator();
                                System.out.println("ERROR: " + currentObject + " Version numbering incorrect. Expecting mSystemVersion followed by the version number " + versionNumbers[i] + ".");
                                testPrepPassed = false;
                            }
                        }
                        else 
                        {
                            //Some eaer files don't have version numbers, error if this is the case
                            errors = errors + "Version number file not found. Either the version number is not stored in this file of the program could not find the version number file" + System.lineSeparator();
                            System.out.println("Version number file not found. Either the version number is not stored in this file of the program could not find the version number file");
                        }
                        currentZipFile.close();
                    }
                }
            }
        }
        //Write errors out to a zip ear error text file
        try 
        {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(directory + "/" + "zipearfileserrors.txt"), "utf-8"));
            writer.write(errors);
        } 
        catch (IOException ex) 
        {
            // Report
            System.out.println("Could not write to zipearfileserrors file.");
        } 
        finally 
        {
           try {writer.close();} catch (Exception ex) {/*ignore*/}
        }
        return testPrepPassed;
    }
    
    //The following method searches Zip files for particular files
    private boolean searchZipPath(ZipFile zipFile, String search) 
    {
        String searchFileName = search;
        //We want to look through the code to find the right file path
        Enumeration e = zipFile.entries();
        boolean found = false;
        System.out.println("Trying to search " + searchFileName + " in " + zipFile.getName());
        while (e.hasMoreElements()) 
        {
            //search through the entire zip file to find the right directory
            ZipEntry entry = (ZipEntry)e.nextElement();
            try
            {
                if (entry.getName().indexOf(searchFileName) != -1) 
                {
                    found = true;
                    versionFileFound = true;
                    //The following string will be used to allow this file to be searchable
                    systemFileStream = zipFile.getInputStream(entry);
                    break;
                }
            }
            catch (IOException i) 
            {
                i.printStackTrace();
            }
        }
        //return result
        return found;
    }
    //The following method extracts Zip files and places the required file in a new directory.
    private void extractFile(InputStream zipIn, OutputStream zipOut, File filePath) throws IOException 
    {
        //This method extracts the correct file from a Zip File
        BufferedOutputStream bos = null;
        try
        {
            //A new file will be created in the directory to hold the zip file's data, which can then subsequently be read
            bos = new BufferedOutputStream(zipOut);
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1)
            {
                bos.write(bytesIn, 0, read);
            }
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        finally 
        {
            //Close input streams to prevent data leaking
            if (zipIn != null) 
            {
                try 
                {
                    bos.close();
                }
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
            if (zipOut != null)
            {
                try 
                {
                    bos.close();
                }
                catch (IOException e) 
                {
                    e.printStackTrace();
                }
            }
        }
    }
    //The following method searches versions for the correct number
    private boolean versionSearch (File systemVersion, BigDecimal versionNumber) 
    {
        //The following code now checks for version number
        boolean versionFound = false;
        try
        {
            BufferedReader bReader = new BufferedReader(new FileReader(systemVersion));
            int lineCount = 0;
            String line = "";
            while ((line = bReader.readLine()) !=null)
            {
                //with the file having already been found by earlier code, we can check for the version number.
                lineCount++;
                int posFound = line.indexOf(versionNumber.toString());
                if (posFound > - 1)
                {
                    //Generally Zip Files require mSystemVersion to be written in front of them, so this checks that
                    Pattern mSystemVersion = Pattern.compile("mSystemVersion\\s*=\\s*" + versionNumber);
                    Matcher matchSystem = mSystemVersion.matcher(line);
                    if ((systemVersion.getName()).equals("ClsSystemVersion.java"))
                    {
                        if (matchSystem.find())
                        {
                            versionFound = true;
                        }
                    }
                    //adrive_cre is the one exception to the mSystemVersion rule so need to allow for that.
                    else
                    {
                        versionFound = true;
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //return result
        return versionFound;
    }
}
