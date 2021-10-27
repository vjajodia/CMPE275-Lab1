package data;

import crimewatch.obs.madis.MesonetProcessor;

import java.io.File;

public class ReadData
{
    MesonetProcessor mesonetProcessor = new MesonetProcessor();

    public void getFile()
    {
        File folder = new File("/Users/asonvane/Documents/data/2013/01-01/");
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
//            System.out.println(listOfFiles[i].getName());

            if (listOfFiles[i].getName().lastIndexOf(".") != -1) {
                String name = listOfFiles[i].getName().substring(0, listOfFiles[i].getName().lastIndexOf("."));
                System.out.println(name);
                mesonetProcessor.main(new String[]{listOfFiles[i].toString(), "./catalog.csv", "./output"});
            }
        }
    }

    public static void main(String[] args)
    {
        ReadData readData = new ReadData();
        readData.getFile();
    }
}
