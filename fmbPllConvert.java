package classes;

import java.io.IOException;
import java.io.File;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class fmbPllConvert
{
    //Class variables
    String[] objects;
    List<String> fmbxmlArray;
    List<String> pldArray;
    int[] noOfFMBs;
    int[] noOfPLLs;
    File directory;
    File form2xml;
    //Class Constructor
    public fmbPllConvert(String[] obj, File dir, Config frm2)
    {
        objects = obj;
        directory = dir;
        form2xml = new File(frm2.forms2xml);
    }
  
    //Main Method
    public fmbxmlPld conversion() throws IOException, InterruptedException
    {
        fmbxmlArray = new ArrayList<String>();
        pldArray = new ArrayList<String>();
        //For All Objects in PVCS Array
        for (int i=0; i < objects.length; i++)
        {
            //If File type is an FMB
            if(("fmb").equals(objects[i].substring((objects[i].length() - 3))))
            {
                System.out.println("Checking forms and converting them into xml files");
                File currentFile = new File(directory + "/" + objects[i]);
                String currentFileString = (directory + "\\" + objects[i]);
                //Run Forms2XML for that particular object
                Runtime fmx = Runtime.getRuntime();
                try
                {
                    System.out.println("Converting " + objects[i] + " into an xml file");
                    //Process cd = fmx.exec("cd " + fmxldir);
                    String formdirString = form2xml.getParent();
                    File formdir = new File(formdirString);
                    Process fx = fmx.exec(new String [] {"cmd", "/c", form2xml.getName() + " OVERWRITE=YES " + currentFileString}, null, formdir);
                    //If form can't process on time, there is clearly a problem here. Later code will resolve this.
                    fx.waitFor();
                    TimeUnit.SECONDS.sleep(10);
                    //Need to look in the directory for the file
                    File[] dir_contents = directory.listFiles();
                    File xmlCheck = new File(directory + "/" + objects[i].substring(0,(objects[i].length() - 4)) + "_fmb.xml");
                    boolean fileFound = false;
                    //Check to see if the xml has been created yet.
                    for (int k = 0; k < dir_contents.length; k++)
                    {
                        //If the XML file has been created yet.
                        if ((xmlCheck).equals(dir_contents[k]))
                        {
                            fileFound = true;
                            fmbxmlArray.add(objects[i].substring(0,(objects[i].length() - 4)) + "_fmb.xml");
                            break;
                        }
                    }
                    //If forms2xml fails to work we need a way to check if the file has not been found.
                    if (fileFound == false) 
                    {
                        System.out.println("ERROR: Program could not convert FMB into XML. Please run " + form2xml.getName() + " manually for " + currentFile.getName() + " and then run the program again to analyse version number.");
                    }
                }
                //Running a command prompt always leaves open the possibility of failure, so a catch is needed.
                catch(IOException e)
                {
                    System.out.println("ERROR: Program could not process FMB.");
                    e.printStackTrace();
                }
                //Need to allow for the timer being interrupted
                catch(InterruptedException e) 
                {
                    e.printStackTrace();
                }
            }
            //Else IF File type is PLL
            else if (("pll").equals(objects[i].substring((objects[i].length() - 3))))
            {
                System.out.println("Checking pll files and converting them into pld files");
                Runtime plx = Runtime.getRuntime();
                try
                {
                    //Run frmcmp to convert the file into a pld
                    File currentFile = new File(directory + "/" + objects[i].substring(0,(objects[i].length() - 3)) + "pld");
                    System.out.println("Converting " + objects[i] + " into a pld file");
                    Process plxpr = plx.exec("frmcmp userid=drivedev/drivedev@drg1dev module=" + objects[i] + " module_type=LIBRARY window_state=minimize script=YES", null, directory);
                    //frmcmp runs independently to the runtime of testPrep, so we need to force the program to wait until the pld has been created, otherwise the array will store null.
                    plxpr.waitFor();
                    //Need to look in the directory for the file
                    File[] dir_contents = directory.listFiles();
                    boolean fileFound = false;
                    //Check to see if the pld has been created yet.
                    for (int k = 0; k < dir_contents.length; k++)
                    {
                        //If the PLD file has been created yet.
                        if ((currentFile).equals(dir_contents[k]))
                        {
                            //We don't need this for loop anymore for that particular object, so add it to the pllTextArray and let the program know it's been found.
                            fileFound = true;
                            pldArray.add(objects[i].substring(0,(objects[i].length() - 3)) + "pld");
                            //Exit out of this for loop due to finding the file
                            break;
                        }
                    }
                    //Need to allow for the possibility that this program won't run correctly.
                    if (fileFound == false) 
                    {
                        System.out.println("ERROR: Program could not convert PLL.");
                    }
                }
                //Running a command prompt always leaves open the possibility of failure, so a catch is needed.
                catch(IOException e)
                {
                    System.out.println("ERROR: Program could not convert PLL.");
                }
            }
        }
        //return
        fmbxmlPld fmbxmlpld = new fmbxmlPld(fmbxmlArray,pldArray);
        return fmbxmlpld;
    }
}
